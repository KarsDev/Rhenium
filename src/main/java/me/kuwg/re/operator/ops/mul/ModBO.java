package me.kuwg.re.operator.ops.mul;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.expr.RUnsupportedBinaryExpressionError;
import me.kuwg.re.operator.BinaryOperator;
import me.kuwg.re.operator.BinaryOperatorContext;
import me.kuwg.re.operator.result.BOResult;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.*;

public final class ModBO extends BinaryOperator {
    public static final BinaryOperator INSTANCE = new ModBO();

    ModBO() {
        super(9, "%");
    }

    @Override
    public BOResult compile(final BinaryOperatorContext c) {
        var leftType = c.leftType();
        var rightType = c.rightType();

        String llvmType;
        boolean isFloating = false;

        TypeRef resultType;

        String leftReg = c.leftReg();
        String rightReg = c.rightReg();

        if (leftType instanceof ByteBuiltinType && rightType instanceof ByteBuiltinType) {
            llvmType = "i8";
            resultType = leftType;
        } else if (leftType instanceof ShortBuiltinType && rightType instanceof ShortBuiltinType) {
            llvmType = "i16";
            resultType = leftType;
        } else if (leftType instanceof IntBuiltinType && rightType instanceof IntBuiltinType) {
            llvmType = "i32";
            resultType = leftType;
        } else if (leftType instanceof LongBuiltinType && rightType instanceof LongBuiltinType) {
            llvmType = "i64";
            resultType = leftType;
        } else if (leftType instanceof LongBuiltinType && rightType instanceof IntBuiltinType) {
            llvmType = "i64";
            resultType = leftType;

            if (!rightReg.matches("\\d+")) {
                String castedReg = c.cctx().nextRegister();
                c.cctx().emit(castedReg + " = sext i32 " + rightReg + " to i64");
                rightReg = castedReg;
            }
        } else if (leftType instanceof IntBuiltinType && rightType instanceof LongBuiltinType) {
            llvmType = "i64";
            resultType = rightType;

            if (!leftReg.matches("\\d+")) {
                String castedReg = c.cctx().nextRegister();
                c.cctx().emit(castedReg + " = sext i32 " + leftReg + " to i64");
                leftReg = castedReg;
            }
        } else if (leftType instanceof FloatBuiltinType && rightType instanceof FloatBuiltinType) {
            llvmType = "float";
            isFloating = true;
            resultType = leftType;
        } else if (leftType instanceof DoubleBuiltinType && rightType instanceof DoubleBuiltinType) {
            llvmType = "double";
            isFloating = true;
            resultType = leftType;
        } else {
            return new RUnsupportedBinaryExpressionError(
                    leftType.getName(), getSymbol(), rightType.getName(), c.fileName(), c.line()
            ).raise();
        }

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

        return res(resReg, resultType);
    }

    @Override
    public String compileToConstant(final ValueNode left, final ValueNode right, final CompilationContext cctx) {
        final TypeRef leftType = left.getType();
        final TypeRef rightType = right.getType();

        try {
            if (leftType instanceof ByteBuiltinType && rightType instanceof ByteBuiltinType) {
                final byte l = Byte.parseByte(left.compileToConstant(cctx));
                final byte r = Byte.parseByte(right.compileToConstant(cctx));

                if (r == 0) throw new ArithmeticException("/ by zero");

                return Byte.toString((byte) (l % r));
            }

            if (leftType instanceof ShortBuiltinType && rightType instanceof ShortBuiltinType) {
                final short l = Short.parseShort(left.compileToConstant(cctx));
                final short r = Short.parseShort(right.compileToConstant(cctx));

                if (r == 0) throw new ArithmeticException("/ by zero");

                return Short.toString((short) (l % r));
            }

            if (leftType instanceof IntBuiltinType && rightType instanceof IntBuiltinType) {
                final int l = Integer.parseInt(left.compileToConstant(cctx));
                final int r = Integer.parseInt(right.compileToConstant(cctx));

                if (r == 0) throw new ArithmeticException("/ by zero");

                return Integer.toString(l % r);
            }

            if (leftType instanceof LongBuiltinType && rightType instanceof LongBuiltinType) {
                final long l = Long.parseLong(left.compileToConstant(cctx));
                final long r = Long.parseLong(right.compileToConstant(cctx));

                if (r == 0L) throw new ArithmeticException("/ by zero");

                return Long.toString(l % r);
            }

            if (leftType instanceof LongBuiltinType && rightType instanceof IntBuiltinType) {
                final long l = Long.parseLong(left.compileToConstant(cctx));
                final int r = Integer.parseInt(right.compileToConstant(cctx));

                if (r == 0) throw new ArithmeticException("/ by zero");

                return Long.toString(l % r);
            }

            if (leftType instanceof IntBuiltinType && rightType instanceof LongBuiltinType) {
                final int l = Integer.parseInt(left.compileToConstant(cctx));
                final long r = Long.parseLong(right.compileToConstant(cctx));

                if (r == 0L) throw new ArithmeticException("/ by zero");

                return Long.toString(l % r);
            }

            if (leftType instanceof FloatBuiltinType && rightType instanceof FloatBuiltinType) {
                final float l = Float.parseFloat(left.compileToConstant(cctx));
                final float r = Float.parseFloat(right.compileToConstant(cctx));

                return Float.toString(l % r);
            }

            if (leftType instanceof DoubleBuiltinType && rightType instanceof DoubleBuiltinType) {
                final double l = Double.parseDouble(left.compileToConstant(cctx));
                final double r = Double.parseDouble(right.compileToConstant(cctx));

                return Double.toString(l % r);
            }
        } catch (NumberFormatException ignored) {
        }

        return unsupported(leftType, rightType, left).raise();
    }
}
