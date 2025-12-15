package me.kuwg.re.ast.nodes.range;

import me.kuwg.re.ast.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.RInternalError;
import me.kuwg.re.type.iterable.range.RangeType;

public class RangeNode extends ValueNode {
    public RangeNode(final int line, final ValueNode start, final ValueNode end, final ValueNode step) {
        super(line, new RangeType(start, end, step));
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        return "COMPILING RANGE NODE";
    }

    @Override
    public void compile(final CompilationContext cctx) {
        throw new RInternalError();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        RangeType range = (RangeType) type;
        sb.append(indent).append("Range: ").append(NEWLINE);

        sb.append(indent).append(TAB).append("Start: ").append(NEWLINE);
        range.start().write(sb, indent + TAB + TAB);
        sb.append(indent).append(TAB).append("End: ").append(NEWLINE);
        range.end().write(sb, indent + TAB + TAB);
        sb.append(indent).append(TAB).append("Step: ").append(NEWLINE);
        range.step().write(sb, indent + TAB + TAB);
    }
}
