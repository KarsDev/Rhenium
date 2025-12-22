package me.kuwg.re.ast.nodes.raise;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.constants.NumberNode;
import me.kuwg.re.ast.nodes.constants.StringNode;
import me.kuwg.re.ast.nodes.function.call.FunctionCallNode;
import me.kuwg.re.ast.types.interrupt.InterruptNode;
import me.kuwg.re.compiler.CompilationContext;

import java.util.List;

public class RaiseNode extends ASTNode implements InterruptNode {
    private final String log;

    public RaiseNode(final int line, final String message) {
        super(line);
        this.log = generateLog(message, line);
    }

    @Override
    public void compile(final CompilationContext cctx) {
        String catchLabel = cctx.popTryCatchScope();

        if (catchLabel == null) {
            new FunctionCallNode(line, "println", List.of(new StringNode(line, log))).compile(cctx);
            new FunctionCallNode(line, "exit", List.of(new NumberNode(line, "1"))).compile(cctx);
            cctx.emit("unreachable");
        } else {
            cctx.emit("br label %" + catchLabel);
        }
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Raise").append(NEWLINE);
    }

    private static String generateLog(String message, int line) {
        StringBuilder sb = new StringBuilder("An error occurred at line ").append(line);

        return message == null ? sb.append(".").toString() : sb.append(": ").append(message).toString();

    }
}
