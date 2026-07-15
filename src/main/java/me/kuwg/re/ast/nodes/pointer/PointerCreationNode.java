package me.kuwg.re.ast.nodes.pointer;

import me.kuwg.re.ast.nodes.cast.CastNode;
import me.kuwg.re.ast.nodes.constants.NumberNode;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;
import me.kuwg.re.type.builtin.LongBuiltinType;

import java.util.Map;

public class PointerCreationNode extends ValueNode {
    private final NumberNode value;

    public PointerCreationNode(final String fileName, final int line, final NumberNode value) {
        super(fileName, line, BuiltinTypes.ANYPTR.getType());
        this.value = value;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        value.replaceGenerics(generics, cctx);
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        TypeRef type = value.getType();

        String constant;

        if (type instanceof LongBuiltinType) constant = value.compileToConstant(cctx);
        else constant = new CastNode(fileName, line, BuiltinTypes.LONG.getType(), value).compileAndGet(cctx);

        String result = cctx.nextRegister();
        cctx.emit("; Pointer creation");
        cctx.emit(result + " = inttoptr i64 " + constant + " to i8*");

        setType(BuiltinTypes.ANYPTR.getType());
        return result;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        compileAndGet(cctx);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Pointer Creation: ").append(NEWLINE);
        value.write(sb, indent + TAB);
    }

    @Override
    public PointerCreationNode clone() {
        return new PointerCreationNode(fileName, line, (NumberNode) value.clone());
    }
}
