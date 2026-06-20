package me.kuwg.re.ast.nodes.constants;

import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
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
        String reg = cctx.nextRegister();
        cctx.emit(reg + " = bitcast ptr null to ptr ; null literal");
        return reg;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RValueMustBeUsedError("Null", fileName, line).raise();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Null").append(NEWLINE);
    }
}
