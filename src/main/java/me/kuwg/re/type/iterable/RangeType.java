package me.kuwg.re.type.iterable;

import me.kuwg.re.ast.value.ValueNode;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;

import java.util.Objects;

public record RangeType(ValueNode start, ValueNode end, ValueNode step) implements IterableTypeRef {
    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public boolean isCompatibleWith(final TypeRef other) {
        return other instanceof RangeType;
    }

    @Override
    public int getSize() {
        return BuiltinTypes.INT.getType().getSize() * 3;
    }

    @Override
    public String getName() {
        return "range";
    }

    @Override
    public String getLLVMName() {
        return "";
    }

    @Override
    public boolean equals(final TypeRef o) {
        if (!(o instanceof final RangeType rangeType)) return false;

        return Objects.equals(end, rangeType.end) && Objects.equals(step, rangeType.step) && Objects.equals(start, rangeType.start);
    }
}
