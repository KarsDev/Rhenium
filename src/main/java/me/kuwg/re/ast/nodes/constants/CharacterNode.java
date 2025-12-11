package me.kuwg.re.ast.nodes.constants;

import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.type.builtin.BuiltinTypes;

public class CharacterNode extends ConstantNode {
    private final char value;

    public CharacterNode(final int line, final char value) {
        super(line, BuiltinTypes.CHAR.getType());
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
        new RValueMustBeUsedError("Char", line).raise();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Char: ").append(value).append(NEWLINE);
    }

    private String printable(char c) {
        return switch (c) {
            case '\n' -> "\\n";
            case '\t' -> "\\t";
            case '\r' -> "\\r";
            case '\\' -> "\\\\";
            case '\'' -> "\\'";
            default -> Character.toString(c);
        };
    }
}
