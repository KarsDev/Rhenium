package me.kuwg.re.ast.nodes.blocks;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.interrupt.InterruptNode;
import me.kuwg.re.ast.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.type.TypeRef;

public class ReturnNode extends ASTNode implements InterruptNode {
    private final ValueNode value;
    public ReturnNode(final int line, final ValueNode value) {
        super(line);
        this.value = value;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        if (value == null) {
            cctx.emit("ret void ; return statement");
        } else {
            String valueReg = value.compileAndGet(cctx);
            String llvmType = value.getType().getLLVMName();
            cctx.emit("ret " + llvmType + " " + valueReg + " ; return statement");
        }
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Return: ").append(NEWLINE);
        if (value != null) {
            value.write(sb, indent + TAB);
        }
    }

    public TypeRef getValueType() {
        return value.getType();
    }
}
