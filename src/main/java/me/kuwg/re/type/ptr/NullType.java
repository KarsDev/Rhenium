package me.kuwg.re.type.ptr;

import me.kuwg.re.type.TypeRef;

public final class NullType implements TypeRef {
    public static final TypeRef INSTANCE = new NullType();

    private NullType() {
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public boolean isCompatibleWith(final TypeRef other) {
        return other.isPointer();
    }

    @Override
    public int getSize() {
        return 8;
    }

    @Override
    public String getName() {
        return "null";
    }

    @Override
    public String getLLVMName() {
        return "null";
    }

    @Override
    public boolean equals(final TypeRef other) {
        return other instanceof NullType;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
