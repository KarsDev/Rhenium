package me.kuwg.re.operator.ops.mul;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.expr.RUnsupportedBinaryExpressionError;
import me.kuwg.re.operator.BinaryOperator;
import me.kuwg.re.operator.BinaryOperatorContext;
import me.kuwg.re.operator.result.BOResult;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.*;

public final class MulBO extends BinaryOperator {
    public static final BinaryOperator INSTANCE = new MulBO();

    MulBO() {
        super(9, "*");
    }

    @Override
    public BOResult compile(final BinaryOperatorContext c) {
        TypeRef leftType = c.leftType();
        TypeRef rightType = c.rightType();

        TypeRef resultType = promoteNumeric(leftType, rightType);
        if (resultType == null) {
            return new RUnsupportedBinaryExpressionError(leftType.getName(), getSymbol(), rightType.getName(), c.fileName(), c.line()).raise();
        }

        String leftReg = convertToType(c.leftReg(), leftType, resultType, c);
        String rightReg = convertToType(c.rightReg(), rightType, resultType, c);

        String resultReg = c.cctx().nextRegister();

        if (isFloat(resultType)) {
            c.cctx().emit(resultReg + " = fmul " + resultType.getLLVMName() + " " + leftReg + ", " + rightReg);
        } else {
            c.cctx().emit(resultReg + " = mul " + resultType.getLLVMName() + " " + leftReg + ", " + rightReg);
        }

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
                return Double.toString(
                        Double.parseDouble(left.compileToConstant(cctx)) *
                                Double.parseDouble(right.compileToConstant(cctx))
                );
            }

            if (resultType instanceof FloatBuiltinType) {
                return Float.toString(
                        Float.parseFloat(left.compileToConstant(cctx)) *
                                Float.parseFloat(right.compileToConstant(cctx))
                );
            }

            if (resultType instanceof LongBuiltinType) {
                return Long.toString(
                        Long.parseLong(left.compileToConstant(cctx)) *
                                Long.parseLong(right.compileToConstant(cctx))
                );
            }

            if (resultType instanceof IntBuiltinType) {
                return Integer.toString(
                        Integer.parseInt(left.compileToConstant(cctx)) *
                                Integer.parseInt(right.compileToConstant(cctx))
                );
            }

            if (resultType instanceof ShortBuiltinType) {
                return Short.toString(
                        (short) (
                                Short.parseShort(left.compileToConstant(cctx)) *
                                        Short.parseShort(right.compileToConstant(cctx))
                        )
                );
            }

            if (resultType instanceof ByteBuiltinType) {
                return Byte.toString(
                        (byte) (
                                Byte.parseByte(left.compileToConstant(cctx)) *
                                        Byte.parseByte(right.compileToConstant(cctx))
                        )
                );
            }
        } catch (NumberFormatException ignored) {
        }

        return unsupported(leftType, rightType, left).raise();
    }
}
