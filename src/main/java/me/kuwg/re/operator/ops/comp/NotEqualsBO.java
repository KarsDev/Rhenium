package me.kuwg.re.operator.ops.comp;

import me.kuwg.re.error.errors.expr.RUnsupportedBinaryExpressionError;
import me.kuwg.re.operator.BinaryOperator;
import me.kuwg.re.operator.BinaryOperatorContext;
import me.kuwg.re.operator.result.BOResult;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.*;
import me.kuwg.re.type.ptr.NullType;

public class NotEqualsBO extends BinaryOperator {
    public static final BinaryOperator INSTANCE = new NotEqualsBO();

    NotEqualsBO() {
        super(8, "!=");
    }

    @Override
    public BOResult compile(final BinaryOperatorContext c) {
        TypeRef leftType = c.leftType();
        TypeRef rightType = c.rightType();

        if (leftType instanceof StrBuiltinType && rightType instanceof StrBuiltinType) {
            c.cctx().include(-1, null, "string", null);

            String resReg = c.cctx().nextRegister();
            c.cctx().emit(resReg + " = call i1 @strNotEquals(i8* " + c.leftReg() + ", i8* " + c.rightReg() + ")");
            return res(resReg, BuiltinTypes.BOOL.getType());
        }

        if (leftType instanceof AnyPointerType && rightType instanceof AnyPointerType) {
            String resReg = c.cctx().nextRegister();
            c.cctx().emit(
                    resReg + " = icmp ne ptr " + c.leftReg() + ", " + c.rightReg()
            );
            return res(resReg, BuiltinTypes.BOOL.getType());
        }

        if (leftType instanceof AnyPointerType && rightType instanceof NullType) {
            String resReg = c.cctx().nextRegister();
            c.cctx().emit(
                    resReg + " = icmp ne ptr " + c.leftReg() + ", null"
            );
            return res(resReg, BuiltinTypes.BOOL.getType());
        }

        if (leftType instanceof NullType && rightType instanceof AnyPointerType) {
            String resReg = c.cctx().nextRegister();
            c.cctx().emit(
                    resReg + " = icmp ne ptr null, " + c.rightReg()
            );
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
            c.cctx().emit(resReg + " = fcmp une " + resultType.getLLVMName() + " " + leftReg + ", " + rightReg);
        } else {
            c.cctx().emit(resReg + " = icmp ne " + resultType.getLLVMName() + " " + leftReg + ", " + rightReg);
        }

        return res(resReg, BuiltinTypes.BOOL.getType());
    }
}
