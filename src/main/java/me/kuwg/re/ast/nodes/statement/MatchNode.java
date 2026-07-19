package me.kuwg.re.ast.nodes.statement;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.blocks.BlockNode;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.constant.RNotConstantError;
import me.kuwg.re.error.errors.variable.RVariableTypeError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.writer.Writeable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class MatchNode extends ASTNode {
    private final ValueNode expr;
    private final List<MatchCase> cases;

    public MatchNode(final String fileName, final int line, final ValueNode expr, final List<MatchCase> cases) {
        super(fileName, line);
        this.expr = expr;
        this.cases = cases;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        expr.replaceGenerics(generics, cctx);
        cases.forEach(c -> {
            if (c.values != null) {
                c.values.forEach(v -> v.replaceGenerics(generics, cctx));
            }
            c.block.replaceGenerics(generics, cctx);
        });
    }

    @Override
    public void compile(final CompilationContext cctx) {
        cctx.emit("; Match statement");

        final String exprReg = expr.compileAndGet(cctx);
        final TypeRef exprType = evalType(expr.getType(), cctx, fileName, line);
        final String llvmType = exprType.getLLVMName();

        final String endLabel = cctx.nextLabel("match_end");

        String defaultLabel = null;

        final List<String> caseLabels = new ArrayList<>();
        final List<MatchCase> nonDefaultCases = new ArrayList<>();

        for (MatchCase mc : cases) {
            if (mc.values == null || mc.values.isEmpty()) {
                if (defaultLabel == null) {
                    defaultLabel = cctx.nextLabel("match_default");
                }
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
            final String label = caseLabels.get(i);

            for (ValueNode v : mc.values) {
                if (!v.isConstant(cctx)) {
                    new RNotConstantError("Expected constant value for match case", fileName, line).raise();
                    return;
                }

                final String constVal = v.compileToConstant(cctx);

                if (!v.getType().equals(exprType)) {
                    new RVariableTypeError(v.getType().getName(), exprType.getName(), fileName, line).raise();
                    return;
                }


                cctx.emit(llvmType + " " + constVal + ", label %" + label);
            }
        }

        cctx.popIndent();
        cctx.emit("]");

        for (int i = 0; i < nonDefaultCases.size(); i++) {
            final MatchCase mc = nonDefaultCases.get(i);
            final String label = caseLabels.get(i);

            cctx.emit(label + ":");
            cctx.pushIndent();
            cctx.pushScope();

            mc.block.compile(cctx);

            cctx.emit("br label %" + endLabel);

            cctx.popIndent();
            cctx.popScope();
        }

        if (!defaultLabel.equals(endLabel)) {
            cctx.emit(defaultLabel + ":");
            cctx.pushIndent();

            for (MatchCase mc : cases) {
                if (mc.values == null || mc.values.isEmpty()) {
                    cctx.pushScope();
                    mc.block.compile(cctx);
                    cctx.popScope();
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

    @Override
    public MatchNode clone() {
        List<MatchCase> casesCloned = new ArrayList<>();
        IntStream.range(0, cases.size()).forEach(i -> casesCloned.add(i, cases.get(i).clone()));
        return new MatchNode(fileName, line, expr.clone(), casesCloned);
    }

    public List<MatchCase> getCases() {
        return cases;
    }

    public static final class MatchCase implements Writeable {
        public final List<ValueNode> values;
        public final BlockNode block;

        public MatchCase(final List<ValueNode> values, final BlockNode block) {
            this.values = values;
            this.block = block;
        }

        @Override
        public void write(final StringBuilder sb, final String indent) {
            sb.append(indent).append("Case: ").append(NEWLINE).append(indent).append(TAB).append("Value: ").append(NEWLINE);
            if (values == null || values.isEmpty()) {
                sb.append(indent).append(TAB).append(TAB).append("Default Case");
            } else {
                values.forEach(v -> v.write(sb, indent + TAB + TAB));
            }

            sb.append(indent).append(TAB).append("Block: ").append(NEWLINE);
            block.write(sb, indent + TAB + TAB);
        }

        @Override
        @SuppressWarnings("MethodDoesntCallSuperMethod")
        public MatchCase clone() {
            if (values == null) {
                return new MatchCase(null, block.clone());
            }

            List<ValueNode> cloned = new ArrayList<>(values.size());
            values.forEach(v -> cloned.add(v.clone()));
            return new MatchCase(cloned, block.clone());
        }

        public boolean isDefault() {
            return values == null;
        }
    }
}