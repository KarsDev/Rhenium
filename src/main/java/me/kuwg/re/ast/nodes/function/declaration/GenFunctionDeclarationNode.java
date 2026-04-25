package me.kuwg.re.ast.nodes.function.declaration;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.blocks.BlockNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.compiler.function.RGenFunction;
import me.kuwg.re.type.TypeRef;

import java.util.List;

public class GenFunctionDeclarationNode extends ASTNode {
    private final String name;
    private final List<String> typeParameters;
    private final List<FunctionParameter> params;
    private final TypeRef returnType;
    private final BlockNode block;

    private boolean registered = false;

    public GenFunctionDeclarationNode(final int line, final String name, final List<String> typeParameters, final List<FunctionParameter> params, final TypeRef returnType, final BlockNode block) {
        super(line);
        this.name = name;
        this.typeParameters = typeParameters;
        this.params = params;
        this.returnType = returnType;
        this.block = block;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        if (registered) return;

        RFunction fn = new RGenFunction(name, name, typeParameters, returnType, params, block);

        cctx.addFunction(fn);
        registered = true;
    }

    public void register(final CompilationContext cctx) {
        if (registered) return;
        compile(cctx);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Function Declaration: ").append(NEWLINE).append(indent).append(TAB).append("Name: ").append(name).append(NEWLINE).append(indent).append(TAB).append("Type Parameters: ").append(typeParameters).append(NEWLINE).append(indent).append(TAB).append("Parameters: ").append(NEWLINE);
        params.forEach(p -> p.write(sb, indent + TAB + TAB));
        sb.append(indent).append(TAB).append("Return Type: ").append(returnType.getName()).append(NEWLINE);
        block.write(sb, indent + TAB);
    }
}
