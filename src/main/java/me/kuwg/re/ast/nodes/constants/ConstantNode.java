package me.kuwg.re.ast.nodes.constants;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.type.TypeRef;

import java.util.Map;

public abstract class ConstantNode extends ValueNode {
    protected ConstantNode(final int line, final TypeRef type) {
        super(line, type);
    }


    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics) {
    }

    public abstract String compileToConstant(final CompilationContext cctx);
}
