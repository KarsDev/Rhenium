package me.kuwg.re.type.ptr;

import me.kuwg.re.type.TypeRef;

import java.util.Objects;

public record PointerType(TypeRef inner) implements TypeRef {
    @Override
    public boolean isPrimitive() {
        return inner.isPrimitive();
    }

    @Override
    public boolean isCompatibleWith(final TypeRef other) {
        return other instanceof PointerType ptr && ptr.inner.isCompatibleWith(inner);
    }

    @Override
    public int getSize() {
        return 8;
    }

    @Override
    public String getName() {
        return "ptr -> " + inner.getName();
    }

    @Override
    public String getLLVMName() {
        return inner.getLLVMName() + "*";
    }

    @Override
    public boolean equals(final TypeRef o) {
        if (!(o instanceof final PointerType that)) return false;

        return Objects.equals(inner, that.inner);
    }
}
