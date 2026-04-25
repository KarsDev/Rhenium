package me.kuwg.re.ast.nodes.function.declaration;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.blocks.BlockNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.parser.RParserError;

import java.util.List;

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
    public void compile(final CompilationContext cctx) {
        new RParserError("Constructor declaration out of its scope", fileName, line).raise();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        new RParserError("Constructor declaration out of its scope", fileName, line).raise();
    }

    public BlockNode getBlock() {
        return block;
    }

    public List<FunctionParameter> getParameters() {
        return parameters;
    }
}
