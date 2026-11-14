package me.kuwg.re.ast.nodes.constants;

import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.number.RNumberFormatError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;

public class NumberNode extends ConstantNode {
    public static final NumberNode ZERO = new NumberNode("_", 0, "0");
    private final Number value;

    public NumberNode(final String fileName, final int line, final String literal) {
        super(fileName, line, inferType(normalizeLiteral(literal, fileName, line), fileName, line));
        this.value = parseNumber(normalizeLiteral(literal, fileName, line), fileName, line);
    }

    private static String normalizeLiteral(final String literal, final String fileName, final int line) {
        if (literal == null)
            return new RNumberFormatError("Number literal cannot be null", fileName, line).raise();

        String normalized = literal.replace("_", "").trim().toLowerCase();
        if (normalized.isEmpty())
            return new RNumberFormatError("Number literal cannot be empty", fileName, line).raise();

        return normalized;
    }

    private static TypeRef inferType(final String literal, final String fileName, final int line) {
        try {
            if (isFloatingLiteral(literal)) {
                boolean isFloat = literal.endsWith("f");
                parseFloatingValue(literal, isFloat, fileName, line);
                return isFloat ? BuiltinTypes.FLOAT.getType() : BuiltinTypes.DOUBLE.getType();
            }

            IntegerSuffix suffix = getIntegerSuffix(literal);
            String number = suffix.strip(literal);
            long parsed = parseLong(number, fileName, line);

            if (suffix == IntegerSuffix.BYTE) {
                requireRange(parsed, Byte.MIN_VALUE, Byte.MAX_VALUE, "byte", fileName, line);
                return BuiltinTypes.BYTE.getType();
            }
            if (suffix == IntegerSuffix.SHORT) {
                requireRange(parsed, Short.MIN_VALUE, Short.MAX_VALUE, "short", fileName, line);
                return BuiltinTypes.SHORT.getType();
            }
            if (suffix == IntegerSuffix.LONG) {
                return BuiltinTypes.LONG.getType();
            }

            return parsed >= Integer.MIN_VALUE && parsed <= Integer.MAX_VALUE ? BuiltinTypes.INT.getType() : BuiltinTypes.LONG.getType();
        } catch (RuntimeException exception) {
            final String message = "Invalid number literal '" + literal + "': " + exception.getMessage();
            new RNumberFormatError(message, fileName, line).raise();
            return BuiltinTypes.INT.getType();
        }
    }

    private static Number parseNumber(final String literal, final String fileName, final int line) {
        try {
            if (isFloatingLiteral(literal)) {
                return parseFloatingValue(literal, literal.endsWith("f"), fileName, line);
            }

            IntegerSuffix suffix = getIntegerSuffix(literal);
            long parsed = parseLong(suffix.strip(literal), fileName, line);

            if (suffix == IntegerSuffix.BYTE) {
                requireRange(parsed, Byte.MIN_VALUE, Byte.MAX_VALUE, "byte", fileName, line);
                return (byte) parsed;
            }
            if (suffix == IntegerSuffix.SHORT) {
                requireRange(parsed, Short.MIN_VALUE, Short.MAX_VALUE, "short", fileName, line);
                return (short) parsed;
            }
            if (suffix == IntegerSuffix.LONG) {
                return parsed;
            }

            if (parsed >= Integer.MIN_VALUE && parsed <= Integer.MAX_VALUE) {
                return (int) parsed;
            }
            return parsed;
        } catch (RuntimeException exception) {
            return new RNumberFormatError("Invalid number literal '" + literal + "': " +
                    exception.getMessage(), fileName, line).raise();
        }
    }

    private static Number parseFloatingValue(
            final String literal,
            final boolean asFloat,
            final String fileName,
            final int line
    ) {
        String number = literal;
        char last = number.charAt(number.length() - 1);

        if (last == 'f' || last == 'd') {
            number = number.substring(0, number.length() - 1);
        }

        try {
            if (asFloat) {
                float parsed = Float.parseFloat(number);
                if (!Float.isFinite(parsed)) {
                    throw new NumberFormatException("Floating-point value is out of range");
                }
                return parsed;
            }

            double parsed = Double.parseDouble(number);
            if (!Double.isFinite(parsed)) {
                throw new NumberFormatException("Floating-point value is out of range");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            return new RNumberFormatError("Invalid floating-point literal '" + literal + "': " +
                    exception.getMessage(), fileName, line).raise();
        }
    }

    private static long parseLong(final String literal, final String fileName, final int line) {
        if (literal == null || literal.isEmpty())
            return new RNumberFormatError("Missing integer digits", fileName, line).raise();


        try {
            int radix = 10;
            String digits = literal;

            if (digits.startsWith("0x")) {
                radix = 16;
                digits = digits.substring(2);
            } else if (digits.startsWith("0b")) {
                radix = 2;
                digits = digits.substring(2);
            } else if (digits.startsWith("0") && digits.length() > 1) {
                radix = 8;
                digits = digits.substring(1);
            }

            if (digits.isEmpty()) {
                throw new NumberFormatException("Missing digits after numeric prefix");
            }

            if (radix == 16 || radix == 2) {
                return Long.parseUnsignedLong(digits, radix);
            }
            return Long.parseLong(digits, radix);
        } catch (NumberFormatException exception) {
            return new RNumberFormatError("Invalid integer literal '" + literal + "': " +
                    exception.getMessage(), fileName, line).raise();
        }
    }

    private static boolean isFloatingLiteral(final String literal) {
        if (literal.startsWith("0x") || literal.startsWith("0b")) {
            return false;
        }

        String number = literal;
        char last = number.charAt(number.length() - 1);
        if (last == 'f' || last == 'd') {
            number = number.substring(0, number.length() - 1);
        }

        return number.contains(".") || number.contains("e") || literal.endsWith("f") || literal.endsWith("d");
    }

    private static IntegerSuffix getIntegerSuffix(final String literal) {
        char last = literal.charAt(literal.length() - 1);
        return switch (last) {
            case 'b' -> IntegerSuffix.BYTE;
            case 's' -> IntegerSuffix.SHORT;
            case 'l' -> IntegerSuffix.LONG;
            default -> IntegerSuffix.NONE;
        };
    }

    private static void requireRange(
            final long value,
            final long minimum,
            final long maximum,
            final String typeName,
            final String fileName,
            final int line
    ) {
        if (value >= minimum && value <= maximum) return;

        new RNumberFormatError("Value " + value + " is outside the " + typeName +
                " range [" + minimum + ", " + maximum + "]", fileName, line).raise();
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
        try {
            if (value instanceof Float f) return Float.toString(f);
            if (value instanceof Double d) return Double.toString(d);
            return value.toString();
        } catch (RuntimeException exception) {
            return new RNumberFormatError("Could not format number literal: " + exception.getMessage(),
                    fileName, line).raise();
        }
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Number: ").append(value).append(", type: ").append(type.getName()).append("\n");
    }

    @Override
    public void compile(final CompilationContext cctx) {
        compileAndGet(cctx);
    }

    private enum IntegerSuffix {
        NONE, BYTE, SHORT, LONG;

        private String strip(final String literal) {
            return this == NONE ? literal : literal.substring(0, literal.length() - 1);
        }
    }
}