package me.kuwg.re.type.layout;

import me.kuwg.re.error.errors.struct.RStructSizeError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.iterable.arr.ArrayType;
import me.kuwg.re.type.ptr.PointerType;
import me.kuwg.re.type.struct.StructType;

import java.util.HashSet;
import java.util.Set;

public final class LayoutCalculator {
    private final Set<TypeRef> sizing = new HashSet<>();
    private final Set<TypeRef> aligning = new HashSet<>();

    public long size(TypeRef type) {
        if (type instanceof PointerType) {
            return type.getSize();
        }

        if (type instanceof ArrayType arr) {
            long elemAlign = alignment(arr.getInner());
            long elemSize = alignTo(size(arr.getInner()), elemAlign);
            return elemSize * arr.size();
        }

        if (type instanceof StructType st) {
            if (!sizing.add(st)) {
                return new RStructSizeError(
                        "Recursive struct '" + st.getName() + "' has infinite size.", "Unknown in context", -1).raise();
            }

            try {
                long offset = 0;
                long maxAlign = 1;

                for (TypeRef field : st.getFieldTypes()) {
                    long align = alignment(field);
                    maxAlign = Math.max(maxAlign, align);

                    offset = alignTo(offset, align);
                    offset += size(field);
                }

                return alignTo(offset, maxAlign);
            } finally {
                sizing.remove(st);
            }
        }

        return type.getSize();
    }

    public long alignment(TypeRef type) {
        if (type instanceof PointerType) {
            return type.getAlignment();
        }

        if (type instanceof ArrayType arr) {
            return alignment(arr.getInner());
        }

        if (type instanceof StructType st) {
            if (!aligning.add(st)) {
                return new RStructSizeError(
                        "Recursive struct '" + st.getName() + "' has infinite alignment.", "Unknown in context", -1).raise();
            }

            try {
                long max = 1;

                for (TypeRef field : st.getFieldTypes()) {
                    max = Math.max(max, alignment(field));
                }

                return max;
            } finally {
                aligning.remove(st);
            }
        }

        return type.getAlignment();
    }

    private static long alignTo(long value, long alignment) {
        return (value + alignment - 1) & -alignment;
    }
}