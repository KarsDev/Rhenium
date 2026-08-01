package me.kuwg.re.ast.nodes.expression;

import me.kuwg.re.ast.nodes.variable.VariableReference;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.error.errors.variable.RVariableIsNotMutableError;
import me.kuwg.re.error.errors.variable.RVariableNotFoundError;
import me.kuwg.re.error.errors.variable.RVariableTypeError;
import me.kuwg.re.type.TypeRef;

import java.util.Map;

import static me.kuwg.re.operator.BinaryOperator.isNumeric;

public class IncDecExpressionNode extends ValueNode {
    private final IncDecOperator operator;
    private final IncDecPosition position;
    private final VariableReference variable;

    public IncDecExpressionNode(final String fileName, final int line, final IncDecOperator operator, final IncDecPosition position, final VariableReference variable) {
        super(fileName, line);
        this.variable = variable;
        this.operator = operator;
        this.position = position;
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        RVariable var = variable.getVariable(cctx);

        if (var == null) {
            return new RVariableNotFoundError(variable.getCompleteName(), fileName, line).raise();
        }

        if (!var.mutable()) {
            return new RVariableIsNotMutableError(variable.getCompleteName(), fileName, line).raise();
        }

        TypeRef type = evalType(var.type(), cctx, fileName, line);
        setType(type);

        if (!isNumeric(type)) {
            return new RVariableTypeError(
                    type.getName(),
                    "numeric",
                    fileName,
                    line
            ).raise();
        }

        String oldValue = cctx.nextRegister();
        cctx.emit(oldValue + " = load " + type.getLLVMName()
                + ", " + toPtr(type.getLLVMName()) + var.addrReg());

        String newValue = cctx.nextRegister();

        String op = operator == IncDecOperator.INCREMENT ? "add" : "sub";

        cctx.emit(newValue + " = " + op + " "
                + type.getLLVMName()
                + " " + oldValue + ", 1");

        cctx.emit("store "
                + type.getLLVMName()
                + " " + newValue
                + ", " + toPtr(type.getLLVMName())
                + var.addrReg());

        return position == IncDecPosition.PREFIX
                ? newValue
                : oldValue;
    }

    @Override
    public ValueNode clone() {
        return new IncDecExpressionNode(fileName, line, operator, position, variable.clone());
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        variable.replaceGenerics(generics, cctx);
    }

    @Override
    public void compile(final CompilationContext cctx) {
        compileAndGet(cctx);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Increment Decrement Expression:").append(NEWLINE)
                .append(indent).append(TAB).append("Operator: ").append(operator).append(NEWLINE)
                .append(indent).append(TAB).append("Position: ").append(position).append(NEWLINE)
                .append(indent).append(TAB).append("Value: ").append(NEWLINE);
        variable.write(sb, indent + TAB + TAB);
    }

    public enum IncDecOperator {
        INCREMENT, DECREMENT
    }
    public enum IncDecPosition {
        PREFIX, POSTFIX
    }
}
