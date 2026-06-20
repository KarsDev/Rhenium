package me.kuwg.re.ast.nodes.loop;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.loop.RLoopError;
import me.kuwg.re.type.TypeRef;

import java.util.Map;

public class ContinueNode extends ASTNode {
    public ContinueNode(final String fileName, final int line) {
        super(fileName, line);
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
    }

    @Override
    public void compile(final CompilationContext cctx) {
        if (cctx.getLoopStack().isEmpty()) {
            new RLoopError("Continue statement not inside a loop", fileName, line).raise();
            return;
        }

        var currentLoop = cctx.getLoopStack().peek();
        cctx.emit("; Continue");
        cctx.emit("br label %" + currentLoop.startLabel());
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Continue").append(NEWLINE);
    }

    @Override
    public ContinueNode clone() {
        return this;
    }
}
