package me.kuwg.re.operator;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.expr.RUnsupportedBinaryExpressionError;
import me.kuwg.re.operator.result.BOResult;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;
import me.kuwg.re.type.ptr.NullType;
import me.kuwg.re.type.ptr.PointerType;

public abstract class BinaryOperator {
    private final int precedence;
    private final String symbol;

    protected BinaryOperator(final int precedence, final String symbol) {
        this.precedence = precedence;
        this.symbol = symbol;
    }

    public static BOResult res(String reg, TypeRef type) {
        return new BOResult(reg, type);
    }

    protected static boolean isInteger(TypeRef t) {
        return t == BuiltinTypes.BYTE.getType()
                || t == BuiltinTypes.SHORT.getType()
                || t == BuiltinTypes.INT.getType()
                || t == BuiltinTypes.LONG.getType();
    }

    public static boolean isFloat(TypeRef t) {
        return t == BuiltinTypes.FLOAT.getType()
                || t == BuiltinTypes.DOUBLE.getType();
    }

    public static TypeRef promoteNumeric(TypeRef a, TypeRef b) {
        if (a == BuiltinTypes.ANYPTR.getType() || b == BuiltinTypes.ANYPTR.getType()) return null;

        if (a instanceof NullType && b instanceof PointerType) return b;
        if (b instanceof NullType && a instanceof PointerType) return a;

        if (a instanceof PointerType && b instanceof PointerType) return a;

        if (a == BuiltinTypes.DOUBLE.getType() || b == BuiltinTypes.DOUBLE.getType()) return BuiltinTypes.DOUBLE.getType();
        if (a == BuiltinTypes.FLOAT.getType() || b == BuiltinTypes.FLOAT.getType()) return BuiltinTypes.FLOAT.getType();
        if (a == BuiltinTypes.LONG.getType() || b == BuiltinTypes.LONG.getType()) return BuiltinTypes.LONG.getType();
        if (a == BuiltinTypes.INT.getType() || b == BuiltinTypes.INT.getType()) return BuiltinTypes.INT.getType();
        if (a == BuiltinTypes.SHORT.getType() || b == BuiltinTypes.SHORT.getType()) return BuiltinTypes.SHORT.getType();
        if (a == BuiltinTypes.BYTE.getType() || b == BuiltinTypes.BYTE.getType()) return BuiltinTypes.BYTE.getType();
        if (a == BuiltinTypes.CHAR.getType() || b == BuiltinTypes.CHAR.getType()) return BuiltinTypes.BYTE.getType();
        if (a == BuiltinTypes.BOOL.getType() && b == BuiltinTypes.BOOL.getType()) return BuiltinTypes.BOOL.getType();

        return null;
    }

    public static boolean isNumeric(TypeRef t) {
        return isInteger(t)
                || isFloat(t)
                || t == BuiltinTypes.CHAR.getType()
                || t == BuiltinTypes.BOOL.getType();
    }

    public static String convertToType(String reg, TypeRef from, TypeRef to, BinaryOperatorContext c) {
        if (from == to) return reg;

        String newReg = c.cctx().nextRegister();

        if (isInteger(from) && isInteger(to)) {
            c.cctx().emit(newReg + " = sext " + from.getLLVMName() + " " + reg + " to " + to.getLLVMName());
        } else if (isInteger(from) && isFloat(to)) {
            c.cctx().emit(newReg + " = sitofp " + from.getLLVMName() + " " + reg + " to " + to.getLLVMName());
        } else if (isFloat(from) && isInteger(to)) {
            c.cctx().emit(newReg + " = fptosi " + from.getLLVMName() + " " + reg + " to " + to.getLLVMName());
        } else if (isFloat(from) && isFloat(to)) {
            if (from == BuiltinTypes.FLOAT.getType() && to == BuiltinTypes.DOUBLE.getType()) {
                c.cctx().emit(newReg + " = fpext float " + reg + " to double");
            } else if (from == BuiltinTypes.DOUBLE.getType() && to == BuiltinTypes.FLOAT.getType()) {
                c.cctx().emit(newReg + " = fptrunc double " + reg + " to float");
            } else {
                return reg;
            }
        } else {
            return reg;
        }

        return newReg;
    }

    public abstract BOResult compile(BinaryOperatorContext c);

    public abstract String compileToConstant(ValueNode left, ValueNode right, CompilationContext cctx);

    public final int getPrecedence() {
        return precedence;
    }

    public final String getSymbol() {
        return symbol;
    }

    protected RUnsupportedBinaryExpressionError unsupported(TypeRef left, TypeRef right, ValueNode node) {
        return unsupported(left, right, node.getFileName(), node.getLine());
    }

    protected RUnsupportedBinaryExpressionError unsupported(TypeRef left, TypeRef right, String fileName, int line) {
        return new RUnsupportedBinaryExpressionError(left.getName(), getSymbol(), right.getName(), fileName, line);
    }
}
