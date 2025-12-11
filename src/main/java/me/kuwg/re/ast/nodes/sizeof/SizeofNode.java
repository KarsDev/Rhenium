package me.kuwg.re.ast.nodes.sizeof;

import me.kuwg.re.ast.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.type.builtin.BuiltinTypes;

public class SizeofNode extends ValueNode {
    private final ValueNode value;

    public SizeofNode(final int line, final ValueNode value) {
        super(line, BuiltinTypes.INT.getType());
        this.value = value;
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        value.compileAndGet(cctx);
        return Integer.toString(value.getType().getSize());
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RValueMustBeUsedError("sizeof", line).raise();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Sizeof: ").append(NEWLINE);
        value.write(sb, indent + TAB);
    }
}
