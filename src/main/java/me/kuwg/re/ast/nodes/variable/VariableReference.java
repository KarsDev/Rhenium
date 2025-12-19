package me.kuwg.re.ast.nodes.variable;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.variable.RVariable;

public abstract class VariableReference extends ValueNode {
    protected VariableReference(final int line) {
        super(line);
    }

    public abstract RVariable getVariable(CompilationContext cctx);

    public abstract String getCompleteName();
    public abstract String getSimpleName();
}
