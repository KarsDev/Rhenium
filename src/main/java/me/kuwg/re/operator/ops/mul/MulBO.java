package me.kuwg.re.operator.ops.mul;

import me.kuwg.re.operator.BinaryOperator;

public final class MulBO extends ArithmeticBO {
    public static final BinaryOperator INSTANCE = new MulBO();

    MulBO() {
        super("*", "mul", "fmul", ZeroDivisor.ALLOWED);
    }

    @Override
    protected long applyLong(final long l, final long r) {
        return l * r;
    }

    @Override
    protected float applyFloat(final float l, final float r) {
        return l * r;
    }

    @Override
    protected double applyDouble(final double l, final double r) {
        return l * r;
    }
}
