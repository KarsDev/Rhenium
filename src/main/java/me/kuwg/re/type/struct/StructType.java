package me.kuwg.re.type.struct;

import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.layout.LayoutCalculator;
import me.kuwg.re.type.union.UnionType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class StructType implements TypeRef {
    private static final LayoutCalculator LAYOUT = new LayoutCalculator();
    private final String name;
    private final List<TypeRef> fieldTypes;
    private boolean resolved = false;

    public StructType(String name, List<TypeRef> fieldTypes) {
        this.name = name;
        this.fieldTypes = fieldTypes;
    }

    static long alignTo(long value, long alignment) {
        return (value + alignment - 1) & -alignment;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public boolean isCompatibleWith(final TypeRef other) {
        if (other instanceof UnionType u) return u.contains(this);
        if (!(other instanceof StructType s)) return false;
        return Objects.equals(name, s.name);
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
    public boolean equals(Object o) {
        return o instanceof StructType s
                && name.equals(s.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
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
}
