package me.kuwg.re.ast.nodes.blocks;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.enumeration.EnumDeclarationNode;
import me.kuwg.re.ast.nodes.function.declaration.FunctionDeclarationNode;
import me.kuwg.re.ast.nodes.function.declaration.GenFunctionDeclarationNode;
import me.kuwg.re.ast.nodes.module.UsingNode;
import me.kuwg.re.ast.nodes.struct.StructDeclarationNode;
import me.kuwg.re.ast.nodes.struct.gen.GenStructDeclarationNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.RInternalError;
import me.kuwg.re.type.TypeRef;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ASTBlockNode extends ASTNode {
    private final List<ASTNode> nodes;

    public ASTBlockNode() {
        super(0);
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
        Iterator<ASTNode> iterator = nodes.iterator();
        while (iterator.hasNext()) {
            ASTNode node = iterator.next();
            if (node instanceof FunctionDeclarationNode fdn) {
                fdn.register(cctx);
            } else if (node instanceof GenFunctionDeclarationNode gfdn) {
                gfdn.register(cctx);
            } else if (node instanceof StructDeclarationNode sdn) {
                sdn.compile(cctx);
                iterator.remove();
            } else if (node instanceof GenStructDeclarationNode gsdn) {
                gsdn.compile(cctx);
                iterator.remove();
            } else if (node instanceof UsingNode un) {
                un.compile(cctx);
                iterator.remove();
            } else if (node instanceof EnumDeclarationNode edn) {
                edn.compile(cctx);
                iterator.remove();
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
