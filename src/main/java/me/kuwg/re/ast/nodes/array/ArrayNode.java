package me.kuwg.re.ast.nodes.array;

import me.kuwg.re.ast.value.PointerValueNode;
import me.kuwg.re.ast.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.array.RArrayTypesMismatchError;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.iterable.arr.ArrayType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class ArrayNode extends PointerValueNode {
    private final List<ValueNode> values;

    public ArrayNode(final int line, final List<ValueNode> values) {
        super(line);
        this.values = values;
    }

    private void inferAndSetType() {
        if (values.isEmpty()) {
            new RArrayTypesMismatchError(line).raise();
            return;
        }
        TypeRef type = values.get(0).getType();

        for (ValueNode v : values) {
            if (type.isCompatibleWith(v.getType())) continue;
            new RArrayTypesMismatchError(v.getType().getName(), type.getName(), line).raise();
            return;
        }

        TypeRef arr = new ArrayType(values.size(), values.get(0).getType());
        setType(arr);
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        List<String> regs = new ArrayList<>(values.size());
        IntStream.range(0, values.size()).forEachOrdered(i ->
                regs.add(i, values.get(i).compileAndGet(cctx))
        );

        inferAndSetType();
        ArrayType arrType = (ArrayType) getType();
        TypeRef elementType = arrType.inner();
        int size = arrType.size();

        String llvmElemType = elementType.getLLVMName();
        String llvmArrType = "[" + size + " x " + llvmElemType + "]";

        String arrReg = cctx.nextRegister();
        cctx.emit(arrReg + " = alloca " + llvmArrType + " ; allocate array");

        for (int i = 0; i < size; i++) {
            String gepReg = cctx.nextRegister();
            cctx.emit(
                    gepReg + " = getelementptr " +
                            llvmArrType + ", " + llvmArrType + "* " + arrReg +
                            ", i32 0, i32 " + i +
                            " ; index into array"
            );

            if (elementType instanceof ArrayType innerArr) {
                int bytes = innerArr.size() * innerArr.inner().getSize();

                cctx.emit(
                        "call void @llvm.memcpy.p0.p0.i64(" +
                                "ptr " + gepReg + ", " +
                                "ptr " + regs.get(i) + ", " +
                                "i64 " + bytes + ", " +
                                "i1 false)"
                );
            } else {
                cctx.emit(
                        "store " + llvmElemType + " " + regs.get(i) +
                                ", " + llvmElemType + "* " + gepReg
                );
            }

        }


        return arrReg;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RValueMustBeUsedError("Array", line).raise();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Array: ").append(NEWLINE);
        values.forEach(v -> v.write(sb, indent + TAB));
    }
}