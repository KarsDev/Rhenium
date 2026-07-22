package me.kuwg.re.ast.nodes.pointer;

import me.kuwg.re.ast.nodes.variable.VariableReference;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.error.errors.deref.RDerefNotPointerError;
import me.kuwg.re.error.errors.variable.RVariableNotFoundError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.ptr.PointerType;
import me.kuwg.re.type.struct.StructType;

import java.util.Map;

public class DereferenceNode extends VariableReference {
    public final VariableReference value;

    public DereferenceNode(final String fileName, final int line, final VariableReference value) {
        super(fileName, line);
        this.value = value;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        value.replaceGenerics(generics, cctx);
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        RVariable var = value.getVariable(cctx);
        if (var == null)
            return new RVariableNotFoundError(value.getCompleteName(), fileName, line).raise();

        if (!(var.type() instanceof PointerType ptr))
            return new RDerefNotPointerError(value.getCompleteName(), fileName, line).raise();

        ptr = evalType(ptr, cctx, fileName, line);

        setType(ptr.getInner());

        cctx.emit(" ; Pointer dereference");

        String ptrValueReg;

        ptrValueReg = cctx.nextRegister();
        cctx.emit(ptrValueReg + " = load "
                + ptr.getLLVMName() + ", "
                + toPtr(ptr.getLLVMName())
                + var.addrReg());

        String destReg = cctx.nextRegister();
        cctx.emit(destReg + " = load "
                + ptr.getInner().getLLVMName() + ", "
                + toPtr(ptr.getInner().getLLVMName())
                + ptrValueReg);

        return destReg;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        compileAndGet(cctx);
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
            return new RVariableNotFoundError(value.getCompleteName(), fileName, line).raise();

        if (!(var.type() instanceof PointerType ptr))
            return new RDerefNotPointerError(value.getCompleteName(), fileName, line).raise();

        ptr = evalType(ptr, cctx, fileName, line);

        String ptrValueReg;

        ptrValueReg = cctx.nextRegister();
        cctx.emit(ptrValueReg + " = load "
                + ptr.getLLVMName() + ", "
                + toPtr(ptr.getLLVMName())
                + var.addrReg());

        String valueReg;

        if (ptr.getInner() instanceof StructType) {
            valueReg = ptrValueReg;
        } else {
            valueReg = cctx.nextRegister();
            cctx.emit(valueReg + " = load "
                    + ptr.getInner().getLLVMName() + ", "
                    + toPtr(ptr.getInner().getLLVMName())
                    + ptrValueReg);
        }

        setType(ptr.getInner());

        return new RVariable(
                value.getSimpleName(),
                true,
                true,
                ptr.getInner(),
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

    @Override
    public DereferenceNode clone() {
        return new DereferenceNode(fileName, line, value.clone());
    }
}
