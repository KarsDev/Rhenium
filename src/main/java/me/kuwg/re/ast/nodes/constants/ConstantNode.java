package me.kuwg.re.ast.nodes.constants;

import me.kuwg.re.ast.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.type.TypeRef;

public abstract class ConstantNode extends ValueNode {
    protected ConstantNode(final int line, final TypeRef type) {
        super(line, type);
    }

    public abstract String compileToConstant(final CompilationContext cctx);
}
