package me.kuwg.re.operator;

import me.kuwg.re.operator.result.BOResult;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.*;

public abstract class BinaryOperator {
    private final int precedence;
    private final String symbol;

    protected BinaryOperator(final int precedence, final String symbol) {
        this.precedence = precedence;
        this.symbol = symbol;
    }

    public abstract BOResult compile(BinaryOperatorContext c);

    public final int getPrecedence() {
        return precedence;
    }

    public final String getSymbol() {
        return symbol;
    }

    public static BOResult res(String reg, TypeRef type) {
        return new BOResult(reg, type);
    }

    protected static boolean isInteger(TypeRef t) {
        return t instanceof ByteBuiltinType
                || t instanceof ShortBuiltinType
                || t instanceof IntBuiltinType
                || t instanceof LongBuiltinType;
    }

    public static boolean isFloat(TypeRef t) {
        return t instanceof FloatBuiltinType || t instanceof DoubleBuiltinType;
    }

    public static TypeRef promoteNumeric(TypeRef a, TypeRef b) {
        if (a instanceof DoubleBuiltinType || b instanceof DoubleBuiltinType) return BuiltinTypes.DOUBLE.getType();
        if (a instanceof FloatBuiltinType || b instanceof FloatBuiltinType) return BuiltinTypes.FLOAT.getType();
        if (a instanceof LongBuiltinType || b instanceof LongBuiltinType) return BuiltinTypes.LONG.getType();
        if (a instanceof IntBuiltinType || b instanceof IntBuiltinType) return BuiltinTypes.INT.getType();
        if (a instanceof ShortBuiltinType || b instanceof ShortBuiltinType) return BuiltinTypes.SHORT.getType();
        if (a instanceof ByteBuiltinType || b instanceof ByteBuiltinType) return BuiltinTypes.BYTE.getType();
        if (a instanceof CharBuiltinType || b instanceof CharBuiltinType) return BuiltinTypes.BYTE.getType();
        if (a instanceof BoolBuiltinType || b instanceof BoolBuiltinType) return BuiltinTypes.BOOL.getType();
        return null;
    }

    public static String convertToType(String reg, TypeRef from, TypeRef to, BinaryOperatorContext c) {
        if (from.equals(to)) return reg;
        String newReg = c.cctx().nextRegister();

        if (isInteger(from) && isInteger(to)) {
            c.cctx().emit(newReg + " = sext " + from.getLLVMName() + " " + reg + " to " + to.getLLVMName());
        } else if (isInteger(from) && isFloat(to)) {
            c.cctx().emit(newReg + " = sitofp " + from.getLLVMName() + " " + reg + " to " + to.getLLVMName());
        } else if (isFloat(from) && isInteger(to)) {
            c.cctx().emit(newReg + " = fptosi " + from.getLLVMName() + " " + reg + " to " + to.getLLVMName());
        } else if (isFloat(from) && isFloat(to)) {
            if (from instanceof FloatBuiltinType && to instanceof DoubleBuiltinType) {
                c.cctx().emit(newReg + " = fpext float " + reg + " to double");
            } else if (from instanceof DoubleBuiltinType && to instanceof FloatBuiltinType) {
                c.cctx().emit(newReg + " = fptrunc double " + reg + " to float");
            } else {
                return reg;
            }
        } else {
            return reg;
        }

        return newReg;
    }

    //        "!", "~" -> 13
    //        "<<", ">>", ">>>" -> 10
    //        "is" -> 9
    //        "&" -> 7
    //        "^" -> 6
    //        "|" -> 5
    //        "and" -> 4
    //        "or" -> 3
    //        "?:" -> 2
    //        "=", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=", ">>>=" -> 1
    //        -> -1
}
