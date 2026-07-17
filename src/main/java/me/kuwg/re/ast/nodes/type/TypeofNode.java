package me.kuwg.re.ast.nodes.type;

import me.kuwg.re.ast.nodes.constants.StringNode;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;

import java.util.Map;

public class TypeofNode extends ValueNode {
    private final ValueNode valueNode;

    public TypeofNode(final String fileName, final int line, final ValueNode valueNode) {
        super(fileName, line, BuiltinTypes.STR.getType());
        this.valueNode = valueNode;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        valueNode.replaceGenerics(generics, cctx);
    }

    @Override
    public void compile(final CompilationContext cctx) {
        compileAndGet(cctx);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Typeof: ").append(NEWLINE);
        valueNode.write(sb, indent + TAB);
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        valueNode.compileAndGet(cctx);

        cctx.emit("; Typeof");
        return new StringNode(fileName, line, valueNode.getType().getName()).compileAndGet(cctx);
    }

    @Override
    public TypeofNode clone() {
        return new TypeofNode(fileName, line, valueNode.clone());
    }
}
