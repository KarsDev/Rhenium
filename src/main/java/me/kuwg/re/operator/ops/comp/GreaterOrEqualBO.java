package me.kuwg.re.operator.ops.comp;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.expr.RUnsupportedBinaryExpressionError;
import me.kuwg.re.operator.BinaryOperator;
import me.kuwg.re.operator.BinaryOperatorContext;
import me.kuwg.re.operator.result.BOResult;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.*;

public final class GreaterOrEqualBO extends BinaryOperator {
    public static final BinaryOperator INSTANCE = new GreaterOrEqualBO();

    GreaterOrEqualBO() {
        super(3, ">=");
    }

    @Override
    public BOResult compile(final BinaryOperatorContext c) {
        TypeRef leftType = c.leftType();
        TypeRef rightType = c.rightType();

        if (leftType instanceof StrBuiltinType && rightType instanceof StrBuiltinType) {
            c.cctx().include(-1, null, "string", null);
            String lLen = c.cctx().nextRegister();
            String rLen = c.cctx().nextRegister();

            c.cctx().emit(lLen + " = call i32 @strlen(i8* " + c.leftReg() + ")");
            c.cctx().emit(rLen + " = call i32 @strlen(i8* " + c.rightReg() + ")");

            String resReg = c.cctx().nextRegister();
            c.cctx().emit(resReg + " = icmp sge i32 " + lLen + ", " + rLen);
            return res(resReg, BuiltinTypes.BOOL.getType());
        }

        TypeRef resultType = promoteNumeric(leftType, rightType);
        if (resultType == null) {
            return new RUnsupportedBinaryExpressionError(leftType.getName(), getSymbol(), rightType.getName(), c.fileName(), c.line()).raise();
        }

        String leftReg = convertToType(c.leftReg(), leftType, resultType, c);
        String rightReg = convertToType(c.rightReg(), rightType, resultType, c);
        String resReg = c.cctx().nextRegister();

        if (resultType instanceof FloatBuiltinType || resultType instanceof DoubleBuiltinType) {
            c.cctx().emit(resReg + " = fcmp oge " + resultType.getLLVMName() + " " + leftReg + ", " + rightReg);
        } else {
            c.cctx().emit(resReg + " = icmp sge " + resultType.getLLVMName() + " " + leftReg + ", " + rightReg);
        }

        return res(resReg, BuiltinTypes.BOOL.getType());
    }

    @Override
    public String compileToConstant(final ValueNode left, final ValueNode right, final CompilationContext cctx) {
        final TypeRef leftType = left.getType();
        final TypeRef rightType = right.getType();

        if (leftType instanceof StrBuiltinType && rightType instanceof StrBuiltinType) {
            return Boolean.toString(
                    left.compileToConstant(cctx).length() >=
                            right.compileToConstant(cctx).length()
            );
        }

        final TypeRef resultType = promoteNumeric(leftType, rightType);

        if (resultType == null) {
            return unsupported(leftType, rightType, left).raise();
        }

        try {
            if (resultType instanceof DoubleBuiltinType) {
                return Boolean.toString(
                        Double.parseDouble(left.compileToConstant(cctx)) >=
                                Double.parseDouble(right.compileToConstant(cctx))
                );
            }

            if (resultType instanceof FloatBuiltinType) {
                return Boolean.toString(
                        Float.parseFloat(left.compileToConstant(cctx)) >=
                                Float.parseFloat(right.compileToConstant(cctx))
                );
            }

            if (resultType instanceof LongBuiltinType) {
                return Boolean.toString(
                        Long.parseLong(left.compileToConstant(cctx)) >=
                                Long.parseLong(right.compileToConstant(cctx))
                );
            }

            if (resultType instanceof IntBuiltinType) {
                return Boolean.toString(
                        Integer.parseInt(left.compileToConstant(cctx)) >=
                                Integer.parseInt(right.compileToConstant(cctx))
                );
            }

            if (resultType instanceof ShortBuiltinType) {
                return Boolean.toString(
                        Short.parseShort(left.compileToConstant(cctx)) >=
                                Short.parseShort(right.compileToConstant(cctx))
                );
            }

            if (resultType instanceof ByteBuiltinType) {
                return Boolean.toString(
                        Byte.parseByte(left.compileToConstant(cctx)) >=
                                Byte.parseByte(right.compileToConstant(cctx))
                );
            }
        } catch (NumberFormatException ignored) {
        }

        return unsupported(leftType, rightType, left).raise();
    }
}
