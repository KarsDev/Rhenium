package me.kuwg.re.ast.nodes.constants;

import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.type.ptr.NullType;

public class NullNode extends ConstantNode {
    public NullNode(final String fileName, final int line) {
        super(fileName, line, NullType.INSTANCE);
    }

    @Override
    public String compileToConstant(final CompilationContext cctx) {
        return "null";
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        return compileToConstant(cctx);
    }

    @Override
    public void compile(final CompilationContext cctx) {
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Null").append(NEWLINE);
    }
}
