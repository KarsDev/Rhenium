package me.kuwg.re.ast.nodes.constants;

import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.type.builtin.BuiltinTypes;

public class CharacterNode extends ConstantNode {
    private final char value;

    public CharacterNode(final String fileName, final int line, final char value) {
        super(fileName, line, BuiltinTypes.CHAR.getType());
        this.value = value;
    }

    @Override
    public String compileToConstant(final CompilationContext cctx) {
        return Integer.toString(value);
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        String reg = cctx.nextRegister();

        cctx.emit(reg + " = add i8 0, " + (int) value + " ; char literal '" + printable(value) + "'");

        return reg;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        compileAndGet(cctx);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Char: '").append(printable(value)).append('\'').append(NEWLINE);
    }

    private String printable(char c) {
        return switch (c) {
            case '\0' -> "\\0";
            case '\n' -> "\\n";
            case '\t' -> "\\t";
            case '\r' -> "\\r";
            case '\\' -> "\\\\";
            case '\'' -> "\\'";
            default -> Character.isISOControl(c)
                    ? "\\x%02X".formatted((int) c)
                    : Character.toString(c);
        };
    }
}
