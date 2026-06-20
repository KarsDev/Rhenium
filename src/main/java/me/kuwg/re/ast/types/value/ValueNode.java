package me.kuwg.re.ast.types.value;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.RInternalError;
import me.kuwg.re.error.errors.constant.RNotConstantError;
import me.kuwg.re.type.TypeRef;

public abstract class ValueNode extends ASTNode {
    protected TypeRef type;

    protected ValueNode(final String fileName, final int line, final TypeRef type) {
        super(fileName, line);
        this.type = type;
    }

    protected ValueNode(final String fileName, final int line) {
        this(fileName, line, null);
    }

    public void setType(final TypeRef type) {
        assert this.type == null;
        this.type = type;
    }

    public abstract String compileAndGet(CompilationContext cctx);

    public String compileToConstant(CompilationContext cctx) {
        return new RNotConstantError("Value was not constant when expected to be", fileName, line).raise();
    }

    public boolean isConstant(CompilationContext cctx) {
        return false;
    }

    public TypeRef getType() {
        if (type == null) {
            throw new RInternalError(getClass().getSimpleName() + " has no type");
        }
        return type;
    }

    @Override
    public abstract ValueNode clone();
}
