package me.kuwg.re.ast.nodes.blocks;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.module.UsingNode;
import me.kuwg.re.ast.nodes.struct.StructDeclarationNode;
import me.kuwg.re.ast.nodes.struct.gen.GenStructDeclarationNode;
import me.kuwg.re.ast.types.load.TopLevelNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.RInternalError;
import me.kuwg.re.type.TypeRef;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ASTBlockNode extends ASTNode {
    private final List<ASTNode> nodes;

    public ASTBlockNode(final String fileName) {
        super(fileName, 0);
        this.nodes = new ArrayList<>();
    }

    public void addChild(ASTNode node) {
        nodes.add(node);
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        nodes.forEach(n -> n.replaceGenerics(generics, cctx));
    }

    @Override
    public void compile(final CompilationContext cctx) {
        nodes.stream().filter(node -> node instanceof TopLevelNode)
                .map(node -> (TopLevelNode) node).forEach(top -> top.load(cctx));



        Iterator<ASTNode> beta = nodes.iterator();
        while (beta.hasNext()) {
            ASTNode node = beta.next();
            if (node instanceof UsingNode using) {
                using.compile(cctx);
                beta.remove();
            }
        }

        Iterator<ASTNode> alpha = nodes.iterator();
        while (alpha.hasNext()) {
            ASTNode node = alpha.next();
            if (node instanceof StructDeclarationNode || node instanceof GenStructDeclarationNode) {
                node.compile(cctx);
                alpha.remove();
            }
        }

        for (ASTNode node : nodes) {
            node.compile(cctx);
        }
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("AST: ").append(NEWLINE);
        nodes.forEach(node -> node.write(sb, TAB + indent));
    }

    @Override
    public ASTNode clone() {
        throw new RInternalError();
    }
}
