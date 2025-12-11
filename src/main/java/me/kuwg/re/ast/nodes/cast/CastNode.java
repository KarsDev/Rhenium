package me.kuwg.re.ast.nodes.cast;

import me.kuwg.re.ast.value.ValueNode;
import me.kuwg.re.cast.CastManager;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.cast.RNotPrimitiveCastError;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.type.TypeRef;

public class CastNode extends ValueNode {
    private final ValueNode value;

    public CastNode(final int line, final TypeRef type, final ValueNode value) {
        super(line, type);
        this.value = value;

        if (!type.isPrimitive()) {
            new RNotPrimitiveCastError(type, line);
        }
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        return CastManager.executeCast(line, value, type, cctx);
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RValueMustBeUsedError("Cast", line).raise();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Cast: ").append(NEWLINE);
        sb.append(indent).append(TAB).append("Type: ").append(type.getName()).append(NEWLINE);
        sb.append(indent).append(TAB).append("Value: ").append(NEWLINE);
        value.write(sb, indent + TAB + TAB);
    }
}
