package me.kuwg.re.operator.ops.mul;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.constant.RDivisionByZeroError;
import me.kuwg.re.error.errors.expr.RUnsupportedBinaryExpressionError;
import me.kuwg.re.operator.BinaryOperator;
import me.kuwg.re.operator.BinaryOperatorContext;
import me.kuwg.re.operator.result.BOResult;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;

/// Shared implementation of {@code *}, {@code /} and {@code %}.
abstract class ArithmeticBO extends BinaryOperator {

    private final String intOp;
    private final String floatOp;
    private final ZeroDivisor zero;

    ArithmeticBO(final String symbol, final String intOp, final String floatOp, final ZeroDivisor zero) {
        super(9, symbol);
        this.intOp = intOp;
        this.floatOp = floatOp;
        this.zero = zero;
    }

    private static boolean isFixedInt(final TypeRef t) {
        return t == BuiltinTypes.BYTE.getType() || t == BuiltinTypes.SHORT.getType() || t == BuiltinTypes.INT.getType() || t == BuiltinTypes.LONG.getType();
    }

    private static long wrap(final long v, final TypeRef t) {
        if (t == BuiltinTypes.BYTE.getType()) return (byte) v;
        if (t == BuiltinTypes.SHORT.getType()) return (short) v;
        if (t == BuiltinTypes.INT.getType()) return (int) v;
        return v;
    }

    private static long parseInt(final String s, final TypeRef t) {
        final long v = Long.parseLong(s);
        if (wrap(v, t) != v) {
            throw new NumberFormatException("out of range for " + t.getName() + ": " + s);
        }
        return v;
    }

    protected abstract long applyLong(long l, long r);

    protected abstract float applyFloat(float l, float r);

    protected abstract double applyDouble(double l, double r);

    private String divisionByZero(final ValueNode at) {
        return new RDivisionByZeroError(at.getFileName(), at.getLine()).raise();
    }

    @Override
    public BOResult compile(final BinaryOperatorContext c) {
        final TypeRef leftType = c.leftType();
        final TypeRef rightType = c.rightType();

        final TypeRef resultType = (isNumeric(leftType) && isNumeric(rightType))
                ? promoteNumeric(leftType, rightType)
                : null;

        if (resultType == null) {
            return new RUnsupportedBinaryExpressionError(leftType.getName(), getSymbol(), rightType.getName(), c.fileName(), c.line()).raise();
        }

        final String leftReg = convertToType(c.leftReg(), leftType, resultType, c);
        final String rightReg = convertToType(c.rightReg(), rightType, resultType, c);

        final String resultReg = c.cctx().nextRegister();
        final String op = isFloat(resultType) ? floatOp : intOp;
        c.cctx().emit(resultReg + " = " + op + " " + resultType.getLLVMName() + " " + leftReg + ", " + rightReg);

        return res(resultReg, resultType);
    }

    @Override
    public String compileToConstant(final ValueNode left, final ValueNode right, final CompilationContext cctx) {
        final TypeRef leftType = left.getType();
        final TypeRef rightType = right.getType();

        final TypeRef resultType = (isNumeric(leftType) && isNumeric(rightType))
                ? promoteNumeric(leftType, rightType)
                : null;

        if (resultType != null) {
            try {
                final String ls = left.compileToConstant(cctx);
                final String rs = right.compileToConstant(cctx);

                if (resultType == BuiltinTypes.DOUBLE.getType()) {
                    final double l = Double.parseDouble(ls);
                    final double r = Double.parseDouble(rs);
                    if (zero == ZeroDivisor.REJECT_ALL && r == 0.0d) return divisionByZero(left);
                    return Double.toString(applyDouble(l, r));
                }

                if (resultType == BuiltinTypes.FLOAT.getType()) {
                    final float l = Float.parseFloat(ls);
                    final float r = Float.parseFloat(rs);
                    if (zero == ZeroDivisor.REJECT_ALL && r == 0.0f) return divisionByZero(left);
                    return Float.toString(applyFloat(l, r));
                }

                if (isFixedInt(resultType)) {
                    final long l = parseInt(ls, resultType);
                    final long r = parseInt(rs, resultType);
                    if (zero != ZeroDivisor.ALLOWED && r == 0L) return divisionByZero(left);
                    return Long.toString(wrap(applyLong(l, r), resultType));
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return new RUnsupportedBinaryExpressionError(leftType.getName(), getSymbol(), rightType.getName(), left.getFileName(), left.getLine()).raise();
    }

    /// What constant folding does when the right operand is zero
    enum ZeroDivisor {
        /// Fold normally (multiplication)
        ALLOWED,
        /// Raise a division-by-zero error for integer types only (float mod 0 folds to NaN)
        REJECT_INTEGER,
        /// Raise a division-by-zero error for every numeric type
        REJECT_ALL
    }
}