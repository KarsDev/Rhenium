package me.kuwg.re.operator.ops.bit;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.expr.RUnsupportedBinaryExpressionError;
import me.kuwg.re.operator.BinaryOperator;
import me.kuwg.re.operator.BinaryOperatorContext;
import me.kuwg.re.operator.result.BOResult;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;

/// Shared implementation of {@code &}, {@code |} and {@code ^}.
abstract class BitwiseBO extends BinaryOperator {
    private final String llvmOp;

    BitwiseBO(final int precedence, final String symbol, final String llvmOp) {
        super(precedence, symbol);
        this.llvmOp = llvmOp;
    }

    private static boolean isBool(final TypeRef t) {
        return t == BuiltinTypes.BOOL.getType();
    }

    private static boolean isIntegral(final TypeRef t) {
        return t == BuiltinTypes.BYTE.getType()
                || t == BuiltinTypes.SHORT.getType()
                || t == BuiltinTypes.INT.getType()
                || t == BuiltinTypes.LONG.getType();
    }

    private TypeRef resultTypeOf(final TypeRef left, final TypeRef right) {
        if (isBool(left) && isBool(right)) return left;
        if (isIntegral(left) && isIntegral(right)) return promoteNumeric(left, right);
        return null;
    }

    private static long wrap(final long v, final TypeRef t) {
        if (isBool(t)) return v & 1L;
        if (t == BuiltinTypes.BYTE.getType()) return (byte) v;
        if (t == BuiltinTypes.SHORT.getType()) return (short) v;
        if (t == BuiltinTypes.INT.getType()) return (int) v;
        return v;
    }

    private static long parse(final String s, final TypeRef t) {
        if (isBool(t)) {
            return switch (s) {
                case "true", "1" -> 1L;
                case "false", "0" -> 0L;
                default -> throw new NumberFormatException("not a boolean: " + s);
            };
        }

        final long v = Long.parseLong(s);
        if (wrap(v, t) != v) {
            throw new NumberFormatException("out of range for " + t.getName() + ": " + s);
        }
        return v;
    }

    private static String format(final long v, final TypeRef t) {
        if (isBool(t)) return Boolean.toString(v != 0);
        return Long.toString(v);
    }

    protected abstract long apply(long l, long r);

    @Override
    public BOResult compile(final BinaryOperatorContext c) {
        final TypeRef leftType = c.leftType();
        final TypeRef rightType = c.rightType();

        final TypeRef resultType = resultTypeOf(leftType, rightType);
        if (resultType == null) {
            return new RUnsupportedBinaryExpressionError(leftType.getName(), getSymbol(), rightType.getName(), c.fileName(), c.line()).raise();
        }

        final String leftReg = isBool(resultType) ? c.leftReg() : convertToType(c.leftReg(), leftType, resultType, c);
        final String rightReg = isBool(resultType) ? c.rightReg() : convertToType(c.rightReg(), rightType, resultType, c);

        final String resReg = c.cctx().nextRegister();
        c.cctx().emit(resReg + " = " + llvmOp + " " + resultType.getLLVMName() + " " + leftReg + ", " + rightReg);

        return res(resReg, resultType);
    }

    @Override
    public String compileToConstant(final ValueNode left, final ValueNode right, final CompilationContext cctx) {
        final TypeRef leftType = left.getType();
        final TypeRef rightType = right.getType();

        final TypeRef resultType = resultTypeOf(leftType, rightType);

        if (resultType != null) {
            try {
                final long lhs = parse(left.compileToConstant(cctx), leftType);
                final long rhs = parse(right.compileToConstant(cctx), rightType);
                return format(wrap(apply(lhs, rhs), resultType), resultType);
            } catch (NumberFormatException ignored) {
            }
        }

        return new RUnsupportedBinaryExpressionError(leftType.getName(), getSymbol(), rightType.getName(), left.getFileName(), left.getLine()).raise();
    }
}