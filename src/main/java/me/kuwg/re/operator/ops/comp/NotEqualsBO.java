package me.kuwg.re.operator.ops.comp;

import me.kuwg.re.operator.BinaryOperator;

public final class NotEqualsBO extends EqualityBO {
    public static final BinaryOperator INSTANCE = new NotEqualsBO();

    NotEqualsBO() {
        super("!=", true);
    }
}
