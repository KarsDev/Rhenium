package me.kuwg.re.ast.nodes.array;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.cast.CastManager;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.variable.RVariableTypeError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;
import me.kuwg.re.type.iterable.arr.ArrayType;

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
        String arrayPtr = array.compileAndGet(cctx);
        String valueReg = value.compileAndGet(cctx);

        TypeRef arrayType = array.getType();
        if (!(arrayType instanceof ArrayType arrType)) {
            new RVariableTypeError("array", arrayType.getName(), line).raise();
            return valueReg;
        }

        if (!arrType.inner().isCompatibleWith(value.getType())) {
            return new RVariableTypeError(arrType.inner().getName(), value.getType().getName(), line).raise();
        }

        String indexReg = index.compileAndGet(cctx);

        if (!BuiltinTypes.INT.getType().isCompatibleWith(index.getType())) {
            return new RVariableTypeError("int", index.getType().getName(), line).raise();
        }

        String index64Reg = indexReg;
        if (arrType.size() == ArrayType.UNKNOWN_SIZE) {
            if (!index.getType().equals(BuiltinTypes.LONG.getType())) {
                index64Reg = CastManager.executeCast(line, indexReg, index.getType(), BuiltinTypes.LONG.getType(), cctx);
            }
        }

        String elemPtrReg = cctx.nextRegister();
        String llvmElemType = arrType.inner().getLLVMName();

        if (arrType.size() == ArrayType.UNKNOWN_SIZE) {
            // Dynamic array: arrayPtr is T*
            cctx.emit(elemPtrReg + " = getelementptr " +
                    llvmElemType + ", " + llvmElemType + "* " + arrayPtr +
                    ", i64 " + index64Reg + " ; index dynamic array element");
        } else {
            // Fixed-size array: arrayPtr is [N x T]*
            String llvmArrType = "[" + arrType.size() + " x " + llvmElemType + "]";
            cctx.emit(elemPtrReg + " = getelementptr inbounds " +
                    llvmArrType + ", " + llvmArrType + "* " + arrayPtr +
                    ", i32 0, i32 " + indexReg + " ; index array element");
        }

        cctx.emit("store " + llvmElemType + " " + valueReg + ", " + llvmElemType + "* " + elemPtrReg + " ; set array element");

        setType(value.getType());
        return valueReg;
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Array Set: ").append(NEWLINE)
                .append(indent).append("\tArray:").append(NEWLINE);
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
