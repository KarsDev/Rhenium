package me.kuwg.re.ast.nodes.pointer;

import me.kuwg.re.ast.nodes.variable.VariableReference;
import me.kuwg.re.ast.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.error.errors.deref.RDerefNotPointerError;
import me.kuwg.re.error.errors.variable.RVariableNotFoundError;
import me.kuwg.re.error.errors.variable.RVariableTypeError;
import me.kuwg.re.type.builtin.AnyPointerType;
import me.kuwg.re.type.ptr.PointerType;

public class DereferenceAssignNode extends ValueNode {
    private final ValueNode pointer;
    private final ValueNode value;

    public DereferenceAssignNode(final int line, final ValueNode pointer, final ValueNode value) {
        super(line);
        this.pointer = pointer;
        this.value = value;
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        compile(cctx);
        return value.compileAndGet(cctx);
    }

    @Override
    public void compile(final CompilationContext cctx) {
        if (!(pointer instanceof VariableReference vr)) {
            new RVariableNotFoundError("(non-variable pointer)", line).raise();
            return;
        }

        RVariable var = vr.getVariable(cctx);
        if (var == null) {
            new RVariableNotFoundError(vr.getCompleteName(), line).raise();
            return;
        }

        if (!(var.type() .isPointer())) {
            new RDerefNotPointerError(vr.getCompleteName(), line).raise();
            return;
        }

        String valueReg = value.compileAndGet(cctx);

        if (var.type() instanceof AnyPointerType) {
            cctx.emit(" ; Dereference assign (anyptr)");

            String ptrReg = cctx.nextRegister();
            cctx.emit(ptrReg + " = load i8*, i8** " + var.valueReg() + " ; load anyptr");

            String castedValueReg = valueReg;
            if (!value.getType().getLLVMName().equals("i8*")) {
                castedValueReg = cctx.nextRegister();
                if (value.getType().getLLVMName().startsWith("i")) {
                    cctx.emit(castedValueReg + " = inttoptr " + value.getType().getLLVMName() + " " + valueReg + " to i8*");
                } else {
                    cctx.emit(castedValueReg + " = bitcast " + value.getType().getLLVMName() + " " + valueReg + " to i8*");
                }
            }

            cctx.emit("store i8* " + castedValueReg + ", i8** " + ptrReg + " ; dereference assign to anyptr");
            setType(value.getType());
            return;
        }


        PointerType ptr = (PointerType) var.type();



        if (!ptr.inner().isCompatibleWith(value.getType())) {
            new RVariableTypeError(value.getType().getName(), ptr.inner().getName(), line).raise();
            return;
        }

        cctx.emit(" ; Dereference assign");

        String ptrReg = cctx.nextRegister();
        cctx.emit(ptrReg + " = load " + ptr.getLLVMName() + ", "
                + ptr.getLLVMName() + "* " + var.valueReg()
                + " ; load pointer " + vr.getCompleteName());

        String innerTypeLLVM = ptr.inner().getLLVMName();
        cctx.emit("store " + innerTypeLLVM + " " + valueReg + ", "
                + innerTypeLLVM + "* " + ptrReg
                + " ; dereference assign to " + vr.getCompleteName());

        setType(ptr.inner());
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Dereference Assign: ").append(NEWLINE)
                .append(indent).append(TAB).append("Pointer: ").append(NEWLINE);
        pointer.write(sb, indent + TAB + TAB);
        sb.append(indent).append(TAB).append("Value: ").append(NEWLINE);
        value.write(sb, indent + TAB + TAB);
    }
}
