package me.kuwg.re.ast.nodes.blocks;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.function.call.FunctionCallNode;
import me.kuwg.re.ast.nodes.function.declaration.FunctionDeclarationNode;
import me.kuwg.re.ast.nodes.raise.RaiseNode;
import me.kuwg.re.ast.nodes.statement.TryCatchNode;
import me.kuwg.re.ast.types.global.GlobalNode;
import me.kuwg.re.ast.types.interrupt.InterruptNode;
import me.kuwg.re.compiler.Compilable;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.block.RBlockSyntaxError;
import me.kuwg.re.error.errors.function.RFunctionReturnTypeMismatchError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.NoneBuiltinType;
import me.kuwg.re.writer.Writeable;

import java.util.ArrayList;
import java.util.List;

public final class BlockNode implements Writeable, Compilable, GlobalNode, Cloneable {
    private final List<ASTNode> nodes;
    private boolean compiled = false;

    public BlockNode(final List<ASTNode> nodes) {
        this.nodes = nodes;
    }

    public List<ASTNode> getNodes() {
        return nodes;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        if (compiled) return;

        boolean terminated = false;

        for (final ASTNode node : nodes) {
            if (terminated) {
                new RBlockSyntaxError("Block gets interrupted but continues", node.getLine()).raise();
                break;
            }

            node.compile(cctx);

            if (node instanceof InterruptNode) {
                terminated = true;
            }
        }

        compiled = true;
    }

    public String compileAndGet(TypeRef type, CompilationContext cctx) {
        String name = "\"BlockCompilationFunction" + cctx.nextRegister().substring(1) + "\"";

        var fdn = new FunctionDeclarationNode(0, false, name, new ArrayList<>(), type, this);
        fdn.compile(cctx);

        var fcn = new FunctionCallNode(0, name, new ArrayList<>());

        return fcn.compileAndGet(cctx);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Block: ").append(NEWLINE);
        nodes.forEach(node -> node.write(sb, TAB + indent));
    }

    public void checkTypes(CompilationContext cctx, TypeRef returnType, boolean mustReturn) {
        if (!compiled) compile(cctx);

        boolean hasReturn = false;

        for (final ASTNode node : nodes) {
            if (node instanceof TryCatchNode tc) {
                tc.getTryBlock().checkTypes(cctx, returnType, false);
                tc.getCatchBlock().checkTypes(cctx, returnType, false);
                continue;
            }
            if (node instanceof IBlockContainer bc) {
                bc.getBlock().checkTypes(cctx, returnType, false);
            }

            if (node instanceof ReturnNode ret) {
                hasReturn = true;
                TypeRef type = ret.getValueType();

                if (!type.isCompatibleWith(returnType)) {
                    new RFunctionReturnTypeMismatchError(returnType, type, node.getLine()).raise();
                }
            }

            if (node instanceof RaiseNode) {
                hasReturn = true;
            }
        }

        if (!hasReturn && !(returnType instanceof NoneBuiltinType) && mustReturn) {
            new RFunctionReturnTypeMismatchError(
                    returnType,
                    NoneBuiltinType.INSTANCE,
                    nodes.isEmpty() ? -1 : nodes.get(nodes.size() - 1).getLine()
            ).raise();
        }
    }

    @Override
    public BlockNode clone() {
        BlockNode cloned ;
        try {
            cloned = (BlockNode) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        List<ASTNode> clonedNodes = new ArrayList<>(nodes.size());

        for (ASTNode node : nodes) {
            try {
                clonedNodes.add((ASTNode) node.clone());
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException("Failed to clone ASTNode: " + node.getClass(), e);
            }
        }

        cloned.getNodes().clear();
        cloned.getNodes().addAll(clonedNodes);

        return cloned;
    }
}
