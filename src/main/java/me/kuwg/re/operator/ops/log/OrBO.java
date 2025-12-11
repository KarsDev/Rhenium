package me.kuwg.re.operator.ops.log;

import me.kuwg.re.error.errors.expr.RUnsupportedBinaryExpressionError;
import me.kuwg.re.operator.BinaryOperator;
import me.kuwg.re.operator.BinaryOperatorContext;
import me.kuwg.re.operator.result.BOResult;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BoolBuiltinType;
import me.kuwg.re.type.builtin.BuiltinTypes;

public final class OrBO extends BinaryOperator {
    public static final BinaryOperator INSTANCE = new OrBO();

    OrBO() {
        super(3, "or");
    }

    @Override
    public BOResult compile(final BinaryOperatorContext c) {
        TypeRef leftType = c.leftType();
        TypeRef rightType = c.rightType();

        if (!(leftType instanceof BoolBuiltinType) || !(rightType instanceof BoolBuiltinType)) {
            return new RUnsupportedBinaryExpressionError(leftType.getName(), getSymbol(), rightType.getName(), c.line()).raise();
        }

        String leftReg = c.leftReg();
        String rightReg = c.rightReg();
        String resultReg = c.cctx().nextRegister();

        c.cctx().emit(resultReg + " = or i1 " + leftReg + ", " + rightReg);

        return res(resultReg, BuiltinTypes.BOOL.getType());
    }
}
