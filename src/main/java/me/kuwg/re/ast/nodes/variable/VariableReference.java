package me.kuwg.re.ast.nodes.variable;

import me.kuwg.re.ast.value.ValueNode;
import me.kuwg.re.compiler.Compilable;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.writer.Writeable;

public abstract class VariableReference extends ValueNode implements Compilable, Writeable {
    protected VariableReference(final int line) {
        super(line);
    }

    public abstract RVariable getVariable(CompilationContext cctx);

    public abstract String getCompleteName();
    public abstract String getSimpleName();
}
