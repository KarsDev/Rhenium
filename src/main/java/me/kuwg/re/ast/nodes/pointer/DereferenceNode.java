package me.kuwg.re.ast.nodes.pointer;

import me.kuwg.re.ast.nodes.variable.VariableReference;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.error.errors.deref.RDerefAnyPointerError;
import me.kuwg.re.error.errors.deref.RDerefNotPointerError;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.error.errors.variable.RVariableNotFoundError;
import me.kuwg.re.type.builtin.AnyPointerType;
import me.kuwg.re.type.ptr.PointerType;

public class DereferenceNode extends VariableReference {
    public final VariableReference value;

    public DereferenceNode(final int line, final VariableReference value) {
        super(line);
        this.value = value;
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        RVariable var = value.getVariable(cctx);
        if (var == null) {
            return new RVariableNotFoundError(value.getCompleteName(), line).raise();
        }

        if (!(var.type() instanceof PointerType ptr)) {
            return new RDerefNotPointerError(value.getCompleteName(), line).raise();
        }

        cctx.emit(" ; Pointer dereference");
        setType(ptr.inner());

        String ptrReg = cctx.nextRegister();
        cctx.emit(ptrReg + " = load " + ptr.getLLVMName() + ", " + ptr.getLLVMName() + "* " + var.valueReg() + " ; load pointer " + value.getCompleteName());

        String destReg = cctx.nextRegister();
        cctx.emit(destReg + " = load " + ptr.inner().getLLVMName() + ", " + ptr.inner().getLLVMName() + "* " + ptrReg + " ; dereference " + value.getCompleteName());

        return destReg;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RValueMustBeUsedError("Dereference", line).raise();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Dereference: ").append(NEWLINE);
        value.write(sb, indent + TAB);
    }

    @Override
    public RVariable getVariable(final CompilationContext cctx) {
        RVariable var = value.getVariable(cctx);
        if (var == null) return new RVariableNotFoundError(value.getCompleteName(), line).raise();

        if (!(var.type().isPointer())) return new RDerefNotPointerError(value.getCompleteName(), line).raise();

        if (var.type() instanceof AnyPointerType) return new RDerefAnyPointerError(line).raise();

        PointerType ptr = (PointerType) var.type();

        String ptrReg = cctx.nextRegister();
        cctx.emit(ptrReg + " = load " + ptr.getLLVMName() + ", " + ptr.getLLVMName() + "* " + var.valueReg());

        return new RVariable(value.getSimpleName(), true, ptr.inner(), ptrReg);
    }

    @Override
    public String getCompleteName() {
        return value.getCompleteName();
    }

    @Override
    public String getSimpleName() {
        return value.getSimpleName();
    }
}
