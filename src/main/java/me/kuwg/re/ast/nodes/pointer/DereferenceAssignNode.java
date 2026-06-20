package me.kuwg.re.ast.nodes.pointer;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.ptr.PointerType;

import java.util.Map;

public class DereferenceAssignNode extends ValueNode {
    private final ValueNode pointer;
    private final ValueNode value;

    public DereferenceAssignNode(final String fileName, final int line, final ValueNode pointer, final ValueNode value) {
        super(fileName, line);
        this.pointer = pointer;
        this.value = value;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        pointer.replaceGenerics(generics, cctx);
        value.replaceGenerics(generics, cctx);
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        compile(cctx);
        return value.compileAndGet(cctx);
    }

    @Override
    public void compile(final CompilationContext cctx) {
        String ptrReg = pointer.compileAndGet(cctx);
        String valueReg = value.compileAndGet(cctx);

        PointerType ptrType = (PointerType) pointer.getType();
        String innerLLVM = ptrType.inner().getLLVMName();

        cctx.emit("; Dereference assign");
        cctx.emit("store " + innerLLVM + " " + valueReg + ", " + innerLLVM + "* " + ptrReg + " ; dereference assign");

        setType(ptrType.inner());
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Dereference Assign: ").append(NEWLINE).append(indent).append(TAB).append("Pointer: ").append(NEWLINE);
        pointer.write(sb, indent + TAB + TAB);
        sb.append(indent).append(TAB).append("Value: ").append(NEWLINE);
        value.write(sb, indent + TAB + TAB);
    }

    @Override
    public DereferenceAssignNode clone() {
        return new DereferenceAssignNode(fileName, line, pointer.clone(), value.clone());
    }
}
