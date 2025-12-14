package me.kuwg.re.operator.ops.bit;

import me.kuwg.re.error.errors.expr.RUnsupportedBinaryExpressionError;
import me.kuwg.re.operator.BinaryOperator;
import me.kuwg.re.operator.BinaryOperatorContext;
import me.kuwg.re.operator.result.BOResult;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.ByteBuiltinType;
import me.kuwg.re.type.builtin.IntBuiltinType;
import me.kuwg.re.type.builtin.LongBuiltinType;
import me.kuwg.re.type.builtin.ShortBuiltinType;

public final class BitwiseAndBO extends BinaryOperator {
    public static final BinaryOperator INSTANCE = new BitwiseAndBO();

    BitwiseAndBO() {
        super(7, "&");
    }

    @Override
    public BOResult compile(final BinaryOperatorContext c) {
        TypeRef leftType = c.leftType();
        TypeRef rightType = c.rightType();

        String llvmType;

        if (leftType instanceof ByteBuiltinType && rightType instanceof ByteBuiltinType) {
            llvmType = "i8";
        } else if (leftType instanceof ShortBuiltinType && rightType instanceof ShortBuiltinType) {
            llvmType = "i16";
        } else if (leftType instanceof IntBuiltinType && rightType instanceof IntBuiltinType) {
            llvmType = "i32";
        } else if (leftType instanceof LongBuiltinType && rightType instanceof LongBuiltinType) {
            llvmType = "i64";
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
        c.cctx().emit(resReg + " = and " + llvmType + " " + leftReg + ", " + rightReg);

        return res(resReg, leftType);
    }
}
