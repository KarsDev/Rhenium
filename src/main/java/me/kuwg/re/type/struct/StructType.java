package me.kuwg.re.type.struct;

import me.kuwg.re.type.TypeRef;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public record StructType(String name, List<TypeRef> fieldTypes) implements TypeRef {
    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public boolean isCompatibleWith(final TypeRef other) {
        if (!(other instanceof StructType s)) return false;
        return Objects.equals(name, s.name);
    }

    @Override
    public long getSize() {
        long offset = 0;
        long maxAlignment = 1;

        for (TypeRef field : fieldTypes) {
            long alignment = field.getAlignment();
            maxAlignment = Math.max(maxAlignment, alignment);

            offset = alignTo(offset, alignment);
            offset += field.getSize();
        }

        return alignTo(offset, maxAlignment);
    }

    @Override
    public long getAlignment() {
        long max = 1;

        for (TypeRef field : fieldTypes) {
            max = Math.max(max, field.getAlignment());
        }

        return max;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getLLVMName() {
        return "%struct." + getMangledName();
    }

    @Override
    public String getMangledName() {
        if (fieldTypes().isEmpty()) {
            return name();
        }

        StringBuilder sb = new StringBuilder(name());
        for (TypeRef t : fieldTypes()) {
            sb.append("_").append(t.getMangledName());
        }
        return sb.toString();
    }

    @Override
    public @NotNull String toString() {
        return "struct " + name;
    }

    @Override
    public boolean equals(final TypeRef o) {
        if (!(o instanceof final StructType type)) return false;

        return Objects.equals(name, type.name) && Objects.equals(fieldTypes, type.fieldTypes);
    }

    static long alignTo(long value, long alignment) {
        return (value + alignment - 1) & -alignment;
    }
}