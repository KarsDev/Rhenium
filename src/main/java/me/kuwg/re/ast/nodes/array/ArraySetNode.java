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

public class ArraySetNode extends ValueNode {

    private final ValueNode array;
    private final ValueNode index;
    private final ValueNode value;

    public ArraySetNode(final int line, final ValueNode array, final ValueNode index, final ValueNode value) {
        super(line);
        this.array = array;
        this.index = index;
        this.value = value;
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        String arrayReg;

        if (array instanceof VariableReference vr) {
            var arrVar = vr.getVariable(cctx);
            if (arrVar == null || arrVar.addrReg() == null) {
                return new RVariableTypeError("addressable array", "temporary value", line).raise();
            }

            arrayReg = arrVar.addrReg();
        } else {
            arrayReg = array.compileAndGet(cctx);
        }

        String valueReg = value.compileAndGet(cctx);
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

        if (!elementType.isCompatibleWith(value.getType())) {
            return new RVariableTypeError(
                    elementType.getName(),
                    value.getType().getName(),
                    line
            ).raise();
        }


        String index64Reg = indexReg;

        if (!index.getType().equals(BuiltinTypes.LONG.getType())) {
            index64Reg = CastManager.executeCast(
                    line,
                    indexReg,
                    index.getType(),
                    BuiltinTypes.LONG.getType(),
                    cctx
            );
        }

        String llvmElemType = elementType.getLLVMName();
        String elemPtrReg = cctx.nextRegister();

        if (fixedArray) {
            String llvmArrType = "[" + fixedSize + " x " + llvmElemType + "]";
            cctx.emit(
                    elemPtrReg +
                            " = getelementptr inbounds " +
                            llvmArrType + ", " +
                            llvmArrType + "* " +
                            arrayReg + ", i32 0, i64 " + index64Reg
            );
        } else {
            cctx.emit(
                    elemPtrReg +
                            " = getelementptr " +
                            llvmElemType + ", " +
                            llvmElemType + "* " +
                            arrayReg + ", i64 " + index64Reg
            );
        }

        cctx.emit(
                "store " +
                        llvmElemType + " " +
                        valueReg + ", " +
                        llvmElemType + "* " +
                        elemPtrReg
        );

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
}
