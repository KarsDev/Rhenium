package me.kuwg.re.ast.nodes.array;

import me.kuwg.re.ast.types.value.PointerValueNode;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.array.RArrayTypeIsNoneError;
import me.kuwg.re.error.errors.array.RArrayTypesMismatchError;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.NoneBuiltinType;
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

    private TypeRef inferType() {
        if (values.isEmpty()) {
            return new RArrayTypesMismatchError(line).raise();
        }
        TypeRef type = values.get(0).getType();

        for (ValueNode v : values) {
            if (type.isCompatibleWith(v.getType())) continue;
            return new RArrayTypesMismatchError(v.getType().getName(), type.getName(), line).raise();
        }

        var arr = new ArrayType(values.size(), values.get(0).getType());

        if (arr.inner() instanceof NoneBuiltinType) {
            return new RArrayTypeIsNoneError(line).raise();
        }

        return arr;
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        List<String> regs = new ArrayList<>(values.size());
        IntStream.range(0, values.size()).forEachOrdered(i ->
                regs.add(i, values.get(i).compileAndGet(cctx))
        );

        setType(inferType());
        ArrayType arrType = (ArrayType) getType();
        TypeRef elementType = arrType.inner();
        long size = arrType.size();

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
                long bytes = innerArr.size() * innerArr.inner().getSize();

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