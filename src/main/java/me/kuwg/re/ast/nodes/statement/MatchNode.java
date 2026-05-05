package me.kuwg.re.ast.nodes.statement;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.blocks.BlockNode;
import me.kuwg.re.ast.nodes.constants.ConstantNode;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.variable.RVariableTypeError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.writer.Writeable;

import java.util.List;
import java.util.Map;

public class MatchNode extends ASTNode {
    private final ValueNode expr;
    private final List<MatchCase> cases;

    public MatchNode(final int line, final ValueNode expr, final List<MatchCase> cases) {
        super(line);
        this.expr = expr;
        this.cases = cases;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics) {
        expr.replaceGenerics(generics);
        cases.forEach(c -> c.block.replaceGenerics(generics));
    }

    @Override
    public void compile(final CompilationContext cctx) {
        final String exprReg = expr.compileAndGet(cctx);
        final String llvmType = evalType(expr.getType(), cctx).getLLVMName();

        final String endLabel = cctx.nextLabel("match_end");

        String defaultLabel = null;

        final List<String> caseLabels = new java.util.ArrayList<>();
        final List<MatchCase> nonDefaultCases = new java.util.ArrayList<>();

        for (MatchCase mc : cases) {
            if (mc.value == null) {
                defaultLabel = cctx.nextLabel("match_default");
            } else {
                caseLabels.add(cctx.nextLabel("match_case"));
                nonDefaultCases.add(mc);
            }


        }

        if (defaultLabel == null) {
            defaultLabel = endLabel;
        }

        cctx.emit("switch " + llvmType + " " + exprReg + ", label %" + defaultLabel + " [");

        cctx.pushIndent();

        for (int i = 0; i < nonDefaultCases.size(); i++) {
            final MatchCase mc = nonDefaultCases.get(i);
            ConstantNode v = mc.value;
            TypeRef t = evalType(expr.getType(), cctx);

            if (!v.getType().equals(t)) {
                new RVariableTypeError(v.getType().getName(), t.getName(), line).raise();
                return;
            }

            final String constVal = v.compileToConstant(cctx);

            cctx.emit(llvmType + " " + constVal + ", label %" + caseLabels.get(i));
        }

        cctx.popIndent();
        cctx.emit("]");

        for (int i = 0; i < nonDefaultCases.size(); i++) {
            final MatchCase mc = nonDefaultCases.get(i);
            final String label = caseLabels.get(i);

            cctx.emit(label + ":");
            cctx.pushIndent();

            mc.block.compile(cctx);

            cctx.emit("br label %" + endLabel);

            cctx.popIndent();
        }

        if (!defaultLabel.equals(endLabel)) {
            cctx.emit(defaultLabel + ":");
            cctx.pushIndent();

            for (MatchCase mc : cases) {
                if (mc.value == null) {
                    mc.block.compile(cctx);
                    break;
                }
            }

            cctx.emit("br label %" + endLabel);
            cctx.popIndent();
        }

        cctx.emit(endLabel + ":");
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Match:").append(NEWLINE).append(indent).append(TAB).append("Expression: ").append(NEWLINE);
        expr.write(sb, indent + TAB + TAB);
        sb.append(indent).append(TAB).append("Cases:").append(NEWLINE);
        cases.forEach(c -> c.write(sb, indent + TAB + TAB));
    }

    public List<MatchCase> getCases() {
        return cases;
    }

    public static final class MatchCase implements Writeable {
        public final ConstantNode value;
        public final BlockNode block;

        public MatchCase(final ConstantNode value, final BlockNode block) {
            this.value = value;
            this.block = block;
        }

        @Override
        public void write(final StringBuilder sb, final String indent) {
            sb.append(indent).append("Case: ").append(NEWLINE).append(indent).append(TAB).append("Value: ").append(NEWLINE);
            if (value == null) sb.append(indent).append(TAB).append(TAB).append("Default Case");
            else value.write(sb, indent + TAB + TAB);

            sb.append(indent).append(TAB).append("Block: ").append(NEWLINE);
            block.write(sb, indent + TAB + TAB);
        }
    }
}
