package me.kuwg.re.type.struct;

import me.kuwg.re.error.errors.RInternalError;
import me.kuwg.re.type.TypeRef;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public record GenStructType(List<String> genericTypes, String name, List<TypeRef> fieldTypes) implements TypeRef {
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
    public int getSize() {
        int size = 0;
        for (TypeRef t : fieldTypes) {
            size += t.getSize();
        }
        return size;
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
    public boolean equals(final TypeRef o) {
        if (!(o instanceof final GenStructType type)) return false;

        return Objects.equals(name, type.name) && Objects.equals(fieldTypes, type.fieldTypes);
    }
}