package me.kuwg.re.operator.ops.log;

import me.kuwg.re.operator.BinaryOperator;

public final class OrBO extends LogicalBO {
    public static final BinaryOperator INSTANCE = new OrBO();

    OrBO() {
        super(1, "or", "or");
    }

    @Override
    protected boolean apply(final boolean l, final boolean r) {
        return l || r;
    }
}
