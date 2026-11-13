package me.kuwg.re.ast.nodes.label;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.types.interrupt.InterruptNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.label.LabelContext;
import me.kuwg.re.error.errors.loop.RLoopError;
import me.kuwg.re.type.TypeRef;

import java.util.Map;

public class LabelBreakNode extends ASTNode implements InterruptNode {
    private final String name;

    public LabelBreakNode(final String fileName, final int line, final String name) {
        super(fileName, line);
        this.name = name;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
    }

    @Override
    public void compile(final CompilationContext cctx) {
        LabelContext target = cctx.getLabel(name);

        if (target == null) {
            new RLoopError("No enclosing block named '" + name + "'", fileName, line).raise();
            return;
        }

        cctx.emit("; Named break: " + name);
        cctx.emit("br label %" + target.endLabel());
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Break").append(NEWLINE);
    }

    @Override
    public LabelBreakNode clone() {
        return this;
    }
}
