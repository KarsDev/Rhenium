package me.kuwg.re.operator.ops.comp;

import me.kuwg.re.error.errors.expr.RUnsupportedBinaryExpressionError;
import me.kuwg.re.operator.BinaryOperator;
import me.kuwg.re.operator.BinaryOperatorContext;
import me.kuwg.re.operator.result.BOResult;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;
import me.kuwg.re.type.builtin.DoubleBuiltinType;
import me.kuwg.re.type.builtin.FloatBuiltinType;
import me.kuwg.re.type.builtin.StrBuiltinType;

public class LessOrEqualBO extends BinaryOperator {
    public static final BinaryOperator INSTANCE = new LessOrEqualBO();

    LessOrEqualBO() {
        super(9, "<=");
    }

    @Override
    public BOResult compile(final BinaryOperatorContext c) {
        TypeRef leftType = c.leftType();
        TypeRef rightType = c.rightType();

        if (leftType instanceof StrBuiltinType && rightType instanceof StrBuiltinType) {
            c.cctx().include(-1, null, "string", null);
            String lLen = c.cctx().nextRegister();
            String rLen = c.cctx().nextRegister();

            c.cctx().emit(lLen + " = call i32 @strlen(i8* " + c.leftReg() + ")");
            c.cctx().emit(rLen + " = call i32 @strlen(i8* " + c.rightReg() + ")");

            String resReg = c.cctx().nextRegister();
            c.cctx().emit(resReg + " = icmp sle i32 " + lLen + ", " + rLen);
            return res(resReg, BuiltinTypes.BOOL.getType());
        }

        TypeRef resultType = promoteNumeric(leftType, rightType);
        if (resultType == null) {
            return new RUnsupportedBinaryExpressionError(leftType.getName(), getSymbol(), rightType.getName(), c.line()).raise();
        }

        String leftReg = convertToType(c.leftReg(), leftType, resultType, c);
        String rightReg = convertToType(c.rightReg(), rightType, resultType, c);
        String resReg = c.cctx().nextRegister();

        if (resultType instanceof FloatBuiltinType || resultType instanceof DoubleBuiltinType) {
            c.cctx().emit(resReg + " = fcmp ole " + resultType.getLLVMName() + " " + leftReg + ", " + rightReg);
        } else {
            c.cctx().emit(resReg + " = icmp sle " + resultType.getLLVMName() + " " + leftReg + ", " + rightReg);
        }

        return res(resReg, BuiltinTypes.BOOL.getType());
    }
}
