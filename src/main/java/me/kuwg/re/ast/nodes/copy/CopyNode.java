package me.kuwg.re.ast.nodes.copy;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.type.TypeRef;

import java.util.Map;

public class CopyNode extends ValueNode {
    private final ValueNode value;

    public CopyNode(final String fileName, final int line, final ValueNode value) {
        super(fileName, line);
        this.value = value;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        value.replaceGenerics(generics, cctx);
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        cctx.emit("; copy");

        String valueReg = value.compileAndGet(cctx);
        TypeRef type = value.getType();
        setType(type);

        if (type.isPrimitive()) {
            return valueReg;
        }

        cctx.ensureCopyFunction(type, fileName, line);

        String src = cctx.nextRegister();
        cctx.emit(src + " = alloca " + type.getLLVMName());
        cctx.emit("store " + type.getLLVMName() + " " + valueReg +
                ", " + type.getLLVMName() + "* " + src);

        String dst = cctx.nextRegister();
        cctx.emit(dst + " = alloca " + type.getLLVMName());

        cctx.emit("call void @__copy_" + type.getMangledName() +
                "(" + type.getLLVMName() + "* " + dst +
                ", " + type.getLLVMName() + "* " + src + ")");

        String result = cctx.nextRegister();
        cctx.emit(result + " = load " + type.getLLVMName() +
                ", " + type.getLLVMName() + "* " + dst);

        return result;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        compileAndGet(cctx);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Copy:").append(NEWLINE);
        value.write(sb, indent + TAB);
    }

    @Override
    public CopyNode clone() {
        return new CopyNode(fileName, line, value.clone());
    }
}