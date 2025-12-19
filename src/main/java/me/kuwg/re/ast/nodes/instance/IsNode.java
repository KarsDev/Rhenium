package me.kuwg.re.ast.nodes.instance;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;

public class IsNode extends ValueNode {
    private final ValueNode value;
    private final TypeRef isType;

    public IsNode(final int line, final ValueNode value, final TypeRef isType) {
        super(line, BuiltinTypes.BOOL.getType());

        this.value = value;
        this.isType = isType;
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        value.compileAndGet(cctx);

        return Boolean.toString(isType.isCompatibleWith(value.getType()));
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RValueMustBeUsedError("is instance", line).raise();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Is:").append(NEWLINE).append(indent).append(TAB).append("Value:").append(NEWLINE);
        value.write(sb, indent + TAB + TAB);
        sb.append(indent).append(TAB).append("Type: ").append(isType.getName());
    }
}
