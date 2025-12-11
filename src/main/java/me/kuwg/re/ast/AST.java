package me.kuwg.re.ast;

import me.kuwg.re.ast.nodes.blocks.ASTBlockNode;
import me.kuwg.re.compiler.Compilable;
import me.kuwg.re.compiler.CompilationContext;

public class AST implements Compilable {
    private final ASTBlockNode block;

    public AST() {
        this.block = new ASTBlockNode();
    }

    public void addChild(ASTNode node) {
        block.addChild(node);
    }

    @Override
    public void compile(final CompilationContext context) {
        block.compile(context);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        block.write(sb, "");
        return sb.toString();
    }
}
