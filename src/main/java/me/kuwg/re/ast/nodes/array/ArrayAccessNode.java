package me.kuwg.re.ast.nodes.array;

import me.kuwg.re.ast.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.error.errors.variable.RVariableTypeError;
import me.kuwg.re.type.arr.ArrayType;
import me.kuwg.re.type.builtin.BuiltinTypes;

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


        var elementType = arrType.inner();
        String llvmElemType = elementType.getLLVMName();

        String elemPtrReg = cctx.nextRegister();
        String llvmArrType = "[" + arrType.size() + " x " + llvmElemType + "]";
        cctx.emit(elemPtrReg + " = getelementptr " +
                llvmArrType + ", " + llvmArrType + "* " + arrayReg +
                ", i32 0, i32 " + indexReg + " ; index array");

        String loadReg = cctx.nextRegister();
        cctx.emit(loadReg + " = load " + llvmElemType + ", " + llvmElemType + "* " + elemPtrReg + " ; load array element");

        setType(elementType);
        return loadReg;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RValueMustBeUsedError("Array Access", line).raise();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Array Access:").append(NEWLINE).append(indent).append(TAB).append("Array: ").append(NEWLINE);
        array.write(sb, indent + TAB + TAB);
        sb.append(indent).append(TAB).append("Index: ").append(NEWLINE);
        index.write(sb, indent + TAB + TAB);
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
