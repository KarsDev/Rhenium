package me.kuwg.re.type.trait;

import me.kuwg.re.ast.nodes.function.declaration.FunctionParameter;
import me.kuwg.re.compiler.trait.TraitFunction;
import me.kuwg.re.error.errors.RInternalError;
import me.kuwg.re.type.TypeRef;

import java.util.*;
import java.util.function.Function;

public record TraitType(String name, Map<String, TraitFunction> functions) implements TypeRef {
    @Override
    public boolean isPrimitive() {
        throw new RInternalError();
    }

    @Override
    public boolean isCompatibleWith(final TypeRef other) {
        throw new RInternalError();
    }

    @Override
    public long getSize() {
        throw new RInternalError();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getLLVMName() {
        throw new RInternalError();
    }

    @Override
    public String getMangledName() {
        throw new RInternalError();
    }

    @Override
    public String getZeroValue() {
        throw new RInternalError();
    }

    @Override
    public boolean equals(final TypeRef o) {
        throw new RInternalError();
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(name);
        result = 31 * result + Objects.hashCode(functions);
        return result;
    }

    @Override
    public TypeRef resolve(final Function<String, TypeRef> resolver) {
        Map<String, TraitFunction> resolvedFunctions = new LinkedHashMap<>(functions.size());

        functions.forEach((key, function) -> {
            List<FunctionParameter> resolvedParams = new ArrayList<>(function.params().size());
            for (FunctionParameter param : function.params()) {
                resolvedParams.add(new FunctionParameter(
                        param.name(),
                        param.mutable(),
                        param.type().resolve(resolver)
                ));
            }
            resolvedFunctions.put(
                    key,
                    new TraitFunction(
                            function.getName(),
                            resolvedParams,
                            function.getReturnType().resolve(resolver)
                    )
            );
        });

        return new TraitType(name, resolvedFunctions);
    }
}
