package me.kuwg.re.operator.ops.mul;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.expr.RUnsupportedBinaryExpressionError;
import me.kuwg.re.operator.BinaryOperator;
import me.kuwg.re.operator.BinaryOperatorContext;
import me.kuwg.re.operator.result.BOResult;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;

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

        if (leftType == BuiltinTypes.BYTE.getType() && rightType == BuiltinTypes.BYTE.getType()) {
            llvmType = "i8";
            resultType = leftType;
        } else if (leftType == BuiltinTypes.SHORT.getType() && rightType == BuiltinTypes.SHORT.getType()) {
            llvmType = "i16";
            resultType = leftType;
        } else if (leftType == BuiltinTypes.INT.getType() && rightType == BuiltinTypes.INT.getType()) {
            llvmType = "i32";
            resultType = leftType;
        } else if (leftType == BuiltinTypes.LONG.getType() && rightType == BuiltinTypes.LONG.getType()) {
            llvmType = "i64";
            resultType = leftType;
        } else if (leftType == BuiltinTypes.LONG.getType() && rightType == BuiltinTypes.INT.getType()) {
            llvmType = "i64";
            resultType = leftType;

            if (!rightReg.matches("\\d+")) {
                String castedReg = c.cctx().nextRegister();
                c.cctx().emit(castedReg + " = sext i32 " + rightReg + " to i64");
                rightReg = castedReg;
            }
        } else if (leftType == BuiltinTypes.INT.getType() && rightType == BuiltinTypes.LONG.getType()) {
            llvmType = "i64";
            resultType = rightType;

            if (!leftReg.matches("\\d+")) {
                String castedReg = c.cctx().nextRegister();
                c.cctx().emit(castedReg + " = sext i32 " + leftReg + " to i64");
                leftReg = castedReg;
            }
        } else if (leftType == BuiltinTypes.FLOAT.getType() && rightType == BuiltinTypes.FLOAT.getType()) {
            llvmType = "float";
            isFloating = true;
            resultType = leftType;
        } else if (leftType == BuiltinTypes.DOUBLE.getType() && rightType == BuiltinTypes.DOUBLE.getType()) {
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
            if (leftType == BuiltinTypes.BYTE.getType() && rightType == BuiltinTypes.BYTE.getType()) {
                final byte l = Byte.parseByte(left.compileToConstant(cctx));
                final byte r = Byte.parseByte(right.compileToConstant(cctx));

                if (r == 0) throw new ArithmeticException("/ by zero");

                return Byte.toString((byte) (l % r));
            }

            if (leftType == BuiltinTypes.SHORT.getType() && rightType == BuiltinTypes.SHORT.getType()) {
                final short l = Short.parseShort(left.compileToConstant(cctx));
                final short r = Short.parseShort(right.compileToConstant(cctx));

                if (r == 0) throw new ArithmeticException("/ by zero");

                return Short.toString((short) (l % r));
            }

            if (leftType == BuiltinTypes.INT.getType() && rightType == BuiltinTypes.INT.getType()) {
                final int l = Integer.parseInt(left.compileToConstant(cctx));
                final int r = Integer.parseInt(right.compileToConstant(cctx));

                if (r == 0) throw new ArithmeticException("/ by zero");

                return Integer.toString(l % r);
            }

            if (leftType == BuiltinTypes.LONG.getType() && rightType == BuiltinTypes.LONG.getType()) {
                final long l = Long.parseLong(left.compileToConstant(cctx));
                final long r = Long.parseLong(right.compileToConstant(cctx));

                if (r == 0L) throw new ArithmeticException("/ by zero");

                return Long.toString(l % r);
            }

            if (leftType == BuiltinTypes.LONG.getType() && rightType == BuiltinTypes.INT.getType()) {
                final long l = Long.parseLong(left.compileToConstant(cctx));
                final int r = Integer.parseInt(right.compileToConstant(cctx));

                if (r == 0) throw new ArithmeticException("/ by zero");

                return Long.toString(l % r);
            }

            if (leftType == BuiltinTypes.INT.getType() && rightType == BuiltinTypes.LONG.getType()) {
                final int l = Integer.parseInt(left.compileToConstant(cctx));
                final long r = Long.parseLong(right.compileToConstant(cctx));

                if (r == 0L) throw new ArithmeticException("/ by zero");

                return Long.toString(l % r);
            }

            if (leftType == BuiltinTypes.FLOAT.getType() && rightType == BuiltinTypes.FLOAT.getType()) {
                final float l = Float.parseFloat(left.compileToConstant(cctx));
                final float r = Float.parseFloat(right.compileToConstant(cctx));

                return Float.toString(l % r);
            }

            if (leftType == BuiltinTypes.DOUBLE.getType() && rightType == BuiltinTypes.DOUBLE.getType()) {
                final double l = Double.parseDouble(left.compileToConstant(cctx));
                final double r = Double.parseDouble(right.compileToConstant(cctx));

                return Double.toString(l % r);
            }
        } catch (NumberFormatException ignored) {
        }

        return unsupported(leftType, rightType, left).raise();
    }
}
