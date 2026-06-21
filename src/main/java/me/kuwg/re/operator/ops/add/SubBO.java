package me.kuwg.re.operator.ops.add;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.operator.BinaryOperator;
import me.kuwg.re.operator.BinaryOperatorContext;
import me.kuwg.re.operator.result.BOResult;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.*;

public final class SubBO extends BinaryOperator {
    public static final BinaryOperator INSTANCE = new SubBO();

    SubBO() {
        super(8, "-");
    }

    @Override
    public BOResult compile(final BinaryOperatorContext c) {
        TypeRef leftType = c.leftType();
        TypeRef rightType = c.rightType();

        TypeRef resultType = promoteNumeric(leftType, rightType);
        if (resultType == null) {
            return unsupported(leftType, rightType, c.fileName(), c.line()).raise();
        }

        String leftReg = convertToType(c.leftReg(), leftType, resultType, c);
        String rightReg = convertToType(c.rightReg(), rightType, resultType, c);

        String resultReg = c.cctx().nextRegister();
        String op = (isFloat(resultType) ? "fsub" : "sub");

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

        final String lhs = left.compileToConstant(cctx);
        final String rhs = right.compileToConstant(cctx);

        try {
            if (resultType instanceof DoubleBuiltinType) {
                return Double.toString(Double.parseDouble(lhs) - Double.parseDouble(rhs));
            }

            if (resultType instanceof FloatBuiltinType) {
                return Float.toString(Float.parseFloat(lhs) - Float.parseFloat(rhs));
            }

            if (resultType instanceof LongBuiltinType) {
                return Long.toString(Long.parseLong(lhs) - Long.parseLong(rhs));
            }

            if (resultType instanceof IntBuiltinType) {
                return Integer.toString(Integer.parseInt(lhs) - Integer.parseInt(rhs));
            }

            if (resultType instanceof ShortBuiltinType) {
                return Short.toString((short) (Short.parseShort(lhs) - Short.parseShort(rhs)));
            }

            if (resultType instanceof ByteBuiltinType) {
                return Byte.toString((byte) (Byte.parseByte(lhs) - Byte.parseByte(rhs)));
            }
        } catch (NumberFormatException ex) {
            return unsupported(leftType, rightType, left).raise();
        }

        return unsupported(leftType, rightType, left).raise();
    }
}