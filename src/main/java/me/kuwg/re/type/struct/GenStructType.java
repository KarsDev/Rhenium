package me.kuwg.re.type.struct;

import me.kuwg.re.compiler.generic.TypeParameter;
import me.kuwg.re.error.errors.RInternalError;
import me.kuwg.re.type.TypeRef;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static me.kuwg.re.type.struct.StructType.alignTo;

public record GenStructType(List<TypeParameter> genericTypes, String name,
                            List<TypeRef> fieldTypes) implements TypeRef {
    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public boolean isCompatibleWith(final TypeRef other) {
        if (!(other instanceof GenStructType s)) return false;
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
        return "%struct." + name;
    }

    @Override
    public String getMangledName() {
        throw new RInternalError();
    }

    @Override
    public @NotNull String toString() {
        return "struct " + name;
    }

    @Override
    public String getZeroValue() {
        return "zeroinitializer";
    }

    @Override
    public boolean equals(final TypeRef o) {
        if (!(o instanceof final GenStructType type)) return false;

        return Objects.equals(name, type.name) && Objects.equals(fieldTypes, type.fieldTypes);
    }

    @Override
    public TypeRef resolve(final Function<String, TypeRef> resolver) {
        return new GenStructType(genericTypes, name, fieldTypes.stream().map(type -> type.resolve(resolver)).toList());
    }
}