package me.kuwg.re.ast.nodes.instance;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;

import java.util.Map;

public class IsNode extends ValueNode {
    private final ValueNode value;
    private TypeRef isType;

    public IsNode(final String fileName, final int line, final ValueNode value, final TypeRef isType) {
        super(fileName, line, BuiltinTypes.BOOL.getType());

        this.value = value;
        this.isType = isType;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        value.replaceGenerics(generics, cctx);
        isType = replaceGenericType(isType, generics, cctx);
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        value.compileAndGet(cctx);
        cctx.emit("; Instance checking");
        return Boolean.toString(isType.isCompatibleWith(value.getType()));
    }

    @Override
    public void compile(final CompilationContext cctx) {
        compileAndGet(cctx);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Is:").append(NEWLINE).append(indent).append(TAB).append("Value:").append(NEWLINE);
        value.write(sb, indent + TAB + TAB);
        sb.append(indent).append(TAB).append("Type: ").append(isType.getName());
    }

    @Override
    public IsNode clone() {
        return new IsNode(fileName, line, value.clone(), isType);
    }
}
