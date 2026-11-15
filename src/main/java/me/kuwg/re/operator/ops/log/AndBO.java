package me.kuwg.re.operator.ops.log;

import me.kuwg.re.operator.BinaryOperator;

public final class AndBO extends LogicalBO {
    public static final BinaryOperator INSTANCE = new AndBO();

    AndBO() {
        super(2, "and", "and");
    }

    @Override
    protected boolean apply(final boolean l, final boolean r) {
        return l && r;
    }
}
