package me.kuwg.re.ast.nodes.array;

import me.kuwg.re.ast.nodes.variable.VariableReference;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.cast.CastManager;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.variable.RVariableTypeError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;
import me.kuwg.re.type.iterable.arr.ArrayType;
import me.kuwg.re.type.ptr.PointerType;
import me.kuwg.re.type.struct.StructType;

import java.util.Map;

public class ArraySetNode extends ValueNode {
    private final ValueNode array;
    private final ValueNode index;
    private final ValueNode value;

    public ArraySetNode(final String fileName, final int line, final ValueNode array, final ValueNode index, final ValueNode value) {
        super(fileName, line);
        this.array = array;
        this.index = index;
        this.value = value;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        array.replaceGenerics(generics, cctx);
        index.replaceGenerics(generics, cctx);
        value.replaceGenerics(generics, cctx);
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        String arrayPtr;

        if (array instanceof VariableReference vr) {
            var arrVar = vr.getVariable(cctx);
            if (arrVar == null) {
                return new RVariableTypeError("addressable array", "temporary value", fileName, line).raise();
            }

            if (arrVar.type() instanceof ArrayType) {
                arrayPtr = arrVar.addrReg();
            } else {
                arrayPtr = arrVar.valueReg();
            }
        } else {
            arrayPtr = array.compileAndGet(cctx);
        }

        String valueReg = value.compileAndGet(cctx);

        cctx.emit("; Array set");

        if (value.getType() instanceof StructType && value instanceof VariableReference vr) {
            var var = vr.getVariable(cctx);
            if (var != null && valueReg.equals(var.addrReg())) {
                String loaded = cctx.nextRegister();
                cctx.emit(loaded + " = load " + value.getType().getLLVMName() + ", " + toPtr(value.getType().getLLVMName()) + " " + valueReg);
                valueReg = loaded;
            }
        }

        String indexReg = index.compileAndGet(cctx);

        if (!BuiltinTypes.INT.getType().isCompatibleWith(index.getType())) {
            return new RVariableTypeError("int", index.getType().getName(), fileName, line).raise();
        }

        TypeRef arrayType = array.getType();
        TypeRef elementType;
        boolean isArrayValuedPtr = arrayType instanceof ArrayType;

        if (arrayType instanceof ArrayType arrType) {
            elementType = arrType.getInner();
        } else if (arrayType instanceof PointerType ptrType) {
            elementType = ptrType.getInner();
        } else {
            return new RVariableTypeError("array or pointer", arrayType.getName(), fileName, line).raise();
        }

        elementType = evalType(elementType, cctx, fileName, line);

        if (!elementType.isCompatibleWith(value.getType())) {
            return new RVariableTypeError(elementType.getName(), value.getType().getName(), fileName, line).raise();
        }

        String index64Reg = indexReg;
        if (!index.getType().equals(BuiltinTypes.LONG.getType())) {
            index64Reg = CastManager.executeCast(fileName, line, indexReg, index.getType(), BuiltinTypes.LONG.getType(), cctx);
        }

        String elemPtrReg = cctx.nextRegister();
        String llvmElemType = elementType.getLLVMName();

        if (isArrayValuedPtr) {
            String llvmArrType = arrayType.getLLVMName();
            cctx.emit(elemPtrReg + " = getelementptr " + llvmArrType + ", " + llvmArrType + "* " + arrayPtr + ", i64 0, i64 " + index64Reg);
        } else {
            cctx.emit(elemPtrReg + " = getelementptr " + llvmElemType + ", " + llvmElemType + "* " + arrayPtr + ", i64 " + index64Reg);
        }
        cctx.emit("store " + llvmElemType + " " + valueReg + ", " + llvmElemType + "* " + elemPtrReg);

        setType(value.getType());
        return valueReg;
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Array Set:").append(NEWLINE);
        sb.append(indent).append("\tArray:").append(NEWLINE);
        array.write(sb, indent + "\t\t");
        sb.append(indent).append("\tIndex:").append(NEWLINE);
        index.write(sb, indent + "\t\t");
        sb.append(indent).append("\tValue:").append(NEWLINE);
        value.write(sb, indent + "\t\t");
    }

    @Override
    public void compile(final CompilationContext cctx) {
        compileAndGet(cctx);
    }

    @Override
    public ArraySetNode clone() {
        return new ArraySetNode(fileName, line, array.clone(), index.clone(), value.clone());
    }
}