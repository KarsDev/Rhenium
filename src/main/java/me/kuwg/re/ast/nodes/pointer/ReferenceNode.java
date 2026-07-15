package me.kuwg.re.ast.nodes.pointer;

import me.kuwg.re.ast.nodes.variable.VariableReference;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.error.errors.variable.RVariableNotFoundError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.ptr.PointerType;

import java.util.Map;

public class ReferenceNode extends ValueNode {
    private final VariableReference value;

    public ReferenceNode(final String fileName, final int line, final VariableReference value) {
        super(fileName, line);
        this.value = value;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        value.replaceGenerics(generics, cctx);
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        RVariable var = value.getVariable(cctx);

        if (var == null) {
            return new RVariableNotFoundError(value.getCompleteName(), fileName, line).raise();
        }

        TypeRef type = var.type();

        setType(new PointerType(type));

        cctx.emit("; Reference node");
        return var.addrReg();
    }

    @Override
    public void compile(final CompilationContext cctx) {
        compileAndGet(cctx);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Reference: ").append(NEWLINE);
        value.write(sb, indent + TAB);
    }

    @Override
    public ReferenceNode clone() {
        return new ReferenceNode(fileName, line, value.clone());
    }
}
