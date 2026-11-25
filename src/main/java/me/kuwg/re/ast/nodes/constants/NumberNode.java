package me.kuwg.re.ast.nodes.constants;

import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.number.RNumberFormatError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;

import java.util.Locale;

public class NumberNode extends ConstantNode {
    public static final NumberNode ZERO = new NumberNode("_", 0, "0");

    private final Number value;

    public NumberNode(final String fileName, final int line, final String literal) {
        this(fileName, line, parse(literal, fileName, line));
    }

    private NumberNode(final String fileName, final int line, final Parsed parsed) {
        super(fileName, line, parsed.type());
        this.value = parsed.value();
    }

    private static Parsed parse(final String literal, final String fileName, final int line) {
        if (literal == null)
            return new RNumberFormatError("Number literal cannot be null", fileName, line).raise();

        final String text = literal.replace("_", "").trim().toLowerCase(Locale.ROOT);
        if (text.isEmpty())
            return new RNumberFormatError("Number literal cannot be empty", fileName, line).raise();

        try {
            return isFloatingLiteral(text) ? parseFloating(text) : parseInteger(text, fileName, line);
        } catch (NumberFormatException exception) {
            return new RNumberFormatError("Invalid number literal '" + literal + "': " +
                    exception.getMessage(), fileName, line).raise();
        }
    }

    private static boolean isFloatingLiteral(final String text) {
        if (text.startsWith("0x") || text.startsWith("0b")) return false;

        return text.contains(".") || text.contains("e") || text.endsWith("f") || text.endsWith("d");
    }

    private static Parsed parseFloating(final String text) {
        final boolean isFloat = text.endsWith("f");
        final String number = isFloat || text.endsWith("d") ? text.substring(0, text.length() - 1) : text;

        final Number value;
        if (isFloat) value = Float.parseFloat(number);
        else value = Double.parseDouble(number);

        if (!Double.isFinite(value.doubleValue()))
            throw new NumberFormatException("Floating-point value is out of range");

        return new Parsed(value, (isFloat ? BuiltinTypes.FLOAT : BuiltinTypes.DOUBLE).getType());
    }

    private static Parsed parseInteger(final String text, final String fileName, final int line) {
        final boolean hex = text.startsWith("0x");
        final char suffix = text.charAt(text.length() - 1);
        final boolean hasSuffix = suffix == 's' || suffix == 'l' || (suffix == 'b' && !hex);

        String digits = hasSuffix ? text.substring(0, text.length() - 1) : text;
        int radix = 10;

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

        final boolean bitPattern = radix == 16 || radix == 2;
        final long number = bitPattern ? Long.parseUnsignedLong(digits, radix) : Long.parseLong(digits, radix);
        final boolean wrapped = bitPattern && number < 0;

        if (hasSuffix && suffix == 'l')
            return new Parsed(number, BuiltinTypes.LONG.getType());

        if (hasSuffix) {
            final boolean isByte = suffix == 'b';
            final String typeName = isByte ? "byte" : "short";

            if (wrapped)
                return new RNumberFormatError("Literal '" + text + "' is outside the " + typeName + " range",
                        fileName, line).raise();

            if (isByte) {
                requireRange(number, Byte.MIN_VALUE, Byte.MAX_VALUE, typeName, fileName, line);
                return new Parsed((byte) number, BuiltinTypes.BYTE.getType());
            }

            requireRange(number, Short.MIN_VALUE, Short.MAX_VALUE, typeName, fileName, line);
            return new Parsed((short) number, BuiltinTypes.SHORT.getType());
        }

        if (!wrapped && number == (int) number)
            return new Parsed((int) number, BuiltinTypes.INT.getType());

        return new Parsed(number, BuiltinTypes.LONG.getType());
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
        if (value instanceof Float || value instanceof Double)
            return String.format("0x%016X", Double.doubleToRawLongBits(value.doubleValue()));

        return value.toString();
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        return compileToConstant(cctx);
    }

    @Override
    public void compile(final CompilationContext cctx) {
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Number: ").append(value)
                .append(", type: ").append(type.getName()).append(NEWLINE);
    }

    private record Parsed(Number value, TypeRef type) {}
}
