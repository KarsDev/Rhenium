package me.kuwg.re.ast.nodes.function.call;

import me.kuwg.re.ast.nodes.function.declaration.FunctionDeclarationNode;
import me.kuwg.re.ast.nodes.function.declaration.FunctionParameter;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.compiler.function.RGenFunction;
import me.kuwg.re.error.errors.function.RFunctionGenericsError;
import me.kuwg.re.type.TypeRef;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class GenericFunctionCallNode extends FunCall {
    private final List<TypeRef> genericTypes;

    public GenericFunctionCallNode(final String fileName, int line, String name, List<TypeRef> genericTypes, List<ValueNode> parameters) {
        super(fileName, line, name, parameters);
        this.genericTypes = genericTypes;
    }

    @Override
    public void replaceGenerics(Map<String, TypeRef> generics, CompilationContext cctx) {
        parameters.forEach(p -> p.replaceGenerics(generics, cctx));
    }

    @Override
    public void compile(CompilationContext cctx) {
        compile0(cctx, false);
    }

    @Override
    public String compileAndGet(CompilationContext cctx) {
        return compile0(cctx, true);
    }

    @Override
    public ValueNode clone() {
        List<TypeRef> genericCloned = new ArrayList<>(genericTypes.size());
        genericCloned.addAll(genericTypes);

        List<ValueNode> paramsCloned = new ArrayList<>();
        IntStream.range(0, parameters.size()).forEach(i -> paramsCloned.add(i, parameters.get(i).clone()));

        return new GenericFunctionCallNode(fileName, line, name, genericCloned, paramsCloned);
    }

    private String compile0(CompilationContext cctx, boolean getting) {
        cctx.emit("; Generic function call");
        List<String> argRegs = new ArrayList<>(parameters.size());
        List<TypeRef> callTypes = new ArrayList<>(parameters.size());

        for (ValueNode param : parameters) {
            argRegs.add(param.compileAndGet(cctx));
            callTypes.add(param.getType());
        }

        String qualifiedName = cctx.qualify(name);
        RFunction fn = cctx.getFunction(qualifiedName, callTypes);

        if (!(fn instanceof RGenFunction genFn)) {
            return new RFunctionGenericsError("Function is not generic or is not found", fileName, line).raise();
        }

        return compileWithExplicitGenerics(cctx, genFn, argRegs, getting);
    }

    private String compileWithExplicitGenerics(CompilationContext cctx, RGenFunction genFn, List<String> argRegs, boolean getting) {
        if (genericTypes.size() != genFn.typeParameters().size()) {
            return new RFunctionGenericsError(
                    "Expected " + genFn.typeParameters().size() + " generic arguments, got " + genericTypes.size(),
                    fileName, line
            ).raise();
        }

        validateGenericUsage(genFn);

        Map<String, TypeRef> bindings = new LinkedHashMap<>();
        for (int i = 0; i < genFn.typeParameters().size(); i++) {
            bindings.put(genFn.typeParameters().get(i).name(), genericTypes.get(i));
        }

        validateGenericConstraints(cctx, genFn, bindings);

        List<FunctionParameter> concreteParams = new ArrayList<>();
        for (var p : genFn.parameters()) {
            concreteParams.add(new FunctionParameter(p.name(), p.mutable(), substituteConcrete(p.type(), bindings)));
        }
        TypeRef concreteReturnType = substituteConcrete(genFn.returnType(), bindings);

        RFunction existing = genFn.getInstantiation(bindings);
        if (existing == null) {
            String mangledName = genFn.name() + "__" +
                    genericTypes.stream()
                            .map(TypeRef::getName)
                            .reduce((a, b) -> a + "_" + b)
                            .orElse("");

            FunctionDeclarationNode concreteFnNode = new FunctionDeclarationNode(
                    fileName, line,
                    true,
                    mangledName,
                    concreteParams,
                    concreteReturnType,
                    genFn.block().clone()
            );

            concreteFnNode.replaceGenerics(bindings, cctx);
            concreteFnNode.compile(cctx);

            RFunction concreteFn = cctx.getFunction(
                    concreteFnNode.getQualifiedName(cctx),
                    concreteParams.stream().map(FunctionParameter::type).toList()
            );

            genFn.addInstantiation(bindings, concreteFn);
            existing = concreteFn;
        }

        return emitCall(
                cctx,
                existing,
                argRegs,
                concreteParams.stream().map(FunctionParameter::type).toList(),
                getting
        );
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Generic Function Call: ").append(name).append("\n");

        sb.append(indent).append("\t").append("Generic Types:").append("\n");
        for (TypeRef t : genericTypes) {
            sb.append(indent).append("\t\t").append(t.getName()).append("\n");
        }

        sb.append(indent).append("\t").append("Parameters:").append("\n");
        parameters.forEach(p -> p.write(sb, indent + "\t\t"));
    }
}