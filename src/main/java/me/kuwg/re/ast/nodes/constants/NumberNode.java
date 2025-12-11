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

    private static String stripSuffix(String literal) {
        literal = literal.trim();
        if (literal.isEmpty()) return literal;

        char last = Character.toLowerCase(literal.charAt(literal.length() - 1));
        if (last == 'f' || last == 'd' || last == 'l') {
            return literal.substring(0, literal.length() - 1);
        }
        return literal;
    }

    private static TypeRef inferType(String literal) {
        literal = literal.trim().toLowerCase();

        if (literal.endsWith("f")) return BuiltinTypes.FLOAT.getType();
        if (literal.endsWith("d")) return BuiltinTypes.DOUBLE.getType();
        if (literal.endsWith("l")) return BuiltinTypes.LONG.getType();

        String numericPart = stripSuffix(literal).replace("_", "");

        try {
            if (numericPart.contains(".") || numericPart.contains("e") || numericPart.contains("p")) {
                return BuiltinTypes.DOUBLE.getType();
            }

            if (numericPart.startsWith("0x")) {
                long val = Long.parseLong(numericPart.substring(2), 16);
                if (val <= Byte.MAX_VALUE) return BuiltinTypes.BYTE.getType();
                if (val <= Short.MAX_VALUE) return BuiltinTypes.SHORT.getType();
                return BuiltinTypes.INT.getType();
            } else if (numericPart.startsWith("0b")) {
                long val = Long.parseLong(numericPart.substring(2), 2);
                if (val <= Short.MAX_VALUE) return BuiltinTypes.SHORT.getType();
                return BuiltinTypes.INT.getType();
            } else if (numericPart.startsWith("0") && numericPart.length() > 1) {
                return BuiltinTypes.INT.getType();
            } else {
                long val = Long.parseLong(numericPart);
                if (val <= Integer.MAX_VALUE) return BuiltinTypes.INT.getType();
                return BuiltinTypes.LONG.getType();
            }

        } catch (NumberFormatException e) {
            return BuiltinTypes.INT.getType();
        }
    }

    private static Number parseNumber(String literal) {
        literal = literal.trim().toLowerCase();
        String numericPart = stripSuffix(literal).replace("_", "");

        try {
            if (literal.endsWith("f")) return Float.parseFloat(numericPart);
            if (literal.endsWith("d")) return Double.parseDouble(numericPart);
            if (literal.endsWith("l")) return parseInteger(numericPart, BuiltinTypes.LONG.getType());
            if (numericPart.contains(".") || numericPart.contains("e") || numericPart.contains("p"))
                return Double.parseDouble(numericPart);

            if (numericPart.startsWith("0x")) return parseInteger(numericPart, null);
            if (numericPart.startsWith("0b")) return parseInteger(numericPart, BuiltinTypes.SHORT.getType());
            if (numericPart.startsWith("0") && numericPart.length() > 1)
                return parseInteger(numericPart, BuiltinTypes.INT.getType());

            return parseInteger(numericPart, null);

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric literal: " + literal);
        }
    }

    private static Number parseInteger(String numericPart, TypeRef forcedType) {
        long val;
        if (numericPart.startsWith("0x")) val = Long.parseLong(numericPart.substring(2), 16);
        else if (numericPart.startsWith("0b")) val = Long.parseLong(numericPart.substring(2), 2);
        else if (numericPart.startsWith("0") && numericPart.length() > 1)
            val = Long.parseLong(numericPart.substring(1), 8);
        else val = Long.parseLong(numericPart);

        if (forcedType != null) {
            if (forcedType.equals(BuiltinTypes.BYTE.getType())) return (byte) val;
            if (forcedType.equals(BuiltinTypes.SHORT.getType())) return (short) val;
            if (forcedType.equals(BuiltinTypes.INT.getType())) return (int) val;
            return val;
        }

        if (val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) return (int) val;
        return val;
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
        return type.equals(BuiltinTypes.INT.getType()) || type.equals(BuiltinTypes.LONG.getType()) || type.equals(BuiltinTypes.SHORT.getType()) || type.equals(BuiltinTypes.BYTE.getType());
    }

    private String formatLLVMValue() {
        if (value instanceof Float f) return Float.toString(f);
        if (value instanceof Double d) return Double.toString(d);
        return value.toString();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Number: ").append(value).append(", type: ").append(type.getName()).append(NEWLINE);
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RValueMustBeUsedError("Number", line).raise();
    }
}
