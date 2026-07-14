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
import java.util.Map;
import java.util.stream.IntStream;

public class ArrayNode extends PointerValueNode {
    private final List<ValueNode> values;

    public ArrayNode(final String fileName, final int line, final List<ValueNode> values) {
        super(fileName, line);
        this.values = values;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        values.forEach(v -> v.replaceGenerics(generics, cctx));
    }

    private TypeRef inferType() {
        if (values.isEmpty()) {
            return new RArrayTypesMismatchError(fileName, line).raise();
        }
        TypeRef type = values.get(0).getType();

        for (ValueNode v : values) {
            if (type.isCompatibleWith(v.getType())) continue;
            return new RArrayTypesMismatchError(v.getType().getName(), type.getName(), fileName, line).raise();
        }

        var arr = new ArrayType(values.size(), values.get(0).getType());

        if (arr.inner() instanceof NoneBuiltinType) {
            return new RArrayTypeIsNoneError(fileName, line).raise();
        }

        return arr;
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        List<String> regs = new ArrayList<>(values.size());
        IntStream.range(0, values.size()).forEachOrdered(i -> regs.add(i, values.get(i).compileAndGet(cctx)));

        setType(inferType());

        ArrayType arrType = (ArrayType) getType();
        TypeRef elementType = arrType.inner();
        long size = arrType.size();

        String llvmElemType = elementType.getLLVMName();
        String llvmArrType = arrType.getLLVMName();

        long bytes = size * elementType.getSize();

        cctx.emit("; Array declaration");

        String rawPtr = cctx.nextRegister();
        cctx.emit(rawPtr + " = call i8* @malloc(i64 " + bytes + ")");

        String arrPtr = cctx.nextRegister();
        cctx.emit(arrPtr + " = bitcast i8* " + rawPtr + " to " + llvmArrType + "*");

        for (int i = 0; i < size; i++) {
            String gepReg = cctx.nextRegister();

            cctx.emit(gepReg + " = getelementptr " + llvmArrType + ", " + llvmArrType + "* " + arrPtr + ", i64 0, i64 " + i);
            if (elementType instanceof ArrayType innerArr) {
                long innerBytes = innerArr.size() * innerArr.inner().getSize();

                cctx.emit("call void @llvm.memcpy.p0.p0.i64(" + "ptr " + gepReg + ", " + "ptr " + regs.get(i) + ", " + "i64 " + innerBytes + ", " + "i1 false)");
            } else {
                cctx.emit("store " + llvmElemType + " " + regs.get(i) + ", " + llvmElemType + "* " + gepReg);
            }
        }

        return arrPtr;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RValueMustBeUsedError("Array", fileName, line).raise();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Array: ").append(NEWLINE);
        values.forEach(v -> v.write(sb, indent + TAB));
    }

    @Override
    public ArrayNode clone() {
        List<ValueNode> values = new ArrayList<>();
        IntStream.range(0, this.values.size()).forEach(i -> values.add(i, this.values.get(i).clone()));
        return new ArrayNode(fileName, line, values);
    }

    @Override
    public boolean isConstant(final CompilationContext cctx) {
        return values.stream().allMatch(v -> v.isConstant(cctx));
    }

    @Override
    public String compileToConstant(final CompilationContext cctx) {
        List<String> constantValues = values.stream().map(v -> v.compileToConstant(cctx)).toList();
        setType(inferType());

        ArrayType arrType = (ArrayType) getType();
        TypeRef inner = arrType.inner();

        StringBuilder sb = new StringBuilder();

        sb.append("[");

        for (int i = 0; i < values.size(); i++) {
            if (i != 0) sb.append(", ");

            sb.append(inner.getLLVMName())
                    .append(" ")
                    .append(constantValues.get(i));
        }

        sb.append("]");


        return sb.toString();
    }
}