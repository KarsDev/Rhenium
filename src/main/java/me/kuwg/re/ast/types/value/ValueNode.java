package me.kuwg.re.ast.types.value;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.RInternalError;
import me.kuwg.re.type.TypeRef;

public abstract class ValueNode extends ASTNode {
    protected TypeRef type;

    protected ValueNode(final int line, final TypeRef type) {
        super(line);
        this.type = type;
    }

    protected ValueNode(final int line) {
        this(line, null);
    }

    protected void setType(final TypeRef type) {
        assert this.type == null;
        this.type = type;
    }

    public abstract String compileAndGet(CompilationContext cctx);

    public TypeRef getType() {
        if (type == null) {
            throw new RInternalError(getClass().getSimpleName() + " has no type");
        }
        return type;
    }
}
