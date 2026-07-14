package me.kuwg.re.ast.nodes.array;

import me.kuwg.re.ast.types.value.PointerValueNode;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.cast.CastManager;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.error.errors.variable.RVariableTypeError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;
import me.kuwg.re.type.iterable.arr.ArrayType;
import me.kuwg.re.type.ptr.PointerType;

import java.util.Map;

public class ArrayCreationNode extends PointerValueNode {
    private final ValueNode size;

    public ArrayCreationNode(final String fileName, final int line, final TypeRef type, final ValueNode size) {
        super(fileName, line, type);
        this.size = size;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        size.replaceGenerics(generics, cctx);
        type = replaceGenericType(type, generics, cctx);
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        if (!(size.isConstant(cctx))) {
            return compileDynamic(cctx);
        }

        String sizeConst = size.compileToConstant(cctx);
        long sizeLong;

        switch (size.getType().getName()) {
            case "int" -> sizeLong = Integer.parseInt(sizeConst);
            case "long" -> sizeLong = Long.parseLong(sizeConst);
            default -> {
                return new RVariableTypeError("int / long", size.getType().getName(), fileName, line).raise();
            }
        }

        if (sizeLong < 0) {
            return new RVariableTypeError("positive int or long", size.getType().getName(), fileName, line).raise();
        }

        TypeRef elementType = evalType(type, cctx, fileName, line);

        long bytes = sizeLong * elementType.getSize();

        cctx.addIR("declare i8* @malloc(i64)");
        cctx.addIR("declare i8* @memset(i8*, i32, i64)");

        String rawPtr = cctx.nextRegister();
        cctx.emit("; Array creation");
        cctx.emit(rawPtr + " = call i8* @malloc(i64 " + bytes + ")");

        cctx.emit("call i8* @memset(i8* " + rawPtr + ", i32 0, i64 " + bytes + ")");
        cctx.nextRegister();

        ArrayType resultType = new ArrayType(sizeLong, elementType);
        String llvmArrType = resultType.getLLVMName();

        String arrReg = cctx.nextRegister();
        cctx.emit(arrReg + " = bitcast i8* " + rawPtr + " to " + llvmArrType + "*");

        setType(resultType);

        return arrReg;
    }

    private String compileDynamic(final CompilationContext cctx) {
        String sizeReg = size.compileAndGet(cctx);

        if (!BuiltinTypes.LONG.getType().isCompatibleWith(size.getType())) {
            return new RVariableTypeError("long", size.getType().getName(), fileName, line).raise();
        }

        if (!size.getType().equals(BuiltinTypes.LONG.getType())) {
            sizeReg = CastManager.executeCast(fileName, line, sizeReg, size.getType(), BuiltinTypes.LONG.getType(), cctx);
        }

        String llvmElemType = evalType(type, cctx, fileName, line).getLLVMName();

        String bytesReg = cctx.nextRegister();
        cctx.emit("; Dynamic array creation");
        cctx.emit(bytesReg + " = mul i64 " + sizeReg + ", " + evalType(type, cctx, fileName, line).getSize());

        cctx.addIR("declare i8* @malloc(i64)");

        String rawPtr = cctx.nextRegister();
        cctx.emit(rawPtr + " = call i8* @malloc(i64 " + bytesReg + ")");

        cctx.addIR("declare i8* @memset(i8*, i32, i64)");
        cctx.emit("call i8* @memset(i8* " + rawPtr + ", i32 0, i64 " + bytesReg + ")");
        cctx.nextRegister();

        String arrReg = cctx.nextRegister();
        cctx.emit(arrReg + " = bitcast i8* " + rawPtr + " to " + llvmElemType + "*");

        setType(new PointerType(evalType(type, cctx, fileName, line)));

        return arrReg;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RValueMustBeUsedError("Array Creation", fileName, line).raise();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Array Creation:").append(NEWLINE).append(indent).append(TAB).append("Type: ").append(type.getName()).append(NEWLINE).append(indent).append(TAB).append("Size: ").append(NEWLINE);
        size.write(sb, indent + TAB + TAB);
    }

    @Override
    public ArrayCreationNode clone() {
        return new ArrayCreationNode(fileName, line, type, size.clone());
    }
}
