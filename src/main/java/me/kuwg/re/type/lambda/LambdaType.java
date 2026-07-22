package me.kuwg.re.type.lambda;

import me.kuwg.re.type.TypeRef;

import java.util.List;
import java.util.function.Function;

public final class LambdaType implements TypeRef {
    private boolean resolved = false;

    private final List<TypeRef> parameters;
    private TypeRef returnType;

    public LambdaType(final List<TypeRef> parameters, final TypeRef returnType) {
        this.parameters = parameters;
        this.returnType = returnType;
    }

    public List<TypeRef> parameters() {
        return parameters;
    }

    public TypeRef returnType() {
        return returnType;
    }

    @Override
    public String getLLVMName() {
        return "ptr";
    }

    @Override
    public String getName() {
        return "lambda";
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof LambdaType l)) return false;

        return returnType.equals(l.returnType) && parameters.equals(l.parameters);
    }

    @Override
    public boolean isCompatibleWith(TypeRef other) {
        return equals(other);
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public long getSize() {
        return 8;
    }

    @Override
    public String getZeroValue() {
        return "null";
    }

    @Override
    public String getMangledName() {
        return "L";
    }

    @Override
    public TypeRef resolve(final Function<String, TypeRef> resolver) {
        if (resolved) {
            return this;
        }

        resolved = true;

        parameters.replaceAll(type -> type.resolve(resolver));
        returnType = returnType.resolve(resolver);
        return this;
    }
}