package me.kuwg.re.operator.ops.add;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.cast.RStringConversionError;
import me.kuwg.re.error.errors.expr.RUnsupportedBinaryExpressionError;
import me.kuwg.re.operator.BinaryOperator;
import me.kuwg.re.operator.BinaryOperatorContext;
import me.kuwg.re.operator.result.BOResult;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;

public final class AddBO extends BinaryOperator {
    public static final BinaryOperator INSTANCE = new AddBO();

    AddBO() {
        super(8, "+");
    }

    @Override
    public BOResult compile(final BinaryOperatorContext c) {
        TypeRef leftType = c.leftType();
        TypeRef rightType = c.rightType();

        if (leftType == BuiltinTypes.STR.getType() || rightType == BuiltinTypes.STR.getType()) {
            String callExpr = compileStringOperation(c);
            String resultReg = c.cctx().nextRegister();
            c.cctx().emit(resultReg + " = " + callExpr);
            return res(resultReg, BuiltinTypes.STR.getType());
        }

        TypeRef resultType = promoteNumeric(leftType, rightType);
        if (resultType == null) {
            return new RUnsupportedBinaryExpressionError(leftType.getName(), getSymbol(), rightType.getName(), c.fileName(), c.line()).raise();
        }

        String leftReg = convertToType(c.leftReg(), leftType, resultType, c);
        String rightReg = convertToType(c.rightReg(), rightType, resultType, c);

        String resultReg = c.cctx().nextRegister();
        String op = (isFloat(resultType) ? "fadd" : "add");

        c.cctx().emit(resultReg + " = " + op + " " + resultType.getLLVMName() + " " + leftReg + ", " + rightReg);
        return res(resultReg, resultType);
    }

    @Override
    public String compileToConstant(final ValueNode left, final ValueNode right, final CompilationContext cctx) {
        final TypeRef leftType = left.getType();
        final TypeRef rightType = right.getType();

        if (leftType == BuiltinTypes.STR.getType() || rightType == BuiltinTypes.STR.getType()) {
            return unsupported(leftType, rightType, left).raise();
        }

        final TypeRef resultType = promoteNumeric(leftType, rightType);

        if (resultType == null) {
            return unsupported(leftType, rightType, left).raise();
        }

        final String lhs = left.compileToConstant(cctx);
        final String rhs = right.compileToConstant(cctx);

        try {
            if (resultType == BuiltinTypes.DOUBLE.getType()) {
                return Double.toString(Double.parseDouble(lhs) + Double.parseDouble(rhs));
            }

            if (resultType == BuiltinTypes.FLOAT.getType()) {
                return Float.toString(Float.parseFloat(lhs) + Float.parseFloat(rhs));
            }

            if (resultType == BuiltinTypes.LONG.getType()) {
                return Long.toString(Long.parseLong(lhs) + Long.parseLong(rhs));
            }

            if (resultType == BuiltinTypes.INT.getType()) {
                return Integer.toString(Integer.parseInt(lhs) + Integer.parseInt(rhs));
            }

            if (resultType == BuiltinTypes.SHORT.getType()) {
                return Short.toString((short) (Short.parseShort(lhs) + Short.parseShort(rhs)));
            }

            if (resultType == BuiltinTypes.BYTE.getType()) {
                return Byte.toString((byte) (Byte.parseByte(lhs) + Byte.parseByte(rhs)));
            }
        } catch (NumberFormatException ex) {
            return unsupported(leftType, rightType, left).raise();
        }

        return unsupported(leftType, rightType, left).raise();
    }

    private String compileStringOperation(BinaryOperatorContext c) {

        String left = c.leftReg();
        String right = c.rightReg();

        if (!(c.leftType() == BuiltinTypes.STR.getType())) {
            left = c.cctx().nextRegister();
            c.cctx().emit(left + " = " + convertToString(c.fileName(), c.line(), c.leftReg(), c.leftType()));
        }

        if (!(c.rightType() == BuiltinTypes.STR.getType())) {
            right = c.cctx().nextRegister();
            c.cctx().emit(right + " = " + convertToString(c.fileName(), c.line(), c.rightReg(), c.rightType()));
        }

        return "call i8* @strConcat(i8* " + left + ", i8* " + right + ")";
    }

    private String convertToString(String fileName, int line, String reg, TypeRef type) {
        if (type == BuiltinTypes.BYTE.getType()) return "call i8* @byteToStr(i8 " + reg + ")";
        if (type == BuiltinTypes.SHORT.getType()) return "call i8* @shortToStr(i16 " + reg + ")";
        if (type == BuiltinTypes.INT.getType()) return "call i8* @intToStr(i32 " + reg + ")";
        if (type == BuiltinTypes.LONG.getType()) return "call i8* @longToStr(i64 " + reg + ")";
        if (type == BuiltinTypes.FLOAT.getType()) return "call i8* @floatToStr(float " + reg + ")";
        if (type == BuiltinTypes.DOUBLE.getType()) return "call i8* @doubleToStr(double " + reg + ")";
        if (type == BuiltinTypes.BOOL.getType()) return "call i8* @boolToStr(i1 " + reg + ")";
        if (type == BuiltinTypes.CHAR.getType()) return "call i8* @charToStrAscii(i8 " + reg + ")";
        if (type == BuiltinTypes.ANYPTR.getType()) return "call i8* @ptrToStr(i8* " + reg + ")";

        return new RStringConversionError(type.getName(), fileName, line).raise();
    }
}