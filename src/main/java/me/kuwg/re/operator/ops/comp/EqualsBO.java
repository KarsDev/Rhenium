package me.kuwg.re.operator.ops.comp;

import me.kuwg.re.operator.BinaryOperator;

public final class EqualsBO extends EqualityBO {
    public static final BinaryOperator INSTANCE = new EqualsBO();

    EqualsBO() {
        super("==", false);
    }
}
