package me.kuwg.re.ast.nodes.pointer;

import me.kuwg.re.ast.nodes.variable.VariableReference;
import me.kuwg.re.ast.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.error.errors.variable.RVariableNotFoundError;
import me.kuwg.re.type.ptr.PointerType;

public class ReferenceNode extends ValueNode {
    private final VariableReference value;

    public ReferenceNode(final int line, final VariableReference value) {
        super(line);
        this.value = value;
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        RVariable var = value.getVariable(cctx);
        if (var == null) {
            return new RVariableNotFoundError(value.getCompleteName(), line).raise();
        }

        cctx.emit(" ; Pointer address of");
        setType(new PointerType(var.type()));
        return var.valueReg();
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RValueMustBeUsedError("Pointer", line).raise();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Reference: ").append(NEWLINE);
        value.write(sb, indent + TAB);
    }
}
