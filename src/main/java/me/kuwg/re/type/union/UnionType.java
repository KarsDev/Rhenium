package me.kuwg.re.type.union;

import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.layout.LayoutCalculator;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class UnionType implements TypeRef {
    private static final LayoutCalculator LAYOUT = new LayoutCalculator();
    private final String name;
    private final List<TypeRef> variants;
    private boolean resolved = false;

    public UnionType(final String name, final @NotNull List<TypeRef> variants) {
        this.name = name;
        this.variants = variants;
    }

    public List<TypeRef> variants() {
        return variants;
    }

    public boolean contains(final TypeRef type) {
        for (final TypeRef v : variants) {
            if (v.equals(type)) return true;
        }
        return false;
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

        return contains(other);
    }

    @Override
    public long getSize() {
        return LAYOUT.size(this);
    }

    @Override
    public long getAlignment() {
        return LAYOUT.alignment(this);
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