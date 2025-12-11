package me.kuwg.re.ast.nodes.loop;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.blocks.BlockNode;
import me.kuwg.re.ast.nodes.blocks.IBlockContainer;
import me.kuwg.re.ast.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.LoopContext;

public class WhileNode extends ASTNode implements IBlockContainer {
    private final ValueNode condition;
    private final BlockNode block;

    public WhileNode(final int line, final ValueNode condition, final BlockNode block) {
        super(line);
        this.condition = condition;
        this.block = block;
    }

    @Override
    public BlockNode getBlock() {
        return block;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        cctx.pushScope();

        String startLabel = cctx.nextLabel("while_start");
        String bodyLabel = cctx.nextLabel("while_body");
        String endLabel = cctx.nextLabel("while_end");

        cctx.getLoopStack().push(new LoopContext(startLabel, bodyLabel, endLabel));

        cctx.emit("br label %" + startLabel);

        cctx.emit(startLabel + ":");
        String condReg = condition.compileAndGet(cctx);
        cctx.emit("br i1 " + condReg + ", label %" + bodyLabel + ", label %" + endLabel);

        cctx.emit(bodyLabel + ":");
        cctx.pushIndent();
        block.compile(cctx);
        cctx.popIndent();

        cctx.emit("br label %" + startLabel);

        cctx.emit(endLabel + ":");

        cctx.getLoopStack().pop();
        cctx.popScope();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("While: ").append(NEWLINE)
                .append(indent).append(TAB).append("Condition: ").append(NEWLINE);
        condition.write(sb, indent + TAB + TAB);
        block.write(sb, indent + TAB);
    }
}
