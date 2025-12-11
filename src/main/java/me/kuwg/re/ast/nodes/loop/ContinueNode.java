package me.kuwg.re.ast.nodes.loop;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.loop.RLoopError;

public class ContinueNode extends ASTNode {
    public ContinueNode(final int line) {
        super(line);
    }

    @Override
    public void compile(final CompilationContext cctx) {
        if (cctx.getLoopStack().isEmpty()) {
            new RLoopError("Continue statement not inside a loop", line).raise();
            return;
        }

        var currentLoop = cctx.getLoopStack().peek();
        cctx.emit("br label %" + currentLoop.startLabel());
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Continue").append(NEWLINE);
    }
}
