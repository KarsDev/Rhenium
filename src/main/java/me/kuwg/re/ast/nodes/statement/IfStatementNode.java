package me.kuwg.re.ast.nodes.statement;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.blocks.BlockNode;
import me.kuwg.re.ast.nodes.blocks.IBlockContainer;
import me.kuwg.re.ast.types.interrupt.InterruptNode;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.condition.RInvalidConditionError;
import me.kuwg.re.type.builtin.BoolBuiltinType;

import java.util.Objects;

public class IfStatementNode extends ASTNode implements IBlockContainer {
    private final ValueNode condition;
    private final BlockNode block;
    private final IfStatementNode elseIfNode;
    private final BlockNode elseNode;

    public IfStatementNode(final int line, final ValueNode condition, final BlockNode block, final IfStatementNode elseIfNode, final BlockNode elseNode) {
        super(line);
        this.condition = condition;
        this.block = block;
        this.elseIfNode = elseIfNode;
        this.elseNode = elseNode;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        String ifLabel = cctx.nextLabel("if_block");
        String endLabel = cctx.nextLabel("if_end");
        String elseLabel = null;

        if (elseIfNode != null) {
            elseLabel = cctx.nextLabel("else_if");
        } else if (elseNode != null) {
            elseLabel = cctx.nextLabel("else");
        }

        String condReg = condition.compileAndGet(cctx);

        if (!(condition.getType() instanceof BoolBuiltinType)) {
            new RInvalidConditionError(condition.getType(), line).raise();
            return;
        }

        cctx.emit("br i1 " + condReg + ", label %" + ifLabel + ", label %" + Objects.requireNonNullElse(elseLabel, endLabel));

        cctx.emit(ifLabel + ":");
        cctx.pushIndent();
        block.compile(cctx);
        cctx.popIndent();

        block.compile(cctx);
        if (block.getNodes().isEmpty() || !(block.getNodes().get(block.getNodes().size() - 1) instanceof InterruptNode)) {
            cctx.emit("br label %" + endLabel);
        }

        if (elseIfNode != null) {
            cctx.emit(elseLabel + ":");
            elseIfNode.compile(cctx);

            BlockNode lastBlock = elseIfNode.getBlock();
            if (lastBlock != null && !lastBlock.getNodes().isEmpty()) {
                if (!(lastBlock.getNodes().get(lastBlock.getNodes().size() - 1) instanceof InterruptNode)) {
                    cctx.emit("br label %" + endLabel);
                }
            }
        } else if (elseNode != null) {
            cctx.emit(elseLabel + ":");
            cctx.pushIndent();
            elseNode.compile(cctx);
            cctx.popIndent();
            if (!elseNode.getNodes().isEmpty()
                    && !(elseNode.getNodes().get(elseNode.getNodes().size() - 1) instanceof InterruptNode)) {
                cctx.emit("br label %" + endLabel);
            }
        }

        cctx.emit(endLabel + ":");
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {

    }

    @Override
    public BlockNode getBlock() {
        return block;
    }
}
