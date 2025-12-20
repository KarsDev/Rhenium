package me.kuwg.re.compiler.function;

import me.kuwg.re.ast.nodes.blocks.BlockNode;
import me.kuwg.re.ast.nodes.function.FunctionParameter;
import me.kuwg.re.type.TypeRef;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RGenFunction extends RFunction {
    private final List<String> typeParameters;
    private final BlockNode block;

    private final Map<List<TypeRef>, RFunction> instantiations = new HashMap<>();

    public RGenFunction(final String llvmName, final String name, final List<String> typeParameters, final TypeRef returnType,
                        final List<FunctionParameter> parameters, final BlockNode block) {
        super(llvmName, name, returnType, parameters);
        this.typeParameters = typeParameters;
        this.block = block;
    }

    public List<String> typeParameters() {
        return typeParameters;
    }

    public BlockNode block() {
        return block;
    }

    public RFunction getInstantiation(List<TypeRef> concreteTypes) {
        return instantiations.get(concreteTypes);
    }

    public void addInstantiation(List<TypeRef> concreteTypes, RFunction fn) {
        instantiations.put(concreteTypes, fn);
    }
}
