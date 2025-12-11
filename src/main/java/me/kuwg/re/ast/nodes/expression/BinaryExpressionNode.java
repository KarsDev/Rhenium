package me.kuwg.re.ast.nodes.expression;

import me.kuwg.re.ast.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.operator.BinaryOperator;
import me.kuwg.re.operator.BinaryOperatorContext;
import me.kuwg.re.type.TypeRef;

public class BinaryExpressionNode extends ValueNode {
    private final ValueNode left;
    private final BinaryOperator op;
    private final ValueNode right;

    public BinaryExpressionNode(final int line, final ValueNode left, final BinaryOperator op, final ValueNode right) {
        super(line);
        this.left = left;
        this.op = op;
        this.right = right;
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        String leftReg = left.compileAndGet(cctx);
        String rightReg = right.compileAndGet(cctx);

        TypeRef leftType = left.getType();
        TypeRef rightType = right.getType();

        var result = op.compile(new BinaryOperatorContext(leftReg, leftType, rightReg, rightType, line, cctx));

        String resultReg = result.code();

        setType(result.type());

        return resultReg;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RValueMustBeUsedError("Binary Expression", line).raise();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Binary Expression: ").append(NEWLINE);
        left.write(sb, indent + TAB);
        sb.append(indent).append(TAB).append("Symbol: ").append(op.getSymbol()).append(NEWLINE);
        right.write(sb, indent + TAB);
    }
}
