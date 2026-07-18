package me.kuwg.re.type.union;

import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.struct.StructType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public final class UnionType implements TypeRef {
    private final String name;
    private final List<StructType> variants;

    public UnionType(final String name, final @NotNull List<StructType> variants) {
        this.name = name;
        this.variants = variants;
    }

    public List<StructType> variants() {
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

        for (StructType variant : variants) {
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

        for (StructType variant : variants) {
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
    public boolean equals(final TypeRef other) {
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

    public long getPayloadSize() {
        return variants.stream().mapToLong(StructType::getSize).max().orElse(0);
    }
}