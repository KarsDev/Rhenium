package me.kuwg.re.operator.ops.instance;

import me.kuwg.re.operator.BinaryOperator;
import me.kuwg.re.operator.BinaryOperatorContext;
import me.kuwg.re.operator.result.BOResult;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;

public class IsBO extends BinaryOperator {
    public static final BinaryOperator INSTANCE = new IsBO();
    protected IsBO() {
        super(9, "is");
    }

    @Override
    public BOResult compile(final BinaryOperatorContext c) {
        TypeRef right = c.rightType();
        TypeRef left = c.rightType();

        boolean value = left.isCompatibleWith(right);

        return res(Boolean.toString(value), BuiltinTypes.BOOL.getType());
    }
}
