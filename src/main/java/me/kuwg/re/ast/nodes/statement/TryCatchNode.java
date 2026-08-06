package me.kuwg.re.ast.nodes.statement;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.blocks.BlockNode;
import me.kuwg.re.ast.nodes.blocks.IBlockContainer;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.error.errors.RInternalError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.writer.Writeable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class TryCatchNode extends ASTNode implements IBlockContainer {
    private final BlockNode tryBlock;
    private final List<CatchClause> catches;

    public TryCatchNode(final String fileName, final int line, final BlockNode tryBlock, final List<CatchClause> catches) {
        super(fileName, line);
        this.tryBlock = tryBlock;
        this.catches = catches;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        tryBlock.replaceGenerics(generics, cctx);
        catches.forEach(c -> c.block.replaceGenerics(generics, cctx));
    }

    @Override
    public void compile(final CompilationContext cctx) {
        cctx.emit("; Try-catch");

        final List<CompiledCatch> compiledCatches = new ArrayList<>(catches.size());
        for (final CatchClause clause : catches) {
            String addrReg = null;

            if (clause.variable() != null && clause.type() != null) {
                addrReg = "%" + RVariable.makeUnique(clause.variable());
                cctx.emit(addrReg + " = alloca " + clause.type().getLLVMName() + " ; storage for caught variable '" + clause.variable() + "'");
            }

            compiledCatches.add(new CompiledCatch(clause.type(), cctx.nextLabel("catch"), addrReg));
        }

        final String endLabel = cctx.nextLabel("try_end");

        try {
            cctx.pushTryCatchScope(compiledCatches);

            cctx.emit("; Try block");
            tryBlock.compile(cctx);
        } finally {
            cctx.popTryCatchScope();
        }

        final String afterTryLabel = cctx.nextLabel("try_after");
        cctx.emit("br label %" + afterTryLabel);
        cctx.emit(afterTryLabel + ":");
        cctx.emit("br label %" + endLabel);

        for (int i = 0; i < catches.size(); i++) {
            final CatchClause clause = catches.get(i);
            final CompiledCatch compiled = compiledCatches.get(i);

            cctx.emit("; Catch " + (compiled.type() == null ? "any" : compiled.type().getName()));
            cctx.emit(compiled.label() + ":");

            final boolean boundVariable = clause.variable() != null && compiled.addrReg() != null;
            if (boundVariable) {
                cctx.pushScope();

                final String llvmType = Objects.requireNonNull(compiled.type()).getLLVMName();
                final String loaded = cctx.nextRegister();
                cctx.emit(loaded + " = load " + llvmType + ", " + toPtr(llvmType) + " " + compiled.addrReg());

                cctx.addVariable(new RVariable(clause.variable(), false, true, compiled.type(), compiled.addrReg(), loaded));
            }

            clause.block().compile(cctx);

            if (boundVariable) {
                cctx.popScope();
            }

            final String afterCatchLabel = cctx.nextLabel("catch_after");
            cctx.emit("br label %" + afterCatchLabel);
            cctx.emit(afterCatchLabel + ":");
            cctx.emit("br label %" + endLabel);
        }

        cctx.emit(endLabel + ":");
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Try:").append(NEWLINE);
        tryBlock.write(sb, indent + TAB);

        sb.append(indent).append("Catch:").append(NEWLINE);
        catches.forEach(c -> c.write(sb, indent + TAB));
    }

    @Override
    public TryCatchNode clone() {
        List<CatchClause> cloned = new ArrayList<>(catches.size());
        for (int i = 0; i < catches.size(); i++) {
            cloned.add(i, catches.get(i).clone());
        }
        return new TryCatchNode(fileName, line, tryBlock.clone(), cloned);
    }

    @Override
    public BlockNode getBlock() {
        throw new RInternalError();
    }

    public BlockNode getTryBlock() {
        return tryBlock;
    }

    public List<BlockNode> getCatchBlocks() {
        return catches.stream().map(c -> c.block).collect(Collectors.toList());
    }

    public record CompiledCatch(@Nullable TypeRef type, String label, @Nullable String addrReg) {}
    public record CatchClause(@Nullable TypeRef type, @Nullable String variable, BlockNode block) implements Writeable, Cloneable {
        @Override
        public void write(final StringBuilder sb, final String indent) {
            sb.append(indent).append("Catch:").append(NEWLINE)
                    .append(indent).append(TAB).append("Type: ").append(type == null ? "any" : type.getName()).append(NEWLINE)
                    .append(indent).append(TAB).append("Variable: ").append(variable == null ? "none" : variable);

            block.write(sb, indent + TAB);
        }

        @SuppressWarnings("MethodDoesntCallSuperMethod")
        @Override
        public CatchClause clone() {
            return new CatchClause(type, variable, block.clone());
        }
    }
}