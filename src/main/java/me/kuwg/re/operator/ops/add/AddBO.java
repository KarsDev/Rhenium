package me.kuwg.re.operator.ops.add;

import me.kuwg.re.error.errors.cast.RStringConversionError;
import me.kuwg.re.error.errors.expr.RUnsupportedBinaryExpressionError;
import me.kuwg.re.operator.BinaryOperator;
import me.kuwg.re.operator.BinaryOperatorContext;
import me.kuwg.re.operator.result.BOResult;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.*;

public final class AddBO extends BinaryOperator {
    public static final BinaryOperator INSTANCE = new AddBO();

    AddBO() {
        super(11, "+");
    }

    @Override
    public BOResult compile(final BinaryOperatorContext c) {
        TypeRef leftType = c.leftType();
        TypeRef rightType = c.rightType();

        if (leftType instanceof StrBuiltinType || rightType instanceof StrBuiltinType) {
            String callExpr = compileStringOperation(c);
            String resultReg = c.cctx().nextRegister();
            c.cctx().emit(resultReg + " = " + callExpr);
            return res(resultReg, BuiltinTypes.STR.getType());
        }

        TypeRef resultType = promoteNumeric(leftType, rightType);
        if (resultType == null) {
            return new RUnsupportedBinaryExpressionError(leftType.getName(), getSymbol(), rightType.getName(), c.line()).raise();
        }

        String leftReg = convertToType(c.leftReg(), leftType, resultType, c);
        String rightReg = convertToType(c.rightReg(), rightType, resultType, c);

        String resultReg = c.cctx().nextRegister();
        String op = (isFloat(resultType) ? "fadd" : "add");

        c.cctx().emit(resultReg + " = " + op + " " + resultType.getLLVMName() + " " + leftReg + ", " + rightReg);
        return res(resultReg, resultType);
    }

    private String compileStringOperation(BinaryOperatorContext c) {
        c.cctx().include(-1, null, "string", null);

        String left = c.leftReg();
        String right = c.rightReg();

        if (!(c.leftType() instanceof StrBuiltinType)) {
            left = c.cctx().nextRegister();
            c.cctx().emit(left + " = " + convertToString(c.line(), c.leftReg(), c.leftType()));
        }

        if (!(c.rightType() instanceof StrBuiltinType)) {
            right = c.cctx().nextRegister();
            c.cctx().emit(right + " = " + convertToString(c.line(), c.rightReg(), c.rightType()));
        }

        return "call i8* @strConcat(i8* " + left + ", i8* " + right + ")";
    }

    private String convertToString(int line, String reg, TypeRef type) {
        if (type instanceof ByteBuiltinType)  return "call i8* @byteToStr(i8 " + reg + ")";
        if (type instanceof ShortBuiltinType) return "call i8* @shortToStr(i16 " + reg + ")";
        if (type instanceof IntBuiltinType)   return "call i8* @intToStr(i32 " + reg + ")";
        if (type instanceof LongBuiltinType)  return "call i8* @longToStr(i64 " + reg + ")";
        if (type instanceof FloatBuiltinType) return "call i8* @floatToStr(float " + reg + ")";
        if (type instanceof DoubleBuiltinType) return "call i8* @doubleToStr(double " + reg + ")";
        if (type instanceof BoolBuiltinType)  return "call i8* @boolToStr(i1 " + reg + ")";
        if (type instanceof CharBuiltinType)  return "call i8* @charToStrAscii(i8 " + reg + ")";
        return new RStringConversionError(type.getName(), line).raise();
    }
}