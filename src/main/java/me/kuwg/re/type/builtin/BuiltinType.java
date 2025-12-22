package me.kuwg.re.type.builtin;

import me.kuwg.re.type.TypeRef;

abstract class BuiltinType implements TypeRef {
    @Override
    public final boolean isPrimitive() {
        return true;
    }

    @Override
    public String getMangledName() {
        return getName();
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(final TypeRef other) {
        return this.getClass().equals(other.getClass());
    }
}
