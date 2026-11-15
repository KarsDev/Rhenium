package me.kuwg.re.operator.ops.bit;

import me.kuwg.re.operator.BinaryOperator;

public final class BitwiseOrBO extends BitwiseBO {
    public static final BinaryOperator INSTANCE = new BitwiseOrBO();

    BitwiseOrBO() {
        super(4, "|", "or");
    }

    @Override
    protected long apply(final long l, final long r) {
        return l | r;
    }
}
