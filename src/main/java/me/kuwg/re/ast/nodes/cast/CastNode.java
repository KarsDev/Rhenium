package me.kuwg.re.ast.nodes.cast;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.cast.CastManager;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.cast.RNotPrimitiveCastError;
import me.kuwg.re.type.TypeRef;

import java.util.Map;

public class CastNode extends ValueNode {
    private final ValueNode value;

    public CastNode(final String fileName, final int line, final TypeRef type, final ValueNode value) {
        super(fileName, line, type);
        this.value = value;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        this.type = replaceGenericType(this.type, generics, cctx);
        value.replaceGenerics(generics, cctx);
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        if (!type.isPrimitive()) {
            new RNotPrimitiveCastError(type, fileName, line);
        }

        return CastManager.executeCast(fileName, line, value, evalType(type, cctx, fileName, line), cctx);
    }

    @Override
    public void compile(final CompilationContext cctx) {
        compileAndGet(cctx);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Cast: ").append(NEWLINE);
        sb.append(indent).append(TAB).append("Type: ").append(type.getName()).append(NEWLINE);
        sb.append(indent).append(TAB).append("Value: ").append(NEWLINE);
        value.write(sb, indent + TAB + TAB);
    }

    @Override
    public CastNode clone() {
        return new CastNode(fileName, line, type, value.clone());
    }
}
