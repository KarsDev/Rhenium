package me.kuwg.re.ast.nodes.namespace;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.blocks.BlockNode;
import me.kuwg.re.ast.types.global.GlobalNode;
import me.kuwg.re.ast.types.load.TopLevelNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.type.TypeRef;

import java.util.Map;

public class NamespaceDeclarationNode extends ASTNode implements GlobalNode, TopLevelNode {
    private final String name;
    private final BlockNode block;

    public NamespaceDeclarationNode(final String fileName, final int line, final String name, final BlockNode block) {
        super(fileName, line);
        this.name = name;
        this.block = block;
    }

    @Override
    public ASTNode clone() {
        return new NamespaceDeclarationNode(fileName, line, name, block.clone());
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        block.replaceGenerics(generics, cctx);
    }

    @Override
    public void compile(final CompilationContext cctx) {
        cctx.emit("; Namespace declaration");
        cctx.pushNamespace(name);
        block.compile(cctx);
        cctx.popNamespace();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Namespace Declaration:").append(NEWLINE)
                .append(indent).append(TAB).append("Name: ").append(name).append(NEWLINE)
                .append(indent).append(TAB).append("Block:").append(NEWLINE);
        block.write(sb, indent + TAB + TAB);
    }

    @Override
    public void load(final CompilationContext cctx) {
        cctx.pushNamespace(name);
        block.load(cctx);
        cctx.popNamespace();
    }
}
