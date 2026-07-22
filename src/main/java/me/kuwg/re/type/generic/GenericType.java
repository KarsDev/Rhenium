package me.kuwg.re.type.generic;

import me.kuwg.re.error.errors.RInternalError;
import me.kuwg.re.type.TypeRef;

import java.util.function.Function;

public record GenericType(String name) implements TypeRef {
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
        throw new RInternalError();
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
    public boolean equals(Object other) {
        return other instanceof GenericType gen
                && name.equals(gen.name());
    }

    @Override
    public TypeRef resolve(final Function<String, TypeRef> resolver) {
        return this;
    }
}
