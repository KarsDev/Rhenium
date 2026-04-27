package me.kuwg.re.ast.nodes.pointer;

import me.kuwg.re.ast.nodes.variable.VariableReference;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.error.errors.deref.RDerefNotPointerError;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.error.errors.variable.RVariableNotFoundError;
import me.kuwg.re.type.ptr.PointerType;
import me.kuwg.re.type.struct.StructType;

public class DereferenceNode extends VariableReference {
    public final VariableReference value;

    public DereferenceNode(final int line, final VariableReference value) {
        super(line);
        this.value = value;
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        RVariable var = value.getVariable(cctx);
        if (var == null)
            return new RVariableNotFoundError(value.getCompleteName(), line).raise();

        if (!(var.type() instanceof PointerType ptr))
            return new RDerefNotPointerError(value.getCompleteName(), line).raise();

        setType(ptr.inner());

        cctx.emit(" ; Pointer dereference");

        String ptrValueReg;

        if (var.addrReg() != null) {
            ptrValueReg = cctx.nextRegister();
            cctx.emit(ptrValueReg + " = load "
                    + ptr.getLLVMName() + ", "
                    + ptr.getLLVMName() + "* "
                    + var.addrReg());
        } else {
            ptrValueReg = var.valueReg();
        }

        if (ptr.inner() instanceof StructType) {
            return ptrValueReg;
        }

        String destReg = cctx.nextRegister();
        cctx.emit(destReg + " = load "
                + ptr.inner().getLLVMName() + ", "
                + ptr.inner().getLLVMName() + "* "
                + ptrValueReg);

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
        if (var == null)
            return new RVariableNotFoundError(value.getCompleteName(), line).raise();

        if (!(var.type() instanceof PointerType ptr))
            return new RDerefNotPointerError(value.getCompleteName(), line).raise();

        String ptrValueReg;

        if (var.addrReg() != null) {
            ptrValueReg = cctx.nextRegister();
            cctx.emit(ptrValueReg + " = load "
                    + ptr.getLLVMName() + ", "
                    + ptr.getLLVMName() + "* "
                    + var.addrReg());
        } else {
            ptrValueReg = var.valueReg();
        }

        String valueReg;

        if (ptr.inner() instanceof StructType) {
            valueReg = ptrValueReg;
        } else {
            valueReg = cctx.nextRegister();
            cctx.emit(valueReg + " = load "
                    + ptr.inner().getLLVMName() + ", "
                    + ptr.inner().getLLVMName() + "* "
                    + ptrValueReg);
        }

        setType(ptr.inner());

        return new RVariable(
                value.getSimpleName(),
                true,
                true,
                ptr.inner(),
                ptrValueReg,
                valueReg
        );
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
