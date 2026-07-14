package me.kuwg.re.type.iterable.arr;

import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.generic.GenericType;
import me.kuwg.re.type.iterable.IterableTypeRef;

import java.util.Objects;

public record ArrayType(long size, TypeRef inner) implements IterableTypeRef {
    public static final int UNKNOWN_SIZE = -1;

    @Override
    public boolean isPrimitive() {
        return inner.isPrimitive();
    }

    @Override
    public boolean isCompatibleWith(final TypeRef other) {
        if (!(other instanceof ArrayType arr)) return false;
        if (arr.inner instanceof GenericType || inner instanceof GenericType) return false;
        if (!arr.inner.isCompatibleWith(inner)) return false;
        return arr.size == UNKNOWN_SIZE || size == UNKNOWN_SIZE || arr.size == size;
    }

    @Override
    public long getSize() {
        if (size == UNKNOWN_SIZE) {
            return 8;
        }
        return size * inner.getSize();
    }

    @Override
    public String getName() {
        return "arr -> " + inner.getName();
    }

    @Override
    public String getLLVMName() {
        if (size == UNKNOWN_SIZE) return inner.getLLVMName() + "*";

        String innerName;
        innerName = inner instanceof ArrayType arr ? arr.getLLVMName() : inner.getLLVMName();

        return "[" + size + " x " + innerName + "]";
    }

    @Override
    public String getMangledName() {
        return "arr" + size() + "_" + inner().getMangledName();
    }

    public boolean isDynamic() {
        return size == UNKNOWN_SIZE;
    }

    public boolean isStatic() {
        return !isDynamic();
    }

    @Override
    public boolean equals(final TypeRef o) {
        if (!(o instanceof final ArrayType arrayType)) return false;

        return (size == -1 || arrayType.size == -1 || size == arrayType.size) && Objects.equals(inner, arrayType.inner);
    }

    @Override
    public int hashCode() {
        long result = size;
        result = 31 * result + Objects.hashCode(inner);
        return (int) result;
    }
}
