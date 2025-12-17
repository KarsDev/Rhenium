package me.kuwg.re.ast.nodes.module;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.compiler.CompilationContext;

public class UsingNode extends ASTNode {
    private final String sourceFile;
    private final String name;
    private final String pkg;

    public UsingNode(final int line, final String sourceFile, final String name, final String pkg) {
        super(line);
        this.sourceFile = sourceFile;
        this.name = name.toLowerCase();
        this.pkg = pkg;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        cctx.include(line, sourceFile, name, pkg);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Using: ").append(name).append(NEWLINE);
    }
}
