package me.kuwg.re.operator.ops.mul;

import me.kuwg.re.operator.BinaryOperator;

public final class ModBO extends ArithmeticBO {
    public static final BinaryOperator INSTANCE = new ModBO();

    ModBO() {
        super("%", "srem", "frem", ZeroDivisor.REJECT_INTEGER);
    }

    @Override
    protected long applyLong(final long l, final long r) {
        return l % r;
    }

    @Override
    protected float applyFloat(final float l, final float r) {
        return l % r;
    }

    @Override
    protected double applyDouble(final double l, final double r) {
        return l % r;
    }
}
