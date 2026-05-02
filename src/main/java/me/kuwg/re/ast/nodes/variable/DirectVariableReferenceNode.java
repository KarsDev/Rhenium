package me.kuwg.re.ast.nodes.variable;

import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.error.errors.variable.RVariableNotFoundError;
import me.kuwg.re.type.struct.StructType;

public class DirectVariableReferenceNode extends VariableReference {
    private final String name;

    public DirectVariableReferenceNode(final int line, final String name) {
        super(line);
        this.name = name;
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        RVariable var = cctx.getVariable(name);

        if (var == null) {
            return new RVariableNotFoundError(name, line).raise();
        }

        setType(var.type());

        if (var.addrReg() != null) {
            String elemLLVM = var.type().getLLVMName();
            String tmp = cctx.nextRegister();
            if (var.type() instanceof StructType) {
                return var.addrReg();
            } else {
                cctx.emit(tmp + " = load " + elemLLVM + ", " + elemLLVM + "* " + var.addrReg());
            }
            return tmp;
        }



        return var.valueReg();
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
        var v = cctx.getVariable(name);
        if (v != null) setType(v.type());
        return v;
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
        return "DirectVariableReferenceNode{" + "name='" + name + '\'' + '}';
    }
}
