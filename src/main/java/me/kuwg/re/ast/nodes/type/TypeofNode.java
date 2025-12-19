package me.kuwg.re.ast.nodes.type;

import me.kuwg.re.ast.nodes.constants.StringNode;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.type.builtin.BuiltinTypes;

public class TypeofNode extends ValueNode {
    private final ValueNode valueNode;
    private final boolean llvm;

    public TypeofNode(final int line, final ValueNode valueNode, final boolean llvm) {
        super(line, BuiltinTypes.STR.getType());
        this.valueNode = valueNode;
        this.llvm = llvm;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RValueMustBeUsedError("Typeof", line).raise();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append(llvm ? "Typeof(LLVM): " : "Typeof: ").append(NEWLINE);
        valueNode.write(sb, indent + TAB);
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        valueNode.compileAndGet(cctx);

        return new StringNode(line, llvm ? valueNode.getType().getLLVMName() : valueNode.getType().getName()).compileAndGet(cctx);
    }
}
