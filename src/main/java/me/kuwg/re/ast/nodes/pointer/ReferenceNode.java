package me.kuwg.re.ast.nodes.pointer;

import me.kuwg.re.ast.nodes.variable.VariableReference;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.error.errors.deref.RNotAddressableError;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.error.errors.variable.RVariableNotFoundError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.ptr.PointerType;

import java.util.Map;

public class ReferenceNode extends ValueNode {
    private final VariableReference value;

    public ReferenceNode(final int line, final VariableReference value) {
        super(line);
        this.value = value;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        value.replaceGenerics(generics, cctx);
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        RVariable var = value.getVariable(cctx);

        if (var == null) {
            return new RVariableNotFoundError(value.getCompleteName(), line).raise();
        }

        if (var.addrReg() == null) {
            return new RNotAddressableError(
                    "Cannot take reference of non-addressable value: " + value.getCompleteName(),
                    line
            ).raise();
        }

        TypeRef type = var.type();

        String loadedValue = cctx.nextRegister();
        cctx.emit(loadedValue + " = load " +
                type.getLLVMName() + ", " +
                type.getLLVMName() + "* " + var.addrReg());

        long size = type.getSize();

        String heapMem = cctx.nextRegister();
        cctx.emit(heapMem + " = call i8* @malloc(i64 " + size + ")");

        String typedPtr = cctx.nextRegister();
        cctx.emit(typedPtr + " = bitcast i8* " +
                heapMem + " to " +
                type.getLLVMName() + "*");

        cctx.emit("store " +
                type.getLLVMName() + " " +
                loadedValue + ", " +
                type.getLLVMName() + "* " +
                typedPtr);

        setType(new PointerType(type));

        return typedPtr;
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

    @Override
    public ReferenceNode clone() {
        return new ReferenceNode(line, value.clone());
    }
}
