package me.kuwg.re.ast.nodes.array;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.error.errors.variable.RVariableTypeError;
import me.kuwg.re.type.builtin.BuiltinTypes;
import me.kuwg.re.type.iterable.arr.ArrayType;
import me.kuwg.re.cast.CastManager;
import me.kuwg.re.type.TypeRef;

public class ArrayAccessNode extends ValueNode {
    private final ValueNode array;
    private final ValueNode index;

    public ArrayAccessNode(final int line, final ValueNode array, final ValueNode index) {
        super(line);
        this.array = array;
        this.index = index;
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        String arrayReg = array.compileAndGet(cctx);
        String indexReg = index.compileAndGet(cctx);

        if (!BuiltinTypes.INT.getType().isCompatibleWith(index.getType())) {
            return new RVariableTypeError("int", index.getType().getName(), line).raise();
        }

        if (array.getType().isCompatibleWith(BuiltinTypes.STR.getType())) {
            return compileStringAccess(cctx, arrayReg, indexReg);
        }

        if (!(array.getType() instanceof ArrayType arrType)) {
            return new RVariableTypeError("arr", array.getType().getName(), line).raise();
        }

        TypeRef elementType = arrType.inner();
        String llvmElemType = elementType.getLLVMName();

        String index64Reg = indexReg;
        if (arrType.size() == ArrayType.UNKNOWN_SIZE && !index.getType().equals(BuiltinTypes.LONG.getType())) {
            index64Reg = CastManager.executeCast(line, indexReg, index.getType(), BuiltinTypes.LONG.getType(), cctx);
        }

        String elemPtrReg = cctx.nextRegister();
        if (arrType.size() == ArrayType.UNKNOWN_SIZE) {
            cctx.emit(elemPtrReg + " = getelementptr " +
                    llvmElemType + ", " + llvmElemType + "* " + arrayReg +
                    ", i64 " + index64Reg);
        } else {
            String llvmArrType = "[" + arrType.size() + " x " + llvmElemType + "]";
            cctx.emit(elemPtrReg + " = getelementptr inbounds " +
                    llvmArrType + ", " + llvmArrType + "* " + arrayReg +
                    ", i32 0, i32 " + indexReg);
        }

        String loadReg = cctx.nextRegister();
        cctx.emit(loadReg + " = load " + llvmElemType + ", " + llvmElemType + "* " + elemPtrReg);

        setType(elementType);
        return loadReg;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RValueMustBeUsedError("Array Access", line).raise();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Array Access:").append(NEWLINE).append(indent).append("\tArray: ").append(NEWLINE);
        array.write(sb, indent + "\t\t");
        sb.append(indent).append("\tIndex: ").append(NEWLINE);
        index.write(sb, indent + "\t\t");
    }

    private String compileStringAccess(CompilationContext cctx, String arrayReg, String indexReg) {
        String charPtrReg = cctx.nextRegister();
        cctx.emit(charPtrReg + " = getelementptr i8, i8* " + arrayReg + ", i32 " + indexReg);

        String loadReg = cctx.nextRegister();
        cctx.emit(loadReg + " = load i8, i8* " + charPtrReg);

        setType(BuiltinTypes.CHAR.getType());
        return loadReg;
    }
}
