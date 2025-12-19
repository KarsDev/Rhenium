package me.kuwg.re.ast.nodes.ir;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.types.global.GlobalNode;
import me.kuwg.re.compiler.CompilationContext;

public class IRDeclarationNode extends ASTNode implements GlobalNode {
    private final String content;

    public IRDeclarationNode(final int line, final String content) {
        super(line);
        this.content = content;
    }


    @Override
    public void compile(final CompilationContext cctx) {
        cctx.addIR("; IR Declaration\n" + content);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("IR Declaration: ").append(NEWLINE);
        content.lines().forEach(l -> sb.append(indent).append(TAB).append(l).append(NEWLINE));
    }
}
