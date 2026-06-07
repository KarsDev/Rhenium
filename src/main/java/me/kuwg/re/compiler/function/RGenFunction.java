package me.kuwg.re.compiler.function;

import me.kuwg.re.ast.nodes.blocks.BlockNode;
import me.kuwg.re.ast.nodes.function.declaration.FunctionParameter;
import me.kuwg.re.compiler.generic.TypeParameter;
import me.kuwg.re.type.TypeRef;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RGenFunction extends RFunction {
    private final List<TypeParameter> typeParameters;
    private final BlockNode block;

    private final Map<String, RFunction> instantiations = new HashMap<>();

    public RGenFunction(final String llvmName, final String name, final List<TypeParameter> typeParameters, final TypeRef returnType,
                        final List<FunctionParameter> parameters, final BlockNode block) {
        super(llvmName, name, returnType, parameters);
        this.typeParameters = typeParameters;
        this.block = block;
    }

    public List<TypeParameter> typeParameters() {
        return typeParameters;
    }

    public BlockNode block() {
        return block;
    }

    public RFunction getInstantiation(Map<String, TypeRef> bindings) {
        return instantiations.get(signature(bindings));
    }

    public void addInstantiation(Map<String, TypeRef> bindings, RFunction fn) {
        instantiations.put(signature(bindings), fn);
    }

    private String signature(Map<String, TypeRef> bindings) {
        return bindings.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue().getName())
                .reduce((a, b) -> a + "," + b)
                .orElse("");
    }
}
