package me.kuwg.re.ast.nodes.function.call;

import me.kuwg.re.ast.nodes.cast.CastNode;
import me.kuwg.re.ast.nodes.function.declaration.FunctionDeclarationNode;
import me.kuwg.re.ast.nodes.function.declaration.FunctionParameter;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.function.RDefFunction;
import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.compiler.function.RGenFunction;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.error.errors.function.RFunctionGenericsError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.NoneBuiltinType;
import me.kuwg.re.type.generic.GenericType;
import me.kuwg.re.type.iterable.arr.ArrayType;
import me.kuwg.re.type.lambda.LambdaType;
import me.kuwg.re.type.ptr.PointerType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class FunctionCallNode extends FunCall {
    public FunctionCallNode(final String fileName, final int line, final String name, final List<ValueNode> parameters) {
        super(fileName, line, name, parameters);
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        parameters.forEach(p -> p.replaceGenerics(generics, cctx));
    }

    @Override
    public void compile(final CompilationContext cctx) {
        compile0(cctx, false);
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        return compile0(cctx, true);
    }

    private String compile0(final CompilationContext cctx, boolean getting) {
        List<String> argRegs = new ArrayList<>(parameters.size());
        List<TypeRef> callTypes = new ArrayList<>(parameters.size());

        for (ValueNode param : parameters) {
            argRegs.add(param.compileAndGet(cctx));
            callTypes.add(param.getType());
        }

        String qualifiedName = cctx.qualify(name);

        RFunction fn  = cctx.getExact(qualifiedName, callTypes);

        if (fn == null) {
            fn = cctx.getFunction(qualifiedName, callTypes);
        }

        if (fn instanceof RGenFunction genFn) {
            cctx.emit("; Generic function call");
            return compileGeneric(cctx, genFn, callTypes, getting);
        }

        if (fn == null) {
            return tryLambda(cctx, callTypes, true);
        }

        cctx.emit("; Function call");
        return emitCall(cctx, fn, argRegs, callTypes, getting);
    }

    private String compileGeneric(CompilationContext cctx, RGenFunction genFn, List<TypeRef> callTypes, boolean getting) {
        validateGenericUsage(genFn);

        Map<String, TypeRef> bindings = inferGenericTypes(genFn, callTypes);
        replaceGenerics(bindings, cctx);

        for (var tp : genFn.typeParameters()) {
            if (!bindings.containsKey(tp.name())) {
                new RFunctionGenericsError("Could not infer type parameter " + tp + ". Please declare type parameters using fn<[T]...>([args]...)", fileName, line).raise();
            }
        }

        validateGenericConstraints(cctx, genFn, bindings);

        List<FunctionParameter> concreteParams = new ArrayList<>();
        for (var p : genFn.parameters()) {
            concreteParams.add(new FunctionParameter(p.name(), p.mutable(), substituteConcrete(p.type(), bindings)));
        }
        TypeRef concreteReturnType = substituteConcrete(genFn.returnType(), bindings);

        RFunction existing = genFn.getInstantiation(bindings);
        if (existing == null) {
            String mangledName = genFn.name() + "__" + bindings.values().stream().map(TypeRef::getName).reduce((a, b) -> a + "_" + b).orElse("");
            FunctionDeclarationNode concreteFnNode = new FunctionDeclarationNode(fileName, line, true, mangledName, concreteParams, concreteReturnType, genFn.block().clone());
            concreteFnNode.replaceGenerics(bindings, cctx);

            concreteFnNode.compile(cctx);
            RFunction concreteFn = cctx.getFunction(concreteFnNode.getQualifiedName(cctx), concreteParams.stream().map(FunctionParameter::type).toList());

            genFn.addInstantiation(bindings, concreteFn);
            existing = concreteFn;
        }

        List<String> argRegs = new ArrayList<>();
        for (ValueNode param : parameters) argRegs.add(param.compileAndGet(cctx));

        return emitCall(cctx, existing, argRegs, concreteParams.stream().map(FunctionParameter::type).toList(), getting);
    }

    private Map<String, TypeRef> inferGenericTypes(RGenFunction fn, List<TypeRef> callTypes) {
        Map<String, TypeRef> map = new HashMap<>();
        for (int i = 0; i < fn.parameters().size(); i++) {
            inferTypeBindings(fn.parameters().get(i).type(), callTypes.get(i), map);
        }
        return map;
    }

    private void inferTypeBindings(TypeRef paramType, TypeRef callType, Map<String, TypeRef> map) {
        if (paramType instanceof GenericType gen) {
            TypeRef existing = map.get(gen.name());
            if (existing == null) {
                map.put(gen.name(), callType);
            } else if (!existing.equals(callType)) {
                new RFunctionGenericsError("Conflicting types for " + gen.name() + ": " + existing.getName() + " vs " + callType.getName(), fileName, line).raise();
            }
        } else if (paramType instanceof ArrayType arr && callType instanceof ArrayType callArr) {
            inferTypeBindings(arr.getInner(), callArr.getInner(), map);
        } else if (paramType instanceof PointerType ptr && callType instanceof PointerType callPtr) {
            inferTypeBindings(ptr.getInner(), callPtr.getInner(), map);
        }
    }

    private String tryLambda(CompilationContext cctx, final List<TypeRef> callTypes, boolean getting) {
        RVariable v = cctx.getVariable(name);

        if (v == null || !(v.type() instanceof final LambdaType lambda)) {
            return throwNotFound(callTypes);
        }

        if (lambda.returnType() instanceof NoneBuiltinType && getting) {
            throwVoid(new RDefFunction(lambda.getLLVMName(), name, lambda.returnType(), List.of()), callTypes);
        }

        List<String> argRegs = new ArrayList<>(parameters.size());

        for (ValueNode param : parameters) {
            argRegs.add(param.compileAndGet(cctx));
        }

        for (int i = 0; i < parameters.size(); i++) {
            TypeRef expected = lambda.parameters().get(i);
            TypeRef actual = callTypes.get(i);

            if (!actual.equals(expected)) {
                CastNode cast = new CastNode(fileName, line, expected, parameters.get(i));
                argRegs.set(i, cast.compileAndGet(cctx));
                callTypes.set(i, expected);
            }
        }

        StringBuilder sb = new StringBuilder();
        String result = null;

        if (!(lambda.returnType() instanceof NoneBuiltinType)) {
            result = cctx.nextRegister();
            sb.append(result).append(" = ");
        }
        String fnPtr = v.valueReg();

        sb.append("call ").append(lambda.returnType().getLLVMName()).append(" ").append(fnPtr).append("(");

        for (int i = 0; i < argRegs.size(); i++) {
            sb.append(callTypes.get(i).getLLVMName()).append(" ").append(argRegs.get(i));

            if (i < argRegs.size() - 1) {
                sb.append(", ");
            }
        }

        sb.append(")");

        cctx.emit(sb.toString());

        setType(lambda.returnType());

        return lambda.returnType() instanceof NoneBuiltinType ? "%void_" + cctx.nextRegister() : result;
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Function Call: ").append(name).append("\n").append(indent).append("\t").append("Parameters:").append("\n");

        parameters.forEach(p -> p.write(sb, indent + "\t\t"));
    }

    @Override
    public FunctionCallNode clone() {
        List<ValueNode> paramsCloned = new ArrayList<>();
        IntStream.range(0, parameters.size()).forEach(i -> paramsCloned.add(i, parameters.get(i).clone()));
        return new FunctionCallNode(fileName, line, name, paramsCloned);
    }
}
