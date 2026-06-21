package me.kuwg.re.ast.nodes.array;

import me.kuwg.re.ast.nodes.variable.VariableReference;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.cast.CastManager;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.error.errors.variable.RVariableNotFoundError;
import me.kuwg.re.error.errors.variable.RVariableTypeError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;
import me.kuwg.re.type.iterable.arr.ArrayType;
import me.kuwg.re.type.ptr.PointerType;

import java.util.Map;

public class ArrayAccessNode extends VariableReference {
    private final ValueNode array;
    private final ValueNode index;

    public ArrayAccessNode(final String fileName, final int line, final ValueNode array, final ValueNode index) {
        super(fileName, line);
        this.array = array;
        this.index = index;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        array.replaceGenerics(generics, cctx);
        index.replaceGenerics(generics, cctx);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Array Access:").append(NEWLINE).append(indent).append(TAB).append("Array: ").append(NEWLINE);
        array.write(sb, indent + TAB + TAB);
        sb.append(indent).append(TAB).append("Index: ").append(NEWLINE);
        index.write(sb, indent + TAB + TAB);
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        String elemPtr = computeElementPointer(cctx);
        TypeRef elementType = getElementType();

        String loadReg = cctx.nextRegister();
        cctx.emit("; Array access");
        cctx.emit(loadReg + " = load " + elementType.getLLVMName() + ", " + toPtr(elementType.getLLVMName()) + " " + elemPtr);

        setType(elementType);
        return loadReg;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RValueMustBeUsedError("Array Access", fileName, line).raise();
    }

    @Override
    public RVariable getVariable(final CompilationContext cctx) {
        String elemPtr = computeElementPointer(cctx);
        TypeRef elementType = getElementType();
        setType(elementType);

        String valueReg = cctx.nextRegister();
        cctx.emit(valueReg + " = load " + elementType.getLLVMName() + ", " + toPtr(elementType.getLLVMName()) + " " + elemPtr);

        return new RVariable(getSimpleName(), true, true, elementType, elemPtr, valueReg);
    }

    private String computeElementPointer(final CompilationContext cctx) {
        String arrayPtr;

        if (array instanceof VariableReference vr) {
            RVariable arrVar = vr.getVariable(cctx);
            if (arrVar == null) {
                return new RVariableNotFoundError(vr.getCompleteName(), fileName, line).raise();
            }

            arrayPtr = arrVar.valueReg();
        } else {
            arrayPtr = array.compileAndGet(cctx);
        }

        String indexReg = index.compileAndGet(cctx);

        if (!BuiltinTypes.INT.getType().isCompatibleWith(index.getType())) {
            return new RVariableTypeError("int", index.getType().getName(), fileName, line).raise();
        }

        TypeRef arrayType = array.getType();
        TypeRef elementType;

        if (arrayType instanceof ArrayType arrType) {
            elementType = arrType.inner();
        } else if (arrayType instanceof PointerType ptrType) {
            elementType = ptrType.inner();
        } else if (arrayType.equals(BuiltinTypes.STR.getType())) {
            elementType = BuiltinTypes.CHAR.getType();
        } else {
            return new RVariableTypeError("array or pointer", arrayType.getName(), fileName, line).raise();
        }

        String index64Reg = indexReg;
        if (!index.getType().equals(BuiltinTypes.LONG.getType())) {
            index64Reg = CastManager.executeCast(fileName, line, indexReg, index.getType(), BuiltinTypes.LONG.getType(), cctx);
        }

        String elemPtrReg = cctx.nextRegister();
        String llvmElemType = elementType.getLLVMName();

        cctx.emit(elemPtrReg + " = getelementptr " + llvmElemType + ", " + llvmElemType + "* " + arrayPtr + ", i64 " + index64Reg);
        return elemPtrReg;
    }

    private TypeRef getElementType() {
        TypeRef arrayType = array.getType();

        if (arrayType instanceof ArrayType arrType) {
            return arrType.inner();
        }

        if (arrayType instanceof PointerType ptrType) {
            return ptrType.inner();
        }

        if (arrayType.equals(BuiltinTypes.STR.getType())) {
            return BuiltinTypes.CHAR.getType();
        }

        throw new IllegalStateException("Invalid array access type");
    }

    @Override
    public String getCompleteName() {
        return "Array Access";
    }

    @Override
    public String getSimpleName() {
        return "Array Access";
    }

    @Override
    public ArrayAccessNode clone() {
        return new ArrayAccessNode(fileName, line, array.clone(), index.clone());
    }
}