package me.kuwg.re.operator.ops.bit;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.expr.RUnsupportedBinaryExpressionError;
import me.kuwg.re.operator.BinaryOperator;
import me.kuwg.re.operator.BinaryOperatorContext;
import me.kuwg.re.operator.result.BOResult;
import me.kuwg.re.type.TypeRef;

/// Shared implementation of {@code <<}, {@code >>} and {@code >>>}.
abstract class ShiftBO extends BinaryOperator {
    private final String llvmOp;

    ShiftBO(final String symbol, final String llvmOp) {
        super(7, symbol);
        this.llvmOp = llvmOp;
    }

    protected abstract long fold(long value, int shift, int width);

    protected static long unsigned(final long v, final int width) {
        return width >= 64 ? v : v & ((1L << width) - 1);
    }

    private static long wrap(final long v, final int width) {
        return switch (width) {
            case 8 -> (byte) v;
            case 16 -> (short) v;
            case 32 -> (int) v;
            default -> v;
        };
    }

    private static int bitWidth(final TypeRef t) {
        final String n = t.getLLVMName();
        if (n.length() < 2 || n.charAt(0) != 'i') return 0;
        try {
            return Integer.parseInt(n.substring(1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public BOResult compile(final BinaryOperatorContext c) {
        final TypeRef leftType = c.leftType();
        final TypeRef rightType = c.rightType();

        if (!isInteger(leftType) || !isInteger(rightType)) {
            return new RUnsupportedBinaryExpressionError(
                    c.leftType().getName(), getSymbol(), c.rightType().getName(), c.fileName(), c.line()
            ).raise();
        }

        final TypeRef resultType = promoteNumeric(leftType, rightType);
        final int width = resultType == null ? 0 : bitWidth(resultType);
        if (width == 0) {
            return new RUnsupportedBinaryExpressionError(
                    c.leftType().getName(), getSymbol(), c.rightType().getName(), c.fileName(), c.line()
            ).raise();
        }
        final String llvmType = resultType.getLLVMName();

        final String leftReg = convertToType(c.leftReg(), leftType, resultType, c);
        final String amountReg = convertToType(c.rightReg(), rightType, resultType, c);

        final String maskedReg = c.cctx().nextRegister();
        c.cctx().emit(maskedReg + " = and " + llvmType + " " + amountReg + ", " + (width - 1));

        final String resReg = c.cctx().nextRegister();
        c.cctx().emit(resReg + " = " + llvmOp + " " + llvmType + " " + leftReg + ", " + maskedReg);

        return res(resReg, resultType);
    }

    @Override
    public String compileToConstant(final ValueNode left, final ValueNode right, final CompilationContext cctx) {
        final TypeRef leftType = left.getType();
        final TypeRef rightType = right.getType();

        if (!isInteger(leftType) || !isInteger(rightType)) {
            return new RUnsupportedBinaryExpressionError(leftType.getName(), getSymbol(), rightType.getName(), left.getFileName(), left.getLine()).raise();
        }

        final TypeRef resultType = promoteNumeric(leftType, rightType);
        final int width = resultType == null ? 0 : bitWidth(resultType);

        if (width == 8 || width == 16 || width == 32 || width == 64) {
            try {
                final long lhs = wrap(Long.parseLong(left.compileToConstant(cctx)), width);
                final long rhs = Long.parseLong(right.compileToConstant(cctx));
                final int shift = (int) (rhs & (width - 1));

                return Long.toString(wrap(fold(lhs, shift, width), width));
            } catch (NumberFormatException ignored) {
            }
        }

        return new RUnsupportedBinaryExpressionError(leftType.getName(), getSymbol(), rightType.getName(), left.getFileName(), left.getLine()).raise();
    }
}
