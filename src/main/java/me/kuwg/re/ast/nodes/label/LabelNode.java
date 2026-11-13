package me.kuwg.re.ast.nodes.label;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.blocks.BlockNode;
import me.kuwg.re.ast.nodes.blocks.IBlockContainer;
import me.kuwg.re.ast.types.interrupt.InterruptNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.label.LabelContext;
import me.kuwg.re.type.TypeRef;

import java.util.Map;

public class LabelNode extends ASTNode implements IBlockContainer {
    private final String name;
    private final BlockNode block;

    public LabelNode(final String fileName, final int line, final String name, final BlockNode block) {
        super(fileName, line);
        this.name = name;
        this.block = block;
    }

    @Override
    public BlockNode getBlock() {
        return block;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        block.replaceGenerics(generics, cctx);
    }

    @Override
    public void compile(final CompilationContext cctx) {
        cctx.pushScope();

        String endLabel = cctx.nextLabel(name + "_block_end");

        cctx.pushLabel(new LabelContext(name, endLabel));

        cctx.emit("; Named block: " + name);

        cctx.pushIndent();
        block.compile(cctx);
        cctx.popIndent();

        if (block.getNodes().isEmpty() ||
                !(block.getNodes().get(block.getNodes().size() - 1) instanceof InterruptNode)) {
            cctx.emit("br label %" + endLabel);
        }

        cctx.emit(endLabel + ":");

        cctx.popLabel();
        cctx.popScope();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Breakable Block:\n");
        block.write(sb, indent + "\t");
    }

    @Override
    public LabelNode clone() {
        return new LabelNode(fileName, line, name, block.clone());
    }
}
