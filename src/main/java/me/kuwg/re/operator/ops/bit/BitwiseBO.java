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

    private static boolean isSupported(final TypeRef t) {
        return isBool(t)
                || t == BuiltinTypes.BYTE.getType()
                || t == BuiltinTypes.SHORT.getType()
                || t == BuiltinTypes.INT.getType()
                || t == BuiltinTypes.LONG.getType();
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

        if (leftType != rightType || !isSupported(leftType)) {
            return new RUnsupportedBinaryExpressionError(leftType.getName(), getSymbol(), rightType.getName(), c.fileName(), c.line()).raise();
        }

        final String resReg = c.cctx().nextRegister();
        c.cctx().emit(resReg + " = " + llvmOp + " " + leftType.getLLVMName() + " " + c.leftReg() + ", " + c.rightReg());

        return res(resReg, leftType);
    }

    @Override
    public String compileToConstant(final ValueNode left, final ValueNode right, final CompilationContext cctx) {
        final TypeRef leftType = left.getType();
        final TypeRef rightType = right.getType();

        if (leftType.equals(rightType) && isSupported(leftType)) {
            try {
                final long lhs = parse(left.compileToConstant(cctx), leftType);
                final long rhs = parse(right.compileToConstant(cctx), leftType);
                return format(wrap(apply(lhs, rhs), leftType), leftType);
            } catch (NumberFormatException ignored) {
            }
        }

        return new RUnsupportedBinaryExpressionError(leftType.getName(), getSymbol(), rightType.getName(), left.getFileName(), left.getLine()).raise();
    }
}