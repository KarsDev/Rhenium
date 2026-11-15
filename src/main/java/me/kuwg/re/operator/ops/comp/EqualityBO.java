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

/// Shared implementation of {@code ==} and {@code !=}
abstract class EqualityBO extends BinaryOperator {
    private final boolean negated;

    EqualityBO(final String symbol, final boolean negated) {
        super(3, symbol);
        this.negated = negated;
    }

    private static boolean isFloating(final TypeRef t) {
        return t == BuiltinTypes.FLOAT.getType() || t == BuiltinTypes.DOUBLE.getType();
    }

    private static boolean isIntegral(final TypeRef t) {
        return t == BuiltinTypes.BOOL.getType() || t == BuiltinTypes.CHAR.getType() || t == BuiltinTypes.BYTE.getType() || t == BuiltinTypes.SHORT.getType() || t == BuiltinTypes.INT.getType() || t == BuiltinTypes.LONG.getType();
    }

    private static boolean isAddress(final TypeRef t) {
        return t != BuiltinTypes.STR.getType() && (t == BuiltinTypes.ANYPTR.getType() || t.isPointer());
    }

    private static boolean isNullComparable(final TypeRef t) {
        return t == BuiltinTypes.STR.getType() || isAddress(t);
    }

    private static String intPred(final boolean neg) {
        return neg ? "ne" : "eq";
    }

    private static String literal(final boolean equal, final boolean neg) {
        return Boolean.toString(equal != neg);
    }

    private static String numericCompare(final TypeRef t, final String l, final String r, final boolean neg) {
        if (isFloating(t)) {
            return "fcmp " + (neg ? "une " : "oeq ") + t.getLLVMName() + " " + l + ", " + r;
        }
        return "icmp " + intPred(neg) + " " + t.getLLVMName() + " " + l + ", " + r;
    }

    private static String leafCompare(final TypeRef t, final String l, final String r, final boolean neg) {
        if (t == BuiltinTypes.STR.getType()) {
            return "call i1 @" + (neg ? "strNotEquals" : "strEquals") + "(i8* " + l + ", i8* " + r + ")";
        }
        if (isAddress(t)) {
            return "icmp " + intPred(neg) + " ptr " + l + ", " + r;
        }
        if (isFloating(t) || isIntegral(t)) {
            return numericCompare(t, l, r, neg);
        }
        return null;
    }

    private static String emit(final BinaryOperatorContext c, final String instruction) {
        final String reg = c.cctx().nextRegister();
        c.cctx().emit(reg + " = " + instruction);
        return reg;
    }

    private static String extract(final BinaryOperatorContext c, final String llvmType, final String agg, final int index) {
        return emit(c, "extractvalue " + llvmType + " " + agg + ", " + index);
    }

    private BOResult bool(final String reg) {
        return res(reg, BuiltinTypes.BOOL.getType());
    }

    private <T> T unsupportedCtx(final TypeRef l, final TypeRef r, final BinaryOperatorContext c) {
        return new RUnsupportedBinaryExpressionError(l.getName(), getSymbol(), r.getName(), c.fileName(), c.line()).raise();
    }

    @Override
    public BOResult compile(final BinaryOperatorContext c) {
        final TypeRef lt = c.leftType();
        final TypeRef rt = c.rightType();
        final boolean lNull = lt instanceof NullType;
        final boolean rNull = rt instanceof NullType;

        if (lNull && rNull) {
            return bool(literal(true, negated));
        }

        if (lNull || rNull) {
            final TypeRef other = lNull ? rt : lt;
            if (!isNullComparable(other)) {
                return unsupportedCtx(lt, rt, c);
            }
            return bool(emit(c, "icmp " + intPred(negated) + " ptr " + (lNull ? c.rightReg() : c.leftReg()) + ", null"));
        }

        if (lt == BuiltinTypes.STR.getType() && rt == BuiltinTypes.STR.getType()) {
            return bool(emit(c, leafCompare(lt, c.leftReg(), c.rightReg(), negated)));
        }

        if (isAddress(lt) && isAddress(rt)) {
            final boolean sameType = lt.getName().equals(rt.getName());
            if (!sameType && lt != BuiltinTypes.ANYPTR.getType() && rt != BuiltinTypes.ANYPTR.getType()) {
                return unsupportedCtx(lt, rt, c);
            }
            return bool(emit(c, "icmp " + intPred(negated) + " ptr " + c.leftReg() + ", " + c.rightReg()));
        }

        if (lt instanceof UnionType ul && rt instanceof UnionType ur) {
            return bool(compareUnions(ul, ur, c.leftReg(), c.rightReg(), c, negated));
        }

        if (lt instanceof StructType sl && rt instanceof StructType sr) {
            return bool(compareStructs(sl, sr, c.leftReg(), c.rightReg(), c, negated));
        }

        if (lt == rt && isIntegral(lt)) {
            return bool(emit(c, numericCompare(lt, c.leftReg(), c.rightReg(), negated)));
        }

        final TypeRef resultType = promoteNumeric(lt, rt);
        if (resultType == null) {
            return unsupportedCtx(lt, rt, c);
        }

        final String leftReg = convertToType(c.leftReg(), lt, resultType, c);
        final String rightReg = convertToType(c.rightReg(), rt, resultType, c);
        return bool(emit(c, numericCompare(resultType, leftReg, rightReg, negated)));
    }

    @Override
    public String compileToConstant(final ValueNode left, final ValueNode right, final CompilationContext cctx) {
        final TypeRef lt = left.getType();
        final TypeRef rt = right.getType();
        final boolean lNull = lt instanceof NullType;
        final boolean rNull = rt instanceof NullType;

        if (lNull && rNull) {
            return literal(true, negated);
        }

        if (lNull || rNull) {
            // A constant string is never null.
            if ((lNull ? rt : lt) == BuiltinTypes.STR.getType()) {
                return literal(false, negated);
            }
            return new RUnsupportedBinaryExpressionError(lt.getName(), getSymbol(), rt.getName(), left.getFileName(), left.getLine()).raise();
        }

        if (lt == BuiltinTypes.STR.getType() && rt == BuiltinTypes.STR.getType()) {
            return literal(left.compileToConstant(cctx).equals(right.compileToConstant(cctx)), negated);
        }

        if (lt == BuiltinTypes.BOOL.getType() && rt == BuiltinTypes.BOOL.getType()) {
            return literal(Boolean.parseBoolean(left.compileToConstant(cctx)) == Boolean.parseBoolean(right.compileToConstant(cctx)), negated);
        }

        if (lt == BuiltinTypes.CHAR.getType() && rt == BuiltinTypes.CHAR.getType()) {
            return literal(Integer.parseInt(left.compileToConstant(cctx)) == Integer.parseInt(right.compileToConstant(cctx)), negated);
        }

        if (lt instanceof StructType || rt instanceof StructType || lt instanceof UnionType || rt instanceof UnionType) {
            return new RUnsupportedBinaryExpressionError(lt.getName(), getSymbol(), rt.getName(), left.getFileName(), left.getLine()).raise();
        }

        final TypeRef resultType = promoteNumeric(lt, rt);
        if (resultType == null) {
            return new RUnsupportedBinaryExpressionError(lt.getName(), getSymbol(), rt.getName(), left.getFileName(), left.getLine()).raise();
        }

        try {
            final String l = left.compileToConstant(cctx);
            final String r = right.compileToConstant(cctx);

            if (resultType == BuiltinTypes.DOUBLE.getType())
                return literal(Double.parseDouble(l) == Double.parseDouble(r), negated);
            if (resultType == BuiltinTypes.FLOAT.getType())
                return literal(Float.parseFloat(l) == Float.parseFloat(r), negated);
            if (resultType == BuiltinTypes.LONG.getType())
                return literal(Long.parseLong(l) == Long.parseLong(r), negated);
            if (resultType == BuiltinTypes.INT.getType())
                return literal(Integer.parseInt(l) == Integer.parseInt(r), negated);
            if (resultType == BuiltinTypes.SHORT.getType())
                return literal(Short.parseShort(l) == Short.parseShort(r), negated);
            if (resultType == BuiltinTypes.BYTE.getType())
                return literal(Byte.parseByte(l) == Byte.parseByte(r), negated);
        } catch (NumberFormatException ignored) {
        }

        return new RUnsupportedBinaryExpressionError(lt.getName(), getSymbol(), rt.getName(), left.getFileName(), left.getLine()).raise();
    }

    private String compareStructs(final StructType lt, final StructType rt, final String l, final String r,
                                  final BinaryOperatorContext c, final boolean neg) {
        if (!lt.name().equals(rt.name())) {
            return literal(false, neg);
        }

        final var def = c.cctx().getStruct(lt.name());
        if (def == null) {
            return unsupportedCtx(lt, rt, c);
        }

        final String combine = neg ? "or" : "and";
        String result = null;

        for (int i = 0; i < def.fields().size(); i++) {
            final TypeRef fieldType = def.fields().get(i).type();

            final String lv = extract(c, lt.getLLVMName(), l, i);
            final String rv = extract(c, rt.getLLVMName(), r, i);
            final String part = compareValues(fieldType, lv, rv, c, neg);

            result = result == null ? part : emit(c, combine + " i1 " + result + ", " + part);
        }

        return result != null ? result : literal(true, neg);
    }

    private String compareUnions(final UnionType lt, final UnionType rt, final String l, final String r,
                                 final BinaryOperatorContext c, final boolean neg) {
        if (!lt.getName().equals(rt.getName())) {
            return literal(false, neg);
        }

        final var def = c.cctx().getUnion(lt.getName());
        if (def == null) {
            return unsupportedCtx(lt, rt, c);
        }

        final String leftTag = extract(c, lt.getLLVMName(), l, 0);
        final String rightTag = extract(c, rt.getLLVMName(), r, 0);
        final String tagEq = emit(c, "icmp eq i32 " + leftTag + ", " + rightTag);

        String payloadEq = null;

        for (int i = 0; i < def.variants().size(); i++) {
            final TypeRef variant = def.variants().get(i);

            final String lv = extract(c, lt.getLLVMName(), l, i + 1);
            final String rv = extract(c, rt.getLLVMName(), r, i + 1);
            final String valueEq = compareValues(variant, lv, rv, c, false);

            final String active = emit(c, "icmp eq i32 " + leftTag + ", " + i);
            final String caseEq = emit(c, "and i1 " + active + ", " + valueEq);

            payloadEq = payloadEq == null ? caseEq : emit(c, "or i1 " + payloadEq + ", " + caseEq);
        }

        final String eq = payloadEq == null ? tagEq : emit(c, "and i1 " + tagEq + ", " + payloadEq);
        return neg ? emit(c, "xor i1 " + eq + ", true") : eq;
    }

    private String compareValues(final TypeRef type, final String l, final String r,
                                 final BinaryOperatorContext c, final boolean neg) {
        if (type instanceof StructType s) return compareStructs(s, s, l, r, c, neg);
        if (type instanceof UnionType u) return compareUnions(u, u, l, r, c, neg);

        final String ir = leafCompare(type, l, r, neg);
        return ir == null ? unsupportedCtx(type, type, c) : emit(c, ir);
    }
}