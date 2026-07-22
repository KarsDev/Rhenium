package me.kuwg.re.type.struct;

import me.kuwg.re.type.TypeRef;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class StructType implements TypeRef {
    private final String name;
    private final List<TypeRef> fieldTypes;

    public StructType(String name, List<TypeRef> fieldTypes) {
        this.name = name;
        this.fieldTypes = fieldTypes;
    }

    private boolean resolved = false;

    static long alignTo(long value, long alignment) {
        return (value + alignment - 1) & -alignment;
    }

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
        return name;
    }

    @Override
    public String getZeroValue() {
        return "zeroinitializer";
    }

    @Override
    public @NotNull String toString() {
        return "struct " + name;
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof final StructType type)) return false;

        return Objects.equals(name, type.name) && Objects.equals(fieldTypes, type.fieldTypes);
    }

    @Override
    public TypeRef resolve(final Function<String, TypeRef> resolver) {
        if (resolved) {
            return this;
        }

        resolved = true;
        this.fieldTypes.replaceAll(t -> t.resolve(resolver));
        return this;
    }

    public String name() {
        return name;
    }

    public List<TypeRef> getFieldTypes() {
        return fieldTypes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, fieldTypes);
    }

}
