package me.kuwg.re.type.arr;

import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.iterable.IterableTypeRef;

import java.util.Objects;

public record ArrayType(int size, TypeRef inner) implements IterableTypeRef {
    public static final int UNKNOWN_SIZE = -1;

    @Override
    public boolean isPrimitive() {
        return inner.isPrimitive();
    }

    @Override
    public boolean isCompatibleWith(final TypeRef other) {
        if (!(other instanceof ArrayType arr)) return false;
        if (!arr.inner.isCompatibleWith(inner)) return false;
        return arr.size == UNKNOWN_SIZE || size == UNKNOWN_SIZE || arr.size == size;
    }

    @Override
    public int getSize() {
        return size * inner.getSize();
    }

    @Override
    public String getName() {
        return "arr -> " + inner.getName();
    }

    @Override
    public String getLLVMName() {
        return "[" + size + " x " + inner.getLLVMName() + "]";
    }

    @Override
    public boolean equals(final TypeRef o) {
        if (!(o instanceof final ArrayType arrayType)) return false;

        return size == arrayType.size && Objects.equals(inner, arrayType.inner);
    }

    @Override
    public int hashCode() {
        int result = size;
        result = 31 * result + Objects.hashCode(inner);
        return result;
    }
}
