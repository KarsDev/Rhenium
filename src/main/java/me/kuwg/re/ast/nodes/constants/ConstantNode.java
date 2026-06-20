package me.kuwg.re.ast.nodes.constants;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.type.TypeRef;

import java.util.Map;

abstract class ConstantNode extends ValueNode {
    protected ConstantNode(final int line, final TypeRef type) {
        super(line, type);
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
    }

    @Override
    public abstract String compileToConstant(final CompilationContext cctx);

    @Override
    public final boolean isConstant(final CompilationContext cctx) {
        return true;
    }

    @Override
    public ConstantNode clone() {
        return this;
    }
}
