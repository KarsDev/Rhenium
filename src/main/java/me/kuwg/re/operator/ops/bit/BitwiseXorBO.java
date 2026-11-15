package me.kuwg.re.operator.ops.bit;

import me.kuwg.re.operator.BinaryOperator;

public final class BitwiseXorBO extends BitwiseBO {
    public static final BinaryOperator INSTANCE = new BitwiseXorBO();

    BitwiseXorBO() {
        super(5, "^", "xor");
    }

    @Override
    protected long apply(final long l, final long r) {
        return l ^ r;
    }
}
