package me.kuwg.re.ast.nodes.namespace;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.type.TypeRef;

import java.util.Map;

public class NamespaceCallNode extends ValueNode {
    private final String name;
    private final ValueNode value;

    public NamespaceCallNode(final String fileName, final int line, final String name, final ValueNode value) {
        super(fileName, line);
        this.name = name;
        this.value = value;
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        cctx.emit("; Namespace call");
        cctx.pushNamespace(name);
        String v = value.compileAndGet(cctx);
        setType(value.getType());
        cctx.popNamespace();
        return v;
    }

    @Override
    public ValueNode clone() {
        return new NamespaceCallNode(fileName, line, name, value.clone());
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        value.replaceGenerics(generics, cctx);
    }

    @Override
    public void compile(final CompilationContext cctx) {
        cctx.emit("; Namespace call");
        cctx.pushNamespace(name);
        value.compile(cctx);
        setType(value.getType());
        cctx.popNamespace();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Namespace Call:").append(NEWLINE)
                .append(indent).append(TAB).append("Name: ").append(name).append(NEWLINE)
                .append(indent).append(TAB).append("Value:").append(NEWLINE);
        value.write(sb, indent + TAB + TAB);
    }
}
