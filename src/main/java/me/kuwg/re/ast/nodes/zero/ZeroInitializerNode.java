package me.kuwg.re.ast.nodes.zero;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.zero.RZeroInitializerTypeRequired;
import me.kuwg.re.type.TypeRef;

import java.util.Map;

public class ZeroInitializerNode extends ValueNode {
    public ZeroInitializerNode(final String fileName, final int line, final TypeRef type) {
        super(fileName, line);
        this.type = type;
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        if (type == null) return new RZeroInitializerTypeRequired(fileName, line).raise();
        return type.getZeroValue();
    }

    @Override
    public TypeRef getType() {
        return type;
    }

    @Override
    public ValueNode clone() {
        return this;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        if (type == null) return;
        type = replaceGenericType(type, generics, cctx);
    }

    @Override
    public void compile(final CompilationContext cctx) {
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Zero Initializer");
        if (type != null) sb.append(": ").append(type.getName());
        sb.append(NEWLINE);
    }
}
