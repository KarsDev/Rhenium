package me.kuwg.re.operator.ops.mul;

import me.kuwg.re.error.errors.expr.RUnsupportedBinaryExpressionError;
import me.kuwg.re.operator.BinaryOperator;
import me.kuwg.re.operator.BinaryOperatorContext;
import me.kuwg.re.operator.result.BOResult;
import me.kuwg.re.type.builtin.*;

public final class ModBO extends BinaryOperator {
    public static final BinaryOperator INSTANCE = new ModBO();

    ModBO() {
        super(12, "%");
    }

    @Override
    public BOResult compile(final BinaryOperatorContext c) {
        var leftType = c.leftType();
        var rightType = c.rightType();

        String llvmType;
        boolean isFloating = false;

        if (leftType instanceof ByteBuiltinType && rightType instanceof ByteBuiltinType) {
            llvmType = "i8";
        } else if (leftType instanceof ShortBuiltinType && rightType instanceof ShortBuiltinType) {
            llvmType = "i16";
        } else if (leftType instanceof IntBuiltinType && rightType instanceof IntBuiltinType) {
            llvmType = "i32";
        } else if (leftType instanceof LongBuiltinType && rightType instanceof LongBuiltinType) {
            llvmType = "i64";
        } else if (leftType instanceof FloatBuiltinType && rightType instanceof FloatBuiltinType) {
            llvmType = "float";
            isFloating = true;
        } else if (leftType instanceof DoubleBuiltinType && rightType instanceof DoubleBuiltinType) {
            llvmType = "double";
            isFloating = true;
        } else {
            return new RUnsupportedBinaryExpressionError(
                    leftType.getName(), getSymbol(), rightType.getName(), c.line()
            ).raise();
        }

        String leftReg = c.leftReg();
        String rightReg = c.rightReg();
        if (leftReg.matches("\\d+")) {
            String tmp = c.cctx().nextRegister();
            c.cctx().emit(tmp + " = add " + llvmType + " 0, " + leftReg);
            leftReg = tmp;
        }
        if (rightReg.matches("\\d+")) {
            String tmp = c.cctx().nextRegister();
            c.cctx().emit(tmp + " = add " + llvmType + " 0, " + rightReg);
            rightReg = tmp;
        }

        String resReg = c.cctx().nextRegister();
        if (isFloating) {
            c.cctx().emit(resReg + " = frem " + llvmType + " " + leftReg + ", " + rightReg);
        } else {
            c.cctx().emit(resReg + " = srem " + llvmType + " " + leftReg + ", " + rightReg);
        }


        return res(resReg, leftType);
    }
}
