package me.kuwg.re.ast.nodes.function.declaration;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.blocks.BlockNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.parser.RParserError;
import me.kuwg.re.type.TypeRef;

import java.util.List;
import java.util.Map;

public class ConstructorDeclarationNode extends ASTNode {
    private final String fileName;
    private final List<FunctionParameter> parameters;
    private final BlockNode block;

    public ConstructorDeclarationNode(final int line, final String fileName, final List<FunctionParameter> parameters, final BlockNode block) {
        super(line);
        this.fileName = fileName;
        this.parameters = parameters;
        this.block = block;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        block.replaceGenerics(generics, cctx);
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RParserError("Constructor declaration out of its scope", fileName, line).raise();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        new RParserError("Constructor declaration out of its scope", fileName, line).raise();
    }

    @Override
    public ConstructorDeclarationNode clone() {
        return new ConstructorDeclarationNode(line, fileName, parameters, block.clone());
    }

    public BlockNode getBlock() {
        return block;
    }

    public List<FunctionParameter> getParameters() {
        return parameters;
    }
}
