package me.kuwg.re.operator.ops.comp;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.expr.RUnsupportedBinaryExpressionError;
import me.kuwg.re.operator.BinaryOperator;
import me.kuwg.re.operator.BinaryOperatorContext;
import me.kuwg.re.operator.result.BOResult;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;
import me.kuwg.re.type.ptr.NullType;
import me.kuwg.re.type.struct.StructType;
import me.kuwg.re.type.union.UnionType;

import java.util.Objects;

public final class EqualsBO extends BinaryOperator {
    public static final BinaryOperator INSTANCE = new EqualsBO();

    EqualsBO() {
        super(3, "==");
    }

    @Override
    public BOResult compile(final BinaryOperatorContext c) {
        TypeRef leftType = c.leftType();
        TypeRef rightType = c.rightType();

        if ((leftType instanceof NullType && !(rightType == BuiltinTypes.ANYPTR.getType() || rightType == BuiltinTypes.STR.getType())) || (rightType instanceof NullType && !(leftType == BuiltinTypes.ANYPTR.getType() || leftType == BuiltinTypes.STR.getType()))) {
            return new RUnsupportedBinaryExpressionError(leftType.getName(), getSymbol(), rightType.getName(), c.fileName(), c.line()).raise();
        }

        if (leftType == BuiltinTypes.STR.getType() && rightType instanceof NullType) {
            String resReg = c.cctx().nextRegister();
            c.cctx().emit(resReg + " = icmp eq ptr " + c.leftReg() + ", null");
            return res(resReg, BuiltinTypes.BOOL.getType());
        }

        if (leftType instanceof NullType && rightType == BuiltinTypes.STR.getType()) {
            String resReg = c.cctx().nextRegister();
            c.cctx().emit(resReg + " = icmp eq ptr null, " + c.rightReg());
            return res(resReg, BuiltinTypes.BOOL.getType());
        }

        if (leftType == BuiltinTypes.STR.getType() && rightType == BuiltinTypes.STR.getType()) {

            String resReg = c.cctx().nextRegister();
            c.cctx().emit(resReg + " = call i1 @strEquals(i8* " + c.leftReg() + ", i8* " + c.rightReg() + ")");
            return res(resReg, BuiltinTypes.BOOL.getType());
        }

        if (leftType == BuiltinTypes.ANYPTR.getType() && rightType == BuiltinTypes.ANYPTR.getType()) {
            String resReg = c.cctx().nextRegister();
            c.cctx().emit(resReg + " = icmp eq ptr " + c.leftReg() + ", " + c.rightReg());
            return res(resReg, BuiltinTypes.BOOL.getType());
        }

        if (leftType == BuiltinTypes.ANYPTR.getType() && rightType instanceof NullType) {
            String resReg = c.cctx().nextRegister();
            c.cctx().emit(resReg + " = icmp eq ptr " + c.leftReg() + ", null");
            return res(resReg, BuiltinTypes.BOOL.getType());
        }

        if (leftType instanceof NullType) {
            String resReg = c.cctx().nextRegister();
            c.cctx().emit(resReg + " = icmp eq ptr null, " + c.rightReg());
            return res(resReg, BuiltinTypes.BOOL.getType());
        }

        if (leftType instanceof UnionType lt && rightType instanceof UnionType rt) {
            if (!lt.getName().equals(rt.getName())) {
                return res("false", BuiltinTypes.BOOL.getType());
            }

            var unionDef = c.cctx().getUnion(lt.getName());
            if (unionDef == null) {
                return new RUnsupportedBinaryExpressionError(leftType.getName(), getSymbol(), rightType.getName(), c.fileName(), c.line()).raise();
            }

            // Tag is stored in slot 0.
            String leftTag = c.cctx().nextRegister();
            String rightTag = c.cctx().nextRegister();

            c.cctx().emit(leftTag + " = extractvalue " + lt.getLLVMName() + " " + c.leftReg() + ", 0");
            c.cctx().emit(rightTag + " = extractvalue " + rt.getLLVMName() + " " + c.rightReg() + ", 0");

            String tagEq = c.cctx().nextRegister();
            c.cctx().emit(tagEq + " = icmp eq i32 " + leftTag + ", " + rightTag);

            String payloadEq = null;

            for (int i = 0; i < unionDef.variants().size(); i++) {
                TypeRef fieldType = unionDef.variants().get(i);

                String leftVal = c.cctx().nextRegister();
                String rightVal = c.cctx().nextRegister();

                c.cctx().emit(leftVal + " = extractvalue " + lt.getLLVMName() + " " + c.leftReg() + ", " + (i + 1));

                c.cctx().emit(rightVal + " = extractvalue " + rt.getLLVMName() + " " + c.rightReg() + ", " + (i + 1));

                String fieldEq = c.cctx().nextRegister();

                if (fieldType == BuiltinTypes.STR.getType()) {
                    c.cctx().emit(fieldEq + " = call i1 @strEquals(i8* " + leftVal + ", i8* " + rightVal + ")");
                } else if (fieldType == BuiltinTypes.FLOAT.getType() || fieldType == BuiltinTypes.DOUBLE.getType()) {
                    c.cctx().emit(fieldEq + " = fcmp oeq " + fieldType.getLLVMName() + " " + leftVal + ", " + rightVal);
                } else {
                    c.cctx().emit(fieldEq + " = icmp eq " + fieldType.getLLVMName() + " " + leftVal + ", " + rightVal);
                }

                String leftActive = c.cctx().nextRegister();
                String rightActive = c.cctx().nextRegister();
                c.cctx().emit(leftActive + " = icmp eq i32 " + leftTag + ", " + i);
                c.cctx().emit(rightActive + " = icmp eq i32 " + rightTag + ", " + i);

                String activeBoth = c.cctx().nextRegister();
                c.cctx().emit(activeBoth + " = and i1 " + leftActive + ", " + rightActive);

                String caseEq = c.cctx().nextRegister();
                c.cctx().emit(caseEq + " = and i1 " + activeBoth + ", " + fieldEq);

                if (payloadEq == null) {
                    payloadEq = caseEq;
                } else {
                    String orReg = c.cctx().nextRegister();
                    c.cctx().emit(orReg + " = or i1 " + payloadEq + ", " + caseEq);
                    payloadEq = orReg;
                }
            }

            String finalPayloadEq = Objects.requireNonNullElse(payloadEq, "true");
            String resReg = c.cctx().nextRegister();
            c.cctx().emit(resReg + " = and i1 " + tagEq + ", " + finalPayloadEq);
            return res(resReg, BuiltinTypes.BOOL.getType());
        }

        if (leftType instanceof StructType lt && rightType instanceof StructType rt) {
            if (!lt.name().equals(rt.name())) {
                return res("false", BuiltinTypes.BOOL.getType());
            }

            String result = compileStructEquality(lt, rt, c.leftReg(), c.rightReg(), c);

            return res(result, BuiltinTypes.BOOL.getType());
        }

        TypeRef resultType = promoteNumeric(leftType, rightType);
        if (resultType == null) {
            return new RUnsupportedBinaryExpressionError(leftType.getName(), getSymbol(), rightType.getName(), c.fileName(), c.line()).raise();
        }

        String leftReg = convertToType(c.leftReg(), leftType, resultType, c);
        String rightReg = convertToType(c.rightReg(), rightType, resultType, c);
        String resReg = c.cctx().nextRegister();

        if (resultType == BuiltinTypes.FLOAT.getType() || resultType == BuiltinTypes.DOUBLE.getType()) {
            c.cctx().emit(resReg + " = fcmp oeq " + resultType.getLLVMName() + " " + leftReg + ", " + rightReg);
        } else {
            c.cctx().emit(resReg + " = icmp eq " + resultType.getLLVMName() + " " + leftReg + ", " + rightReg);
        }

        return res(resReg, BuiltinTypes.BOOL.getType());
    }

    @Override
    public String compileToConstant(final ValueNode left, final ValueNode right, final CompilationContext cctx) {
        final TypeRef leftType = left.getType();
        final TypeRef rightType = right.getType();

        if (leftType instanceof NullType && rightType instanceof NullType) {
            return "true";
        }

        if ((leftType instanceof NullType && !(rightType == BuiltinTypes.ANYPTR.getType() || rightType == BuiltinTypes.STR.getType())) || (rightType instanceof NullType && !(leftType == BuiltinTypes.ANYPTR.getType() || leftType == BuiltinTypes.STR.getType()))) {
            return unsupported(leftType, rightType, left).raise();
        }

        if ((leftType == BuiltinTypes.STR.getType() && rightType instanceof NullType) || (leftType instanceof NullType && rightType == BuiltinTypes.STR.getType())) {
            return "false";
        }

        if (leftType == BuiltinTypes.STR.getType() && rightType == BuiltinTypes.STR.getType()) {
            return Boolean.toString(left.compileToConstant(cctx).equals(right.compileToConstant(cctx)));
        }

        if (leftType == BuiltinTypes.BOOL.getType() && rightType == BuiltinTypes.BOOL.getType()) {
            return Boolean.toString(Boolean.parseBoolean(left.compileToConstant(cctx)) == Boolean.parseBoolean(right.compileToConstant(cctx)));
        }

        if (leftType == BuiltinTypes.CHAR.getType() && rightType == BuiltinTypes.CHAR.getType()) {
            return Boolean.toString(Integer.parseInt(left.compileToConstant(cctx)) == Integer.parseInt(right.compileToConstant(cctx)));
        }

        final TypeRef resultType = promoteNumeric(leftType, rightType);

        if (resultType == null) {
            return unsupported(leftType, rightType, left).raise();
        }

        try {
            if (resultType == BuiltinTypes.DOUBLE.getType()) {
                return Boolean.toString(Double.parseDouble(left.compileToConstant(cctx)) == Double.parseDouble(right.compileToConstant(cctx)));
            }

            if (resultType == BuiltinTypes.FLOAT.getType()) {
                return Boolean.toString(Float.parseFloat(left.compileToConstant(cctx)) == Float.parseFloat(right.compileToConstant(cctx)));
            }

            if (resultType == BuiltinTypes.LONG.getType()) {
                return Boolean.toString(Long.parseLong(left.compileToConstant(cctx)) == Long.parseLong(right.compileToConstant(cctx)));
            }

            if (resultType == BuiltinTypes.INT.getType()) {
                return Boolean.toString(Integer.parseInt(left.compileToConstant(cctx)) == Integer.parseInt(right.compileToConstant(cctx)));
            }

            if (resultType == BuiltinTypes.SHORT.getType()) {
                return Boolean.toString(Short.parseShort(left.compileToConstant(cctx)) == Short.parseShort(right.compileToConstant(cctx)));
            }

            if (resultType == BuiltinTypes.BYTE.getType()) {
                return Boolean.toString(Byte.parseByte(left.compileToConstant(cctx)) == Byte.parseByte(right.compileToConstant(cctx)));
            }
        } catch (NumberFormatException ignored) {
        }

        return unsupported(leftType, rightType, left).raise();
    }

    private String compileStructEquality(final StructType leftType, final StructType rightType, final String leftReg, final String rightReg, final BinaryOperatorContext c) {
        if (!leftType.name().equals(rightType.name())) {
            return "false";
        }

        var structDef = c.cctx().getStruct(leftType.name());
        if (structDef == null) {
            return new RUnsupportedBinaryExpressionError(leftType.getName(), getSymbol(), rightType.getName(), c.fileName(), c.line()).raise();
        }

        String result = null;

        for (int i = 0; i < structDef.fields().size(); i++) {
            var field = structDef.fields().get(i);
            TypeRef fieldType = field.type();

            String leftVal = c.cctx().nextRegister();
            String rightVal = c.cctx().nextRegister();

            c.cctx().emit(leftVal + " = extractvalue " + leftType.getLLVMName() + " " + leftReg + ", " + i);

            c.cctx().emit(rightVal + " = extractvalue " + rightType.getLLVMName() + " " + rightReg + ", " + i);

            String fieldEq = c.cctx().nextRegister();

            if (fieldType == BuiltinTypes.STR.getType()) {
                c.cctx().emit(fieldEq + " = call i1 @strEquals(i8* " + leftVal + ", i8* " + rightVal + ")");
            } else if (fieldType == BuiltinTypes.FLOAT.getType() || fieldType == BuiltinTypes.DOUBLE.getType()) {
                c.cctx().emit(fieldEq + " = fcmp oeq " + fieldType.getLLVMName() + " " + leftVal + ", " + rightVal);
            } else if (fieldType == BuiltinTypes.BOOL.getType() || fieldType == BuiltinTypes.CHAR.getType() || fieldType == BuiltinTypes.BYTE.getType() || fieldType == BuiltinTypes.SHORT.getType() || fieldType == BuiltinTypes.INT.getType() || fieldType == BuiltinTypes.LONG.getType()) {
                c.cctx().emit(fieldEq + " = icmp eq " + fieldType.getLLVMName() + " " + leftVal + ", " + rightVal);
            } else if (fieldType == BuiltinTypes.ANYPTR.getType()) {
                c.cctx().emit(fieldEq + " = icmp eq ptr " + leftVal + ", " + rightVal);
            } else if (fieldType instanceof StructType nestedStruct) {
                fieldEq = compileStructEquality(nestedStruct, nestedStruct, leftVal, rightVal, c);
            } else {
                return new RUnsupportedBinaryExpressionError(fieldType.getName(), getSymbol(), fieldType.getName(), c.fileName(), c.line()).raise();
            }

            if (result == null) {
                result = fieldEq;
            } else {
                String andReg = c.cctx().nextRegister();
                c.cctx().emit(andReg + " = and i1 " + result + ", " + fieldEq);
                result = andReg;
            }
        }

        return Objects.requireNonNullElse(result, "true");
    }
}
