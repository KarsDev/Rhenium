package me.kuwg.re.operator.ops.bit;

import me.kuwg.re.operator.BinaryOperator;

public final class LeftShiftBO extends ShiftBO {
    public static final BinaryOperator INSTANCE = new LeftShiftBO();

    LeftShiftBO() {
        super("<<", "shl");
    }

    @Override
    protected long fold(final long value, final int shift, final int width) {
        return value << shift;
    }
}
