package me.kuwg.re.ast.nodes.loop;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.types.interrupt.InterruptNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.loop.RLoopError;
import me.kuwg.re.type.TypeRef;

import java.util.Map;

public class BreakNode extends ASTNode implements InterruptNode {
    public BreakNode(final int line) {
        super(line);
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
    }

    @Override
    public void compile(final CompilationContext cctx) {
        if (cctx.getLoopStack().isEmpty()) {
            new RLoopError("Break statement not inside a loop", line).raise();
            return;
        }

        var currentLoop = cctx.getLoopStack().peek();
        cctx.emit("; Break");
        cctx.emit("br label %" + currentLoop.endLabel());
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Break").append(NEWLINE);
    }

    @Override
    public BreakNode clone() {
        return this;
    }
}
