
package me.kuwg.re.ast.nodes.variable;

import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.error.errors.variable.RVariableNotFoundError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.iterable.arr.ArrayType;

public class DirectVariableReferenceNode extends VariableReference {
    private final String name;

    public DirectVariableReferenceNode(final int line, final String name) {
        super(line);
        this.name = name;
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        RVariable variable = cctx.getVariable(name);

        if (variable == null) {
            return new RVariableNotFoundError(name, line).raise();
        }

        TypeRef type = variable.type();
        setType(type);

        String resultReg;

        if (type instanceof ArrayType) {
            resultReg = variable.valueReg();
        } else {
            resultReg = cctx.nextRegister();
            cctx.emit(resultReg + " = load " + type.getLLVMName() + ", " + type.getLLVMName() + "* " + variable.valueReg() + " ; load variable " + name);
        }

        return resultReg;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RValueMustBeUsedError("Variable", line).raise();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Name: ").append(name).append(NEWLINE);
    }

    @Override
    public RVariable getVariable(final CompilationContext cctx) {
        return cctx.getVariable(name);
    }

    @Override
    public String getCompleteName() {
        return name;
    }

    @Override
    public String getSimpleName() {
        return name;
    }

    @Override
    public String toString() {
        return "DirectVariableReferenceNode{" +
                "name='" + name + '\'' +
                '}';
    }
}
