package me.kuwg.re.operator.ops.bit;

import me.kuwg.re.error.errors.expr.RUnsupportedBinaryExpressionError;
import me.kuwg.re.operator.BinaryOperator;
import me.kuwg.re.operator.BinaryOperatorContext;
import me.kuwg.re.operator.result.BOResult;
import me.kuwg.re.type.TypeRef;

public final class LeftShiftBO extends BinaryOperator {
    public static final BinaryOperator INSTANCE = new LeftShiftBO();

    LeftShiftBO() {
        super(10, "<<");
    }

    @Override
    public BOResult compile(final BinaryOperatorContext c) {
        TypeRef leftType = c.leftType();
        TypeRef rightType = c.rightType();

        if (!isInteger(leftType) || !isInteger(rightType)) {
            return new RUnsupportedBinaryExpressionError(
                    leftType.getName(), getSymbol(), rightType.getName(), c.line()
            ).raise();
        }

        TypeRef resultType = promoteNumeric(leftType, rightType);
        String llvmType = resultType.getLLVMName();

        String leftReg = convertToType(c.leftReg(), leftType, resultType, c);
        String rightReg = convertToType(c.rightReg(), rightType, resultType, c);

        String shiftReg = rightReg;
        if (!"i32".equals(rightType.getLLVMName())) {
            shiftReg = c.cctx().nextRegister();
            c.cctx().emit(shiftReg + " = trunc " + rightReg + " " + rightType.getLLVMName() + " to i32");
        }

        String resReg = c.cctx().nextRegister();
        c.cctx().emit(resReg + " = shl " + llvmType + " " + leftReg + ", " + shiftReg);

        return res(resReg, resultType);
    }
}
