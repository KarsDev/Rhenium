package me.kuwg.re.type.trait;

import me.kuwg.re.ast.nodes.function.declaration.FunctionParameter;
import me.kuwg.re.compiler.trait.TraitFunction;
import me.kuwg.re.error.errors.RInternalError;
import me.kuwg.re.type.TypeRef;

import java.util.*;
import java.util.function.Function;

public final class TraitType implements TypeRef {
    private boolean resolved = false;

    private final String name;
    private Map<String, TraitFunction> functions;

    public TraitType(final String name, final Map<String, TraitFunction> functions) {
        this.name = name;
        this.functions = new LinkedHashMap<>(functions);
    }

    public Map<String, TraitFunction> getFunctions() {
        return functions;
    }

    public void setFunctions(final Map<String, TraitFunction> functions) {
        this.functions = new LinkedHashMap<>(functions);
    }

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
    public boolean equals(final Object other) {
        throw new RInternalError();
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, functions);
    }

    @Override
    public TypeRef resolve(final Function<String, TypeRef> resolver) {
        if (resolved) {
            return this;
        }

        resolved = true;

        Map<String, TraitFunction> resolvedFunctions = new LinkedHashMap<>(functions.size());

        for (Map.Entry<String, TraitFunction> entry : functions.entrySet()) {
            TraitFunction function = entry.getValue();

            List<FunctionParameter> params = new ArrayList<>(function.params().size());
            for (FunctionParameter param : function.params()) {
                params.add(new FunctionParameter(
                        param.name(),
                        param.mutable(),
                        param.type().resolve(resolver)
                ));
            }

            resolvedFunctions.put(entry.getKey(), new TraitFunction(
                    function.getName(),
                    params,
                    function.getReturnType().resolve(resolver)
            ));
        }

        this.functions = resolvedFunctions;
        return this;
    }
}