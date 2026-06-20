package me.kuwg.re.ast.nodes.blocks;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.types.interrupt.InterruptNode;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.NoneBuiltinType;

import java.util.Map;

public class ReturnNode extends ASTNode implements InterruptNode {
    private final ValueNode value;

    public ReturnNode(final String fileName, final int line, final ValueNode value) {
        super(fileName, line);
        this.value = value;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        if (value != null) value.replaceGenerics(generics, cctx);
    }

    @Override
    public void compile(final CompilationContext cctx) {
        cctx.emit("; Return statement");
        if (value == null) {
            cctx.emit("ret void");
        } else {
            String valueReg = value.compileAndGet(cctx);
            String llvmType = value.getType().getLLVMName();

            valueReg = cctx.ensureValue(value, valueReg);

            cctx.emit("ret " + llvmType + " " + valueReg);
        }
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Return: ").append(NEWLINE);
        if (value != null) {
            value.write(sb, indent + TAB);
        }
    }

    public TypeRef getValueType() {
        if (value == null) return NoneBuiltinType.INSTANCE;
        return value.getType();
    }

    @Override
    public ReturnNode clone() {
        return new ReturnNode(fileName, line, value == null ? null : value.clone());
    }
}
