package me.kuwg.re.ast.nodes.constants;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.error.errors.variable.RVariableTypeError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;

import java.util.Map;

public class SizeofNode extends ConstantNode {
    private TypeRef type;
    private final ValueNode value;

    public SizeofNode(final int line, final ValueNode value) {
        super(line, BuiltinTypes.INT.getType());
        this.type = null;
        this.value = value;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        type = replaceGenericType(type, generics, cctx);
        value.replaceGenerics(generics, cctx);
    }

    public SizeofNode(final int line, final TypeRef type) {
        super(line, BuiltinTypes.INT.getType());
        this.type = type;
        this.value = null;
    }

    @Override
    public String compileToConstant(final CompilationContext cctx) {
        if (value != null) return new RVariableTypeError("constant", "runtime", line).raise();
        cctx.emit("; Sizeof");
        return Long.toString(type.getSize());
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        cctx.emit("; Sizeof");
        if (type != null) return Long.toString(type.getSize());

        value.compileAndGet(cctx);
        return Long.toString(value.getType().getSize());
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RValueMustBeUsedError("sizeof", line).raise();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Sizeof: ").append(NEWLINE);
        value.write(sb, indent + TAB);
    }

    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public SizeofNode clone() {
        return new SizeofNode(line, value.clone());
    }
}
