package me.kuwg.re.type.ptr;

import me.kuwg.re.type.TypeRef;

import java.util.Objects;
import java.util.function.Function;

public final class PointerType implements TypeRef {
    private TypeRef inner;

    public PointerType(TypeRef inner) {
        this.inner = inner;
    }

    public TypeRef getInner() {
        return inner;
    }

    @Override
    public boolean isPrimitive() {
        return inner.isPrimitive();
    }

    @Override
    public boolean isCompatibleWith(final TypeRef other) {
        return other instanceof PointerType ptr && ptr.inner.isCompatibleWith(inner);
    }

    @Override
    public long getSize() {
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
    public String getMangledName() {
        return "ptr_" + getInner().getMangledName();
    }

    @Override
    public String getZeroValue() {
        return "null";
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof final PointerType that)) return false;

        return Objects.equals(inner, that.inner);
    }

    @Override
    public TypeRef resolve(final Function<String, TypeRef> resolver) {
        inner = inner.resolve(resolver);
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(inner);
    }

    @Override
    public String toString() {
        return "PointerType[" +
                "getInner=" + inner + ']';
    }
}
