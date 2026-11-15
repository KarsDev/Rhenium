package me.kuwg.re.operator.ops.bit;

import me.kuwg.re.operator.BinaryOperator;

public final class BitwiseAndBO extends BitwiseBO {
    public static final BinaryOperator INSTANCE = new BitwiseAndBO();

    BitwiseAndBO() {
        super(6, "&", "and");
    }

    @Override
    protected long apply(final long l, final long r) {
        return l & r;
    }
}
