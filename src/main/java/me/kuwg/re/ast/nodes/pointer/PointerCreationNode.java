package me.kuwg.re.ast.nodes.pointer;

import me.kuwg.re.ast.nodes.cast.CastNode;
import me.kuwg.re.ast.nodes.constants.ConstantNode;
import me.kuwg.re.ast.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;
import me.kuwg.re.type.builtin.LongBuiltinType;

public class PointerCreationNode extends ValueNode {
    private final ConstantNode value;

    public PointerCreationNode(final int line, final ConstantNode value) {
        super(line, BuiltinTypes.ANYPTR.getType());
        this.value = value;
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        TypeRef type = value.getType();

        String constant;

        if (type instanceof LongBuiltinType) constant = value.compileToConstant(cctx);
        else constant = new CastNode(line, BuiltinTypes.LONG.getType(), value).compileAndGet(cctx);

        String result = cctx.nextRegister();
        cctx.emit(result + " = inttoptr i64 " + constant + " to i8*");

        setType(BuiltinTypes.ANYPTR.getType());
        return result;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RValueMustBeUsedError("Pointer Creation", line).raise();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Pointer Creation: ").append(NEWLINE);
        value.write(sb, indent + TAB);
    }
}
