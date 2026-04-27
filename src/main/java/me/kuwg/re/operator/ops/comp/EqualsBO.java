package me.kuwg.re.operator.ops.comp;

import me.kuwg.re.error.errors.expr.RUnsupportedBinaryExpressionError;
import me.kuwg.re.operator.BinaryOperator;
import me.kuwg.re.operator.BinaryOperatorContext;
import me.kuwg.re.operator.result.BOResult;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.*;
import me.kuwg.re.type.ptr.NullType;
import me.kuwg.re.type.struct.StructType;

import java.util.Objects;

public class EqualsBO extends BinaryOperator {
    public static final BinaryOperator INSTANCE = new EqualsBO();

    EqualsBO() {
        super(8, "==");
    }

    @Override
    public BOResult compile(final BinaryOperatorContext c) {
        TypeRef leftType = c.leftType();
        TypeRef rightType = c.rightType();

        if (leftType instanceof StrBuiltinType && rightType instanceof StrBuiltinType) {
            c.cctx().include(-1, null, "string", null);

            String resReg = c.cctx().nextRegister();
            c.cctx().emit(resReg + " = call i1 @strEquals(i8* " + c.leftReg() + ", i8* " + c.rightReg() + ")");
            return res(resReg, BuiltinTypes.BOOL.getType());
        }

        if (leftType instanceof AnyPointerType && rightType instanceof AnyPointerType) {
            String resReg = c.cctx().nextRegister();
            c.cctx().emit(
                    resReg + " = icmp eq ptr " + c.leftReg() + ", " + c.rightReg()
            );
            return res(resReg, BuiltinTypes.BOOL.getType());
        }

        if (leftType instanceof AnyPointerType && rightType instanceof NullType) {
            String resReg = c.cctx().nextRegister();
            c.cctx().emit(
                    resReg + " = icmp eq ptr " + c.leftReg() + ", null"
            );
            return res(resReg, BuiltinTypes.BOOL.getType());
        }

        if (leftType instanceof NullType && rightType instanceof AnyPointerType) {
            String resReg = c.cctx().nextRegister();
            c.cctx().emit(
                    resReg + " = icmp eq ptr null, " + c.rightReg()
            );
            return res(resReg, BuiltinTypes.BOOL.getType());
        }

        if (leftType instanceof StructType lt && rightType instanceof StructType rt) {
            if (!lt.name().equals(rt.name())) {
                return res("false", BuiltinTypes.BOOL.getType());
            }

            var structDef = c.cctx().getStruct(lt.name());
            if (structDef == null) {
                return new RUnsupportedBinaryExpressionError(
                        leftType.getName(), getSymbol(), rightType.getName(), c.line()
                ).raise();
            }

            String result = null;

            for (int i = 0; i < structDef.fields().size(); i++) {
                var field = structDef.fields().get(i);
                TypeRef fieldType = field.type();

                String leftFieldPtr = c.cctx().nextRegister();
                String rightFieldPtr = c.cctx().nextRegister();

                c.cctx().emit(leftFieldPtr + " = getelementptr "
                        + lt.getLLVMName() + ", "
                        + lt.getLLVMName() + "* " + c.leftReg()
                        + ", i32 0, i32 " + i);

                c.cctx().emit(rightFieldPtr + " = getelementptr "
                        + rt.getLLVMName() + ", "
                        + rt.getLLVMName() + "* " + c.rightReg()
                        + ", i32 0, i32 " + i);

                String leftVal = c.cctx().nextRegister();
                String rightVal = c.cctx().nextRegister();

                c.cctx().emit(leftVal + " = load " + fieldType.getLLVMName()
                        + ", " + fieldType.getLLVMName() + "* " + leftFieldPtr);

                c.cctx().emit(rightVal + " = load " + fieldType.getLLVMName()
                        + ", " + fieldType.getLLVMName() + "* " + rightFieldPtr);

                String fieldEq = c.cctx().nextRegister();

                if (fieldType instanceof FloatBuiltinType || fieldType instanceof DoubleBuiltinType) {
                    c.cctx().emit(fieldEq + " = fcmp oeq "
                            + fieldType.getLLVMName() + " "
                            + leftVal + ", " + rightVal);
                } else {
                    c.cctx().emit(fieldEq + " = icmp eq "
                            + fieldType.getLLVMName() + " "
                            + leftVal + ", " + rightVal);
                }

                if (result == null) {
                    result = fieldEq;
                } else {
                    String andReg = c.cctx().nextRegister();
                    c.cctx().emit(andReg + " = and i1 " + result + ", " + fieldEq);
                    result = andReg;
                }
            }

            return res(Objects.requireNonNullElse(result, "true"), BuiltinTypes.BOOL.getType());
        }

        TypeRef resultType = promoteNumeric(leftType, rightType);
        if (resultType == null) {
            return new RUnsupportedBinaryExpressionError(leftType.getName(), getSymbol(), rightType.getName(), c.line()).raise();
        }

        String leftReg = convertToType(c.leftReg(), leftType, resultType, c);
        String rightReg = convertToType(c.rightReg(), rightType, resultType, c);
        String resReg = c.cctx().nextRegister();

        if (resultType instanceof FloatBuiltinType || resultType instanceof DoubleBuiltinType) {
            c.cctx().emit(resReg + " = fcmp oeq " + resultType.getLLVMName() + " " + leftReg + ", " + rightReg);
        } else {
            c.cctx().emit(resReg + " = icmp eq " + resultType.getLLVMName() + " " + leftReg + ", " + rightReg);
        }

        return res(resReg, BuiltinTypes.BOOL.getType());
    }
}
