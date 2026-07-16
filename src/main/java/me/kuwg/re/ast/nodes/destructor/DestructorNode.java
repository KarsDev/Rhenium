package me.kuwg.re.ast.nodes.destructor;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.blocks.BlockNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.destructor.RDestructorOutOfScopeError;
import me.kuwg.re.type.TypeRef;

import java.util.Map;

public class DestructorNode extends ASTNode {
    private final BlockNode block;

    public DestructorNode(final String fileName, final int line, final BlockNode block) {
        super(fileName, line);
        this.block = block;
    }

    public BlockNode getBlock() {
        return block;
    }

    @Override
    public ASTNode clone() {
        return new RDestructorOutOfScopeError(fileName, line).raise();
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        new RDestructorOutOfScopeError(fileName, line).raise();
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RDestructorOutOfScopeError(fileName, line).raise();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        new RDestructorOutOfScopeError(fileName, line).raise();
    }
}
