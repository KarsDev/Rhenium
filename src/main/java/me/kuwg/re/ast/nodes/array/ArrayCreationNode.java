package me.kuwg.re.ast.nodes.array;

import me.kuwg.re.ast.nodes.constants.ConstantNode;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.cast.CastManager;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.error.errors.variable.RVariableTypeError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;
import me.kuwg.re.type.iterable.arr.ArrayType;
import me.kuwg.re.type.ptr.PointerType;

public class ArrayCreationNode extends ValueNode {
    private final ValueNode size;

    public ArrayCreationNode(final int line, final TypeRef type, final ValueNode size) {
        super(line, type);
        this.size = size;
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        if (!(size instanceof ConstantNode cnst)) {
            return compileDynamic(cctx);
        }

        String sizeConst = cnst.compileToConstant(cctx);
        long sizeLong;

        switch (cnst.getType().getName()) {
            case "int" -> sizeLong = Integer.parseInt(sizeConst);
            case "long" -> sizeLong = Long.parseLong(sizeConst);
            default -> {
                return new RVariableTypeError("int / long", size.getType().getName(), line).raise();
            }
        }

        if (sizeLong <= 0) {
            return new RVariableTypeError("positive int or long", size.getType().getName(), line).raise();
        }

        TypeRef elementType = type;
        String llvmElemType = elementType.getLLVMName();

        String arrReg = cctx.nextRegister();
        cctx.emit(arrReg + " = alloca " + llvmElemType + ", i64 " + sizeLong);

        setType(new ArrayType(sizeLong, elementType));

        return arrReg;

    }

    private String compileDynamic(final CompilationContext cctx) {
        String sizeReg = size.compileAndGet(cctx);

        if (!BuiltinTypes.LONG.getType().isCompatibleWith(size.getType())) {
            return new RVariableTypeError("long", size.getType().getName(), line).raise();
        }

        if (!size.getType().equals(BuiltinTypes.LONG.getType())) {
            sizeReg = CastManager.executeCast(line, sizeReg, size.getType(), BuiltinTypes.LONG.getType(), cctx);
        }

        TypeRef elementType = type;
        String llvmElemType = elementType.getLLVMName();

        String bytesReg = cctx.nextRegister();
        cctx.emit(bytesReg + " = mul i64 " + sizeReg + ", " + elementType.getSize());

        cctx.addIR("declare i8* @malloc(i64)");

        String rawPtr = cctx.nextRegister();
        cctx.emit(rawPtr + " = call i8* @malloc(i64 " + bytesReg + ")");

        cctx.addIR("declare i8* @memset(i8*, i32, i64)");
        cctx.emit("call i8* @memset(i8* " + rawPtr + ", i32 0, i64 " + bytesReg + ")");
        cctx.nextRegister();

        String arrReg = cctx.nextRegister();
        cctx.emit(arrReg + " = bitcast i8* " + rawPtr + " to " + llvmElemType + "*");

        setType(new PointerType(new ArrayType(ArrayType.UNKNOWN_SIZE, elementType)));

        return arrReg;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RValueMustBeUsedError("Array Creation", line).raise();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Array Creation:").append(NEWLINE)
                .append(indent).append(TAB).append("Type: ").append(type.getName()).append(NEWLINE)
                .append(indent).append(TAB).append("Size: ").append(NEWLINE);
        size.write(sb, indent + TAB + TAB);
    }
}
