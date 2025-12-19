package me.kuwg.re.ast.nodes.ternary;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.condition.RInvalidConditionError;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.type.builtin.BuiltinTypes;

public class TernaryOperatorNode extends ValueNode {
    private final ValueNode condition;
    private final ValueNode thenExpr;
    private final ValueNode elseExpr;

    public TernaryOperatorNode(int line, ValueNode condition, ValueNode thenExpr, ValueNode elseExpr) {
        super(line);
        this.condition = condition;
        this.thenExpr = thenExpr;
        this.elseExpr = elseExpr;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RValueMustBeUsedError("Ternary operator", line);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Ternary Operator").append(NEWLINE);

        sb.append(indent).append(TAB).append("Condition:").append(NEWLINE);
        condition.write(sb, indent + TAB + TAB);

        sb.append(indent).append(TAB).append("Then:").append(NEWLINE);
        thenExpr.write(sb, indent + TAB + TAB);

        sb.append(indent).append(TAB).append("Else:").append(NEWLINE);
        elseExpr.write(sb, indent + TAB + TAB);
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        String condReg = condition.compileAndGet(cctx);
        var condType = condition.getType();

        if (!condType.equals(BuiltinTypes.BOOL.getType())) {
            return new RInvalidConditionError(condType, line).raise();
        }

        String thenLabel = cctx.nextLabel("ternary_then");
        String elseLabel = cctx.nextLabel("ternary_else");
        String mergeLabel = cctx.nextLabel("ternary_merge");

        cctx.emit("br i1 " + condReg + ", label %" + thenLabel + ", label %" + elseLabel);

        cctx.emit(thenLabel + ":");
        cctx.pushIndent();
        String thenReg = thenExpr.compileAndGet(cctx);
        var thenType = thenExpr.getType();
        cctx.emit("br label %" + mergeLabel);
        cctx.popIndent();

        cctx.emit(elseLabel + ":");
        cctx.pushIndent();
        String elseReg = elseExpr.compileAndGet(cctx);
        var elseType = elseExpr.getType();
        cctx.emit("br label %" + mergeLabel);
        cctx.popIndent();

        cctx.emit(mergeLabel + ":");

        if (!thenType.equals(elseType)) {
            throw new RuntimeException(
                    "Ternary branches must have the same type (got "
                            + thenType.getName() + " and " + elseType.getName() + ")"
            );
        }

        String resultReg = cctx.nextRegister();
        cctx.emit(
                resultReg + " = phi " + thenType.getLLVMName() +
                        " [ " + thenReg + ", %" + thenLabel + " ]," +
                        " [ " + elseReg + ", %" + elseLabel + " ]"
        );

        setType(thenType);
        return resultReg;
    }

}
