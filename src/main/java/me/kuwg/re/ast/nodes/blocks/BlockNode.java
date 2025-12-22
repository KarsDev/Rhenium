package me.kuwg.re.ast.nodes.blocks;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.function.call.FunctionCallNode;
import me.kuwg.re.ast.nodes.function.declaration.FunctionDeclarationNode;
import me.kuwg.re.ast.nodes.raise.RaiseNode;
import me.kuwg.re.ast.types.global.GlobalNode;
import me.kuwg.re.ast.types.interrupt.InterruptNode;
import me.kuwg.re.compiler.Compilable;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.RInternalError;
import me.kuwg.re.error.errors.block.RBlockSyntaxError;
import me.kuwg.re.error.errors.function.RFunctionReturnTypeMismatchError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.NoneBuiltinType;
import me.kuwg.re.writer.Writeable;

import java.util.ArrayList;
import java.util.List;

public class BlockNode implements Writeable, Compilable, GlobalNode {
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
        nodes.stream()
            .takeWhile(node -> node instanceof GlobalNode)
            .forEach(node ->
                    new RBlockSyntaxError("Block cannot contain global statements: " + node, node.getLine()).raise()
            );

        for (int i = 0; i < nodes.size(); i++) {
            final ASTNode node = nodes.get(i);
            node.compile(cctx);

            if (node instanceof InterruptNode && i < nodes.size() - 1) {
                new RBlockSyntaxError("Block gets interrupted but continues", node.getLine()).raise();
            }
        }
        compiled = true;
    }

    public String compileAndGet(TypeRef type, CompilationContext cctx) {
        String name = "\"BlockCompilationFunction" + cctx.nextRegister().substring(1) + "\"";

        var fdn = new FunctionDeclarationNode(0, false,true, name, new ArrayList<>(), type, this);
        fdn.compile(cctx);

        var fcn = new FunctionCallNode(0, name, new ArrayList<>());

        return fcn.compileAndGet(cctx);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Block: ").append(NEWLINE);
        nodes.forEach(node -> node.write(sb, TAB + indent));
    }

    public void checkTypes(TypeRef returnType, boolean mustReturn) {
        if (!compiled) throw new RInternalError("Block node has not been compiled yet.");

        boolean hasReturn = false;

        for (final ASTNode node : nodes) {
            if (node instanceof IBlockContainer bc) {
                bc.getBlock().checkTypes(returnType, false);
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
}
