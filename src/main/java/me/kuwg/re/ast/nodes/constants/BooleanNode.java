package me.kuwg.re.ast.nodes.constants;

import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.type.builtin.BuiltinTypes;

public class BooleanNode extends ConstantNode {
    private final boolean value;

    public BooleanNode(final int line, final boolean value) {
        super(line, BuiltinTypes.BOOL.getType());
        this.value = value;
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        return value ? "true" : "false";
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RValueMustBeUsedError("Boolean", line).raise();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Boolean: ").append(value).append(NEWLINE);
    }

    @Override
    public String compileToConstant(final CompilationContext cctx) {
        return value ? "1" : "0";
    }
}
