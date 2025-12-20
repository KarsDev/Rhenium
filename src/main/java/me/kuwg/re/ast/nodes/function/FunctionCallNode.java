package me.kuwg.re.ast.nodes.function;

import me.kuwg.re.ast.nodes.cast.CastNode;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.compiler.function.RGenFunction;
import me.kuwg.re.error.errors.function.RFunctionGenericsError;
import me.kuwg.re.error.errors.function.RFunctionIsVoidError;
import me.kuwg.re.error.errors.function.RFunctionNotFoundError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.NoneBuiltinType;
import me.kuwg.re.type.generic.GenericType;
import me.kuwg.re.type.iterable.arr.ArrayType;
import me.kuwg.re.type.ptr.PointerType;

import java.util.*;

public class FunctionCallNode extends ValueNode {
    private final String name;
    private final List<ValueNode> parameters;

    public FunctionCallNode(final int line, final String name, final List<ValueNode> parameters) {
        super(line);
        this.name = name;
        this.parameters = parameters;
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

        RFunction fn = cctx.getFunction(name, callTypes);

        if (fn instanceof RGenFunction genFn) {
            return compileGeneric(cctx, genFn, callTypes, getting);
        }

        if (fn == null) {
            return throwNotFound(callTypes);
        }

        return emitCall(cctx, fn, argRegs, callTypes, getting);
    }

    private String compileGeneric(CompilationContext cctx, RGenFunction genFn, List<TypeRef> callTypes, boolean getting) {
        validateGenericUsage(genFn);

        Map<String, TypeRef> bindings = inferGenericTypes(genFn, callTypes);
        for (String tp : genFn.typeParameters()) {
            if (!bindings.containsKey(tp)) {
                new RFunctionGenericsError("Could not infer type parameter " + tp, line).raise();
            }
        }

        List<FunctionParameter> concreteParams = new ArrayList<>();
        for (var p : genFn.parameters()) {
            concreteParams.add(new FunctionParameter(p.name(), p.mutable(), substituteConcrete(p.type(), bindings)));
        }
        TypeRef concreteReturnType = substituteConcrete(genFn.returnType(), bindings);

        RFunction existing = genFn.getInstantiation(callTypes);
        if (existing == null) {
            FunctionDeclarationNode concreteFnNode = new FunctionDeclarationNode(
                    line, true, false, genFn.name(), concreteParams, concreteReturnType, genFn.block()
            );


            concreteFnNode.compile(cctx);

            RFunction concreteFn = cctx.getFunction(genFn.name(), concreteParams.stream().map(FunctionParameter::type).toList());

            genFn.addInstantiation(callTypes, concreteFn);
            existing = concreteFn;
        }

        List<String> argRegs = new ArrayList<>();
        for (ValueNode param : parameters) argRegs.add(param.compileAndGet(cctx));

        return emitCall(cctx, existing, argRegs, concreteParams.stream().map(FunctionParameter::type).toList(), getting);
    }

    private String emitCall(CompilationContext cctx, RFunction fn, List<String> argRegs, List<TypeRef> callTypes, boolean getting) {
        if (fn.returnType() instanceof NoneBuiltinType && getting) {
            throwVoid(fn, callTypes);
        }

        if (argRegs == null) {
            argRegs = new ArrayList<>();
            callTypes = new ArrayList<>();
            for (ValueNode param : parameters) {
                argRegs.add(param.compileAndGet(cctx));
                callTypes.add(param.getType());
            }
        }

        for (int i = 0; i < parameters.size(); i++) {
            TypeRef expected = fn.parameters().get(i).type();

            if (containsGeneric(expected)) {
                return new RFunctionGenericsError("Generic type leaked into concrete call", line).raise();
            }

            if (!callTypes.get(i).equals(expected)) {
                CastNode cast = new CastNode(line, expected, parameters.get(i));
                argRegs.set(i, cast.compileAndGet(cctx));
                callTypes.set(i, expected);
            }
        }

        StringBuilder sb = new StringBuilder();
        String result = null;

        if (!(fn.returnType() instanceof NoneBuiltinType)) {
            result = cctx.nextRegister();
            sb.append(result).append(" = ");
        }

        sb.append("call ").append(fn.returnType().getLLVMName()).append(" @").append(fn.llvmName()).append("(");

        for (int i = 0; i < argRegs.size(); i++) {
            sb.append(callTypes.get(i).getLLVMName()).append(" ").append(argRegs.get(i));
            if (i < argRegs.size() - 1) sb.append(", ");
        }

        sb.append(")");
        cctx.emit(sb.toString());

        setType(fn.returnType());
        return fn.returnType() instanceof NoneBuiltinType ? "%void_" + cctx.nextRegister() : result;
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
                new RFunctionGenericsError(
                        "Conflicting types for " + gen.name() + ": " + existing.getName() + " vs " + callType.getName(),
                        line
                ).raise();
            }
        } else if (paramType instanceof ArrayType arr && callType instanceof ArrayType callArr) {
            inferTypeBindings(arr.inner(), callArr.inner(), map);
        } else if (paramType instanceof PointerType ptr && callType instanceof PointerType callPtr) {
            inferTypeBindings(ptr.inner(), callPtr.inner(), map);
        }
    }

    private TypeRef substituteConcrete(TypeRef type, Map<String, TypeRef> map) {
        if (type instanceof GenericType gen) {
            TypeRef resolved = map.get(gen.name());
            if (resolved == null) {
                return new RFunctionGenericsError("Unresolved generic type: " + gen.name(), line).raise();
            }
            return resolved;
        } else if (type instanceof PointerType ptr) {
            return new PointerType(substituteConcrete(ptr.inner(), map));
        } else if (type instanceof ArrayType arr) {
            return new ArrayType(arr.size(), substituteConcrete(arr.inner(), map));
        }
        return type;
    }

    private boolean containsGeneric(TypeRef type) {
        if (type instanceof GenericType) return true;
        if (type instanceof ArrayType arr) return containsGeneric(arr.inner());
        if (type instanceof PointerType ptr) return containsGeneric(ptr.inner());
        return false;
    }

    private void validateGenericUsage(RGenFunction fn) {
        Set<String> allowed = new HashSet<>(fn.typeParameters());
        for (var p : fn.parameters()) {
            if (p.type() instanceof GenericType g && !allowed.contains(g.name())) {
                new RFunctionGenericsError("Unknown type parameter: " + g.name(), line).raise();
            }
        }
        if (fn.returnType() instanceof GenericType g && !allowed.contains(g.name())) {
            new RFunctionGenericsError("Unknown type parameter: " + g.name(), line).raise();
        }
    }

    private <T> T throwNotFound(List<TypeRef> types) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < types.size(); i++) {
            sb.append(types.get(i).getName());
            if (i < types.size() - 1) sb.append(", ");
        }
        sb.append(")");
        return new RFunctionNotFoundError(name, sb.toString(), line).raise();
    }

    private void throwVoid(RFunction fn, List<TypeRef> types) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < types.size(); i++) {
            sb.append(types.get(i).getName());
            if (i < types.size() - 1) sb.append(", ");
        }
        sb.append(")");
        new RFunctionIsVoidError(fn.name(), sb.toString(), line).raise();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Function Call: ").append(name).append("\n")
                .append(indent).append("\t").append("Parameters:").append("\n");

        parameters.forEach(p -> p.write(sb, indent + "\t\t"));
    }
}
