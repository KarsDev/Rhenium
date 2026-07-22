package me.kuwg.re.type.union;

import me.kuwg.re.type.TypeRef;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class UnionType implements TypeRef {
    private boolean resolved = false;

    private final String name;
    private final List<TypeRef> variants;

    public UnionType(final String name, final @NotNull List<TypeRef> variants) {
        this.name = name;
        this.variants = variants;
    }

    public List<TypeRef> variants() {
        return variants;
    }

    public boolean contains(final TypeRef type) {
        return variants.stream().anyMatch(v -> v.equals(type));
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public boolean isCompatibleWith(final TypeRef other) {
        if (other instanceof UnionType u) {
            return name.equals(u.name);
        }

        return variants.stream().anyMatch(v -> v.equals(other));
    }

    @Override
    public long getSize() {
        long payloadSize = 0;
        long payloadAlign = 4;

        for (TypeRef variant : variants) {
            payloadSize = Math.max(payloadSize, variant.getSize());
            payloadAlign = Math.max(payloadAlign, variant.getAlignment());
        }

        long offset = 4;

        long padding = (payloadAlign - (offset % payloadAlign)) % payloadAlign;

        return offset + padding + payloadSize;
    }

    @Override
    public long getAlignment() {
        long max = 4;

        for (TypeRef variant : variants) {
            max = Math.max(max, variant.getAlignment());
        }

        return max;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getLLVMName() {
        return "%union." + getMangledName();
    }

    @Override
    public String getMangledName() {
        return name;
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof UnionType u && name.equals(u.name);
    }

    @Override
    public String getZeroValue() {
        return "zeroinitializer";
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public TypeRef resolve(final Function<String, TypeRef> resolver) {
        if (resolved) {
            return this;
        }

        resolved = true;

        this.variants.replaceAll(t -> t.resolve(resolver));
        return this;
    }

    public long payloadSize() {
        long max = 0;
        for (TypeRef variant : variants()) {
            max = Math.max(max, variant.getSize());
        }
        return max;
    }
}