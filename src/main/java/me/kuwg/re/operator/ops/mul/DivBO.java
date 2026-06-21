package me.kuwg.re.operator.ops.mul;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.constant.RDivisionByZeroError;
import me.kuwg.re.error.errors.expr.RUnsupportedBinaryExpressionError;
import me.kuwg.re.operator.BinaryOperator;
import me.kuwg.re.operator.BinaryOperatorContext;
import me.kuwg.re.operator.result.BOResult;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.*;

public final class DivBO extends BinaryOperator {
    public static final BinaryOperator INSTANCE = new DivBO();

    DivBO() {
        super(9, "/");
    }

    @Override
    public BOResult compile(final BinaryOperatorContext c) {
        TypeRef leftType = c.leftType();
        TypeRef rightType = c.rightType();

        TypeRef resultType = promoteNumeric(leftType, rightType);
        if (resultType == null) {
            return new RUnsupportedBinaryExpressionError(
                    leftType.getName(), getSymbol(), rightType.getName(), c.fileName(), c.line()
            ).raise();
        }

        String leftReg = convertToType(c.leftReg(), leftType, resultType, c);
        String rightReg = convertToType(c.rightReg(), rightType, resultType, c);

        String resultReg = c.cctx().nextRegister();
        String op = (isFloat(resultType) ? "fdiv" : "sdiv");

        c.cctx().emit(resultReg + " = " + op + " " + resultType.getLLVMName() + " " + leftReg + ", " + rightReg);

        return res(resultReg, resultType);
    }

    @Override
    public String compileToConstant(final ValueNode left, final ValueNode right, final CompilationContext cctx) {
        final TypeRef leftType = left.getType();
        final TypeRef rightType = right.getType();

        final TypeRef resultType = promoteNumeric(leftType, rightType);

        if (resultType == null) {
            return unsupported(leftType, rightType, left).raise();
        }

        try {
            if (resultType instanceof DoubleBuiltinType) {
                final double l = Double.parseDouble(left.compileToConstant(cctx));
                final double r = Double.parseDouble(right.compileToConstant(cctx));

                if (r == 0.0d) {
                    return new RDivisionByZeroError(left.getFileName(), left.getLine()).raise();
                }

                return Double.toString(l / r);
            }

            if (resultType instanceof FloatBuiltinType) {
                final float l = Float.parseFloat(left.compileToConstant(cctx));
                final float r = Float.parseFloat(right.compileToConstant(cctx));

                if (r == 0.0f) {
                    return new RDivisionByZeroError(left.getFileName(), left.getLine()).raise();
                }

                return Float.toString(l / r);
            }

            if (resultType instanceof LongBuiltinType) {
                final long l = Long.parseLong(left.compileToConstant(cctx));
                final long r = Long.parseLong(right.compileToConstant(cctx));

                if (r == 0L) {
                    return new RDivisionByZeroError(left.getFileName(), left.getLine()).raise();
                }

                return Long.toString(l / r);
            }

            if (resultType instanceof IntBuiltinType) {
                final int l = Integer.parseInt(left.compileToConstant(cctx));
                final int r = Integer.parseInt(right.compileToConstant(cctx));

                if (r == 0) {
                    return new RDivisionByZeroError(left.getFileName(), left.getLine()).raise();
                }

                return Integer.toString(l / r);
            }

            if (resultType instanceof ShortBuiltinType) {
                final short l = Short.parseShort(left.compileToConstant(cctx));
                final short r = Short.parseShort(right.compileToConstant(cctx));

                if (r == 0) {
                    return new RDivisionByZeroError(left.getFileName(), left.getLine()).raise();
                }

                return Short.toString((short) (l / r));
            }

            if (resultType instanceof ByteBuiltinType) {
                final byte l = Byte.parseByte(left.compileToConstant(cctx));
                final byte r = Byte.parseByte(right.compileToConstant(cctx));

                if (r == 0) {
                    return new RDivisionByZeroError(left.getFileName(), left.getLine()).raise();
                }

                return Byte.toString((byte) (l / r));
            }
        } catch (NumberFormatException ignored) {
        }

        return unsupported(leftType, rightType, left).raise();
    }
}
