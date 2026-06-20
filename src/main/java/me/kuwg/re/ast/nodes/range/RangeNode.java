package me.kuwg.re.ast.nodes.range;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.RInternalError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.iterable.range.RangeType;

import java.util.Map;

public class RangeNode extends ValueNode {
    public RangeNode(final String fileName, final int line, final ValueNode start, final ValueNode end, final ValueNode step) {
        super(fileName, line, new RangeType(start, end, step));
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        RangeType self = (RangeType) this.type;

        self.start().replaceGenerics(generics, cctx);
        self.end().replaceGenerics(generics, cctx);
        self.step().replaceGenerics(generics, cctx);
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

    @Override
    public RangeNode clone() {
        RangeType t = (RangeType) type;
        return new RangeNode(fileName, line, t.start().clone(), t.end().clone(), t.step().clone());
    }
}
