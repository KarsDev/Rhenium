package me.kuwg.re.ast.nodes.array;

import me.kuwg.re.ast.nodes.variable.VariableReference;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.cast.CastManager;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.error.errors.variable.RVariableTypeError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;
import me.kuwg.re.type.iterable.arr.ArrayType;
import me.kuwg.re.type.ptr.PointerType;
import me.kuwg.re.type.struct.StructType;

public class ArrayAccessNode extends VariableReference {
    private final ValueNode array;
    private final ValueNode index;

    public ArrayAccessNode(final int line, final ValueNode array, final ValueNode index) {
        super(line);
        this.array = array;
        this.index = index;
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Array Access:").append(NEWLINE).append(indent).append("\tArray: ").append(NEWLINE);
        array.write(sb, indent + "\t\t");
        sb.append(indent).append("\tIndex: ").append(NEWLINE);
        index.write(sb, indent + "\t\t");
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        String elemPtr = computeElementPointer(cctx);

        TypeRef elementType = getElementType();
        String llvmElemType = elementType.getLLVMName();

        String loadReg = cctx.nextRegister();
        cctx.emit(loadReg + " = load " + llvmElemType + ", " + llvmElemType + "* " + elemPtr);

        setType(elementType);
        return loadReg;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RValueMustBeUsedError("Array Access", line).raise();
    }

    @Override
    public RVariable getVariable(final CompilationContext cctx) {
        String elemPtr = computeElementPointer(cctx);

        TypeRef elementType = getElementType();

        setType(elementType);

        String valueReg;

        if (elementType instanceof StructType) {
            valueReg = elemPtr;
        } else {
            valueReg = cctx.nextRegister();
            cctx.emit(valueReg + " = load "
                    + elementType.getLLVMName() + ", "
                    + elementType.getLLVMName() + "* "
                    + elemPtr);
        }

        return new RVariable(
                getSimpleName(),
                true,
                true,
                elementType,
                elemPtr,
                valueReg
        );
    }

    private String computeElementPointer(final CompilationContext cctx) {
        String arrayAddr;
        if (array instanceof VariableReference) {
            RVariable arrVar = ((VariableReference) array).getVariable(cctx);
            if (arrVar == null) {
                return new RVariableTypeError("array or pointer", "null", line).raise();
            }
            if (arrVar.addrReg() == null) {
                return new RVariableTypeError("addressable array", "temporary value", line).raise();
            }

            arrayAddr = arrVar.addrReg();
        } else {
            arrayAddr = array.compileAndGet(cctx);
        }

        String indexReg = index.compileAndGet(cctx);

        if (!BuiltinTypes.INT.getType().isCompatibleWith(index.getType())) {
            return new RVariableTypeError("int", index.getType().getName(), line).raise();
        }

        TypeRef arrayType = array.getType();
        TypeRef elementType;
        boolean fixedArray = false;
        long fixedSize = 0;

        if (arrayType instanceof ArrayType arrType) {
            elementType = arrType.inner();
            fixedArray = arrType.size() != ArrayType.UNKNOWN_SIZE;
            fixedSize = arrType.size();
        } else if (arrayType instanceof PointerType ptrType) {
            elementType = ptrType.inner();
        } else {
            return new RVariableTypeError("array or pointer", arrayType.getName(), line).raise();
        }

        String index64Reg = indexReg;
        if (!index.getType().equals(BuiltinTypes.LONG.getType())) {
            index64Reg = CastManager.executeCast(line, indexReg, index.getType(), BuiltinTypes.LONG.getType(), cctx);
        }

        String llvmElemType = elementType.getLLVMName();
        String elemPtrReg = cctx.nextRegister();

        if (fixedArray) {
            String llvmArrType = "[" + fixedSize + " x " + llvmElemType + "]";
            cctx.emit(elemPtrReg + " = getelementptr inbounds " + llvmArrType + ", " + llvmArrType + "* " + arrayAddr + ", i32 0, i64 " + index64Reg);
        } else {
            cctx.emit(elemPtrReg + " = getelementptr " + llvmElemType + ", " + llvmElemType + "* " + arrayAddr + ", i64 " + index64Reg);
        }

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
}