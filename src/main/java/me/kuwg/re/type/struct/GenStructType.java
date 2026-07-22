package me.kuwg.re.type.struct;

import me.kuwg.re.compiler.generic.TypeParameter;
import me.kuwg.re.error.errors.RInternalError;
import me.kuwg.re.type.TypeRef;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static me.kuwg.re.type.struct.StructType.alignTo;

public final class GenStructType implements TypeRef {
    private boolean resolved = false;

    private final List<TypeParameter> genericTypes;
    private final String name;
    private final List<TypeRef> fieldTypes;

    public GenStructType(final List<TypeParameter> genericTypes,
                         final String name,
                         final List<TypeRef> fieldTypes) {
        this.genericTypes = new ArrayList<>(genericTypes);
        this.name = name;
        this.fieldTypes = new ArrayList<>(fieldTypes);
    }

    public List<TypeParameter> getGenericTypes() {
        return genericTypes;
    }

    public List<TypeRef> getFieldTypes() {
        return fieldTypes;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public boolean isCompatibleWith(final TypeRef other) {
        return other instanceof GenStructType s && Objects.equals(name, s.name);
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
    public boolean equals(final Object other) {
        if (!(other instanceof GenStructType s)) {
            return false;
        }

        return Objects.equals(name, s.name)
                && Objects.equals(genericTypes, s.genericTypes)
                && Objects.equals(fieldTypes, s.fieldTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(genericTypes, name, fieldTypes);
    }

    @Override
    public TypeRef resolve(final Function<String, TypeRef> resolver) {
        if (resolved) {
            return this;
        }

        resolved = true;

        this.fieldTypes.replaceAll(f -> f.resolve(resolver));
        return this;
    }
}