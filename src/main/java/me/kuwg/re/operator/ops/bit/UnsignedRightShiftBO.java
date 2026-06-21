package me.kuwg.re.operator.ops.bit;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.expr.RUnsupportedBinaryExpressionError;
import me.kuwg.re.operator.BinaryOperator;
import me.kuwg.re.operator.BinaryOperatorContext;
import me.kuwg.re.operator.result.BOResult;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.ByteBuiltinType;
import me.kuwg.re.type.builtin.IntBuiltinType;
import me.kuwg.re.type.builtin.LongBuiltinType;
import me.kuwg.re.type.builtin.ShortBuiltinType;

public final class UnsignedRightShiftBO extends BinaryOperator {
    public static final BinaryOperator INSTANCE = new UnsignedRightShiftBO();

    UnsignedRightShiftBO() {
        super(7, ">>>");
    }

    @Override
    public BOResult compile(final BinaryOperatorContext c) {
        TypeRef leftType = c.leftType();
        TypeRef rightType = c.rightType();

        if (!isInteger(leftType) || !isInteger(rightType)) {
            return new RUnsupportedBinaryExpressionError(
                    leftType.getName(), getSymbol(), rightType.getName(), c.fileName(), c.line()
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
        c.cctx().emit(resReg + " = lshr " + llvmType + " " + leftReg + ", " + shiftReg);

        return res(resReg, resultType);
    }

    @Override
    public String compileToConstant(final ValueNode left, final ValueNode right, final CompilationContext cctx) {
        final TypeRef leftType = left.getType();
        final TypeRef rightType = right.getType();

        if (!isInteger(leftType) || !isInteger(rightType)) {
            return unsupported(leftType, rightType, left).raise();
        }

        final TypeRef resultType = promoteNumeric(leftType, rightType);

        try {
            final int rhs = Integer.parseInt(right.compileToConstant(cctx));

            if (resultType instanceof ByteBuiltinType) {
                final byte lhs = Byte.parseByte(left.compileToConstant(cctx));
                return Byte.toString((byte) (lhs >>> rhs));
            }

            if (resultType instanceof ShortBuiltinType) {
                final short lhs = Short.parseShort(left.compileToConstant(cctx));
                return Short.toString((short) (lhs >>> rhs));
            }

            if (resultType instanceof IntBuiltinType) {
                final int lhs = Integer.parseInt(left.compileToConstant(cctx));
                return Integer.toString(lhs >>> rhs);
            }

            if (resultType instanceof LongBuiltinType) {
                final long lhs = Long.parseLong(left.compileToConstant(cctx));
                return Long.toString(lhs >>> rhs);
            }
        } catch (NumberFormatException ignored) {
        }

        return unsupported(leftType, rightType, left).raise();
    }
}
