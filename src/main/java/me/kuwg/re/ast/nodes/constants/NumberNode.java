package me.kuwg.re.ast.nodes.constants;

import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;

public class NumberNode extends ConstantNode {
    public static final NumberNode ZERO = new NumberNode(0, "0");
    private final Number value;

    public NumberNode(final int line, String literal) {
        super(line, inferType(literal));
        this.value = parseNumber(literal);
    }

    private static TypeRef inferType(String literal) {
        literal = literal.trim().toLowerCase();

        if (isDecimalFloating(literal)) {
            if (literal.endsWith("f")) return BuiltinTypes.FLOAT.getType();
            return BuiltinTypes.DOUBLE.getType();
        }

        char last = literal.charAt(literal.length() - 1);
        if (last == 'l' || last == 's' || last == 'b') {
            literal = literal.substring(0, literal.length() - 1);
        }

        long value = parseLong(literal);
        if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
            return BuiltinTypes.INT.getType();
        } else {
            return BuiltinTypes.LONG.getType();
        }
    }

    private static Number parseNumber(String literal) {
        literal = literal.trim().toLowerCase();

        if (isDecimalFloating(literal)) {
            if (literal.endsWith("f")) {
                return Float.parseFloat(literal.substring(0, literal.length() - 1));
            }
            return Double.parseDouble(literal);
        }

        char last = literal.charAt(literal.length() - 1);
        boolean forceLong = false, forceShort = false, forceByte = false;

        if (last == 'l') { literal = literal.substring(0, literal.length() - 1); forceLong = true; }
        else if (last == 's') { literal = literal.substring(0, literal.length() - 1); forceShort = true; }
        else if (last == 'b') { literal = literal.substring(0, literal.length() - 1); forceByte = true; }

        long value = parseLong(literal);

        if (forceLong) return value;
        if (forceShort) return (short) value;
        if (forceByte) return (byte) value;

        if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) return (int) value;
        return value;
    }

    private static long parseLong(String literal) {
        int radix = 10;

        if (literal.startsWith("0x")) { radix = 16; literal = literal.substring(2); }
        else if (literal.startsWith("0b")) { radix = 2; literal = literal.substring(2); }
        else if (literal.startsWith("0") && literal.length() > 1) { radix = 8; literal = literal.substring(1); }

        return Long.parseLong(literal, radix);
    }

    private static boolean isDecimalFloating(String literal) {
        return !literal.startsWith("0x")
                && !literal.startsWith("0b")
                && (literal.contains(".") || literal.contains("e"));
    }

    public Number getValue() {
        return value;
    }

    @Override
    public String compileToConstant(final CompilationContext cctx) {
        return formatLLVMValue();
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        String llvmType = type.getLLVMName();
        String literal = formatLLVMValue();

        if (isIntegerType()) return literal;

        String reg = cctx.nextRegister();
        cctx.emit(reg + " = fadd " + llvmType + " 0.0, " + literal + " ; load float literal");
        return reg;
    }

    private boolean isIntegerType() {
        return type.equals(BuiltinTypes.INT.getType()) || type.equals(BuiltinTypes.LONG.getType()) ||
                type.equals(BuiltinTypes.SHORT.getType()) || type.equals(BuiltinTypes.BYTE.getType());
    }

    private String formatLLVMValue() {
        if (value instanceof Float f) return Float.toString(f);
        if (value instanceof Double d) return Double.toString(d);
        return value.toString();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Number: ").append(value).append(", type: ").append(type.getName()).append("\n");
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RValueMustBeUsedError("Number", line).raise();
    }
}
