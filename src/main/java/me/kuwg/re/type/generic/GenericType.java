package me.kuwg.re.type.generic;

import me.kuwg.re.error.errors.RInternalError;
import me.kuwg.re.type.TypeRef;

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
    public int getSize() {
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
    public boolean equals(final TypeRef other) {
        throw new RInternalError();
    }
}
