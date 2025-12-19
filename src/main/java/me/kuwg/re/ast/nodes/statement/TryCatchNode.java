package me.kuwg.re.ast.nodes.statement;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.blocks.BlockNode;
import me.kuwg.re.ast.nodes.blocks.IBlockContainer;
import me.kuwg.re.compiler.CompilationContext;

import java.util.ArrayList;
import java.util.List;

public class TryCatchNode extends ASTNode implements IBlockContainer {
    private final BlockNode tryBlock;
    private final BlockNode catchBlock;

    public TryCatchNode(final int line, final BlockNode tryBlock, final BlockNode catchBlock) {
        super(line);
        this.tryBlock = tryBlock;
        this.catchBlock = catchBlock;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        String catchLabel = cctx.nextLabel("catch_label");
        cctx.pushTryCatchScope(catchLabel);

        tryBlock.compile(cctx);
        cctx.emit(catchLabel + ":");
        catchBlock.compile(cctx);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Try:").append(NEWLINE);
        tryBlock.write(sb, indent + TAB);

        sb.append(indent).append("Catch:").append(NEWLINE);
        catchBlock.write(sb, indent + TAB);
    }

    @Override
    public BlockNode getBlock() {
        List<ASTNode> nodes = new ArrayList<>();
        nodes.addAll(tryBlock.getNodes());
        nodes.addAll(catchBlock.getNodes());
        return new BlockNode(nodes);
    }
}
