package me.kuwg.re.operator.ops.bit;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.expr.RUnsupportedBinaryExpressionError;
import me.kuwg.re.operator.BinaryOperator;
import me.kuwg.re.operator.BinaryOperatorContext;
import me.kuwg.re.operator.result.BOResult;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.ByteBuiltinType;
import me.kuwg.re.type.builtin.IntBuiltinType;
import me.kuwg.re.type.builtin.LongBuiltinType;
import me.kuwg.re.type.builtin.ShortBuiltinType;

public final class BitwiseAndBO extends BinaryOperator {
    public static final BinaryOperator INSTANCE = new BitwiseAndBO();

    BitwiseAndBO() {
        super(6, "&");
    }

    @Override
    public BOResult compile(final BinaryOperatorContext c) {
        TypeRef leftType = c.leftType();
        TypeRef rightType = c.rightType();

        String llvmType;

        if (leftType instanceof ByteBuiltinType && rightType instanceof ByteBuiltinType) {
            llvmType = "i8";
        } else if (leftType instanceof ShortBuiltinType && rightType instanceof ShortBuiltinType) {
            llvmType = "i16";
        } else if (leftType instanceof IntBuiltinType && rightType instanceof IntBuiltinType) {
            llvmType = "i32";
        } else if (leftType instanceof LongBuiltinType && rightType instanceof LongBuiltinType) {
            llvmType = "i64";
        } else {
            return new RUnsupportedBinaryExpressionError(leftType.getName(), getSymbol(), rightType.getName(), c.fileName(), c.line()).raise();
        }

        String leftReg = c.leftReg();
        String rightReg = c.rightReg();

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
        c.cctx().emit(resReg + " = and " + llvmType + " " + leftReg + ", " + rightReg);

        return res(resReg, leftType);
    }

    @Override
    public String compileToConstant(final ValueNode left, final ValueNode right, final CompilationContext cctx) {
        final TypeRef leftType = left.getType();
        final TypeRef rightType = right.getType();

        if (!leftType.equals(rightType)) {
            return unsupported(leftType, rightType, left).raise();
        }

        final String lhs = left.compileToConstant(cctx);
        final String rhs = right.compileToConstant(cctx);

        try {
            if (leftType instanceof ByteBuiltinType) {
                return Byte.toString((byte) (Byte.parseByte(lhs) & Byte.parseByte(rhs)));
            }

            if (leftType instanceof ShortBuiltinType) {
                return Short.toString((short) (Short.parseShort(lhs) & Short.parseShort(rhs)));
            }

            if (leftType instanceof IntBuiltinType) {
                return Integer.toString(Integer.parseInt(lhs) & Integer.parseInt(rhs));
            }

            if (leftType instanceof LongBuiltinType) {
                return Long.toString(Long.parseLong(lhs) & Long.parseLong(rhs));
            }
        } catch (NumberFormatException ex) {
            return unsupported(leftType, rightType, left).raise();
        }

        return unsupported(leftType, rightType, left).raise();
    }
}
