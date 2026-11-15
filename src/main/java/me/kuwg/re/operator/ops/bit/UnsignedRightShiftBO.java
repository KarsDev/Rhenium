package me.kuwg.re.operator.ops.bit;

import me.kuwg.re.operator.BinaryOperator;

public final class UnsignedRightShiftBO extends ShiftBO {
    public static final BinaryOperator INSTANCE = new UnsignedRightShiftBO();

    UnsignedRightShiftBO() {
        super(">>>", "lshr");
    }

    @Override
    protected long fold(final long value, final int shift, final int width) {
        return unsigned(value, width) >>> shift;
    }
}
