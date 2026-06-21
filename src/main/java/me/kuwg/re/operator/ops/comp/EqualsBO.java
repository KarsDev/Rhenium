package me.kuwg.re.operator.ops.comp;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.expr.RUnsupportedBinaryExpressionError;
import me.kuwg.re.operator.BinaryOperator;
import me.kuwg.re.operator.BinaryOperatorContext;
import me.kuwg.re.operator.result.BOResult;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.*;
import me.kuwg.re.type.ptr.NullType;
import me.kuwg.re.type.struct.StructType;

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

        if ((leftType instanceof NullType && !(rightType instanceof AnyPointerType)) ||
                (rightType instanceof NullType && !(leftType instanceof AnyPointerType))) {
            return new RUnsupportedBinaryExpressionError(
                    leftType.getName(), getSymbol(), rightType.getName(), c.fileName(), c.line()
            ).raise();
        }

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

        if (leftType instanceof NullType) {
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
                        leftType.getName(), getSymbol(), rightType.getName(), c.fileName(), c.line()
                ).raise();
            }

            String result = null;

            for (int i = 0; i < structDef.fields().size(); i++) {
                var field = structDef.fields().get(i);
                TypeRef fieldType = field.type();

                String leftVal = c.cctx().nextRegister();
                String rightVal = c.cctx().nextRegister();

                c.cctx().emit(leftVal + " = extractvalue "
                        + lt.getLLVMName() + " "
                        + c.leftReg() + ", " + i);

                c.cctx().emit(rightVal + " = extractvalue "
                        + rt.getLLVMName() + " "
                        + c.rightReg() + ", " + i);

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
            return new RUnsupportedBinaryExpressionError(leftType.getName(), getSymbol(), rightType.getName(), c.fileName(), c.line()).raise();
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

    @Override
    public String compileToConstant(final ValueNode left, final ValueNode right, final CompilationContext cctx) {
        final TypeRef leftType = left.getType();
        final TypeRef rightType = right.getType();

        if (leftType instanceof NullType && rightType instanceof NullType) {
            return "true";
        }

        if ((leftType instanceof NullType && !(rightType instanceof AnyPointerType)) ||
                (rightType instanceof NullType && !(leftType instanceof AnyPointerType))) {
            return unsupported(leftType, rightType, left).raise();
        }

        if (leftType instanceof StrBuiltinType && rightType instanceof StrBuiltinType) {
            return Boolean.toString(
                    left.compileToConstant(cctx)
                            .equals(right.compileToConstant(cctx))
            );
        }

        if (leftType instanceof BoolBuiltinType && rightType instanceof BoolBuiltinType) {
            return Boolean.toString(
                    Boolean.parseBoolean(left.compileToConstant(cctx)) ==
                            Boolean.parseBoolean(right.compileToConstant(cctx))
            );
        }

        if (leftType instanceof CharBuiltinType && rightType instanceof CharBuiltinType) {
            return Boolean.toString(
                    Integer.parseInt(left.compileToConstant(cctx)) ==
                            Integer.parseInt(right.compileToConstant(cctx))
            );
        }

        final TypeRef resultType = promoteNumeric(leftType, rightType);

        if (resultType == null) {
            return unsupported(leftType, rightType, left).raise();
        }

        try {
            if (resultType instanceof DoubleBuiltinType) {
                return Boolean.toString(
                        Double.parseDouble(left.compileToConstant(cctx)) ==
                                Double.parseDouble(right.compileToConstant(cctx))
                );
            }

            if (resultType instanceof FloatBuiltinType) {
                return Boolean.toString(
                        Float.parseFloat(left.compileToConstant(cctx)) ==
                                Float.parseFloat(right.compileToConstant(cctx))
                );
            }

            if (resultType instanceof LongBuiltinType) {
                return Boolean.toString(
                        Long.parseLong(left.compileToConstant(cctx)) ==
                                Long.parseLong(right.compileToConstant(cctx))
                );
            }

            if (resultType instanceof IntBuiltinType) {
                return Boolean.toString(
                        Integer.parseInt(left.compileToConstant(cctx)) ==
                                Integer.parseInt(right.compileToConstant(cctx))
                );
            }

            if (resultType instanceof ShortBuiltinType) {
                return Boolean.toString(
                        Short.parseShort(left.compileToConstant(cctx)) ==
                                Short.parseShort(right.compileToConstant(cctx))
                );
            }

            if (resultType instanceof ByteBuiltinType) {
                return Boolean.toString(
                        Byte.parseByte(left.compileToConstant(cctx)) ==
                                Byte.parseByte(right.compileToConstant(cctx))
                );
            }
        } catch (NumberFormatException ignored) {
        }

        return unsupported(leftType, rightType, left).raise();
    }
}
