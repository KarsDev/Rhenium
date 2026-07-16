package me.kuwg.re.ast.nodes.function.declaration;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.blocks.BlockNode;
import me.kuwg.re.ast.types.load.TopLevelNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.compiler.function.RGenFunction;
import me.kuwg.re.compiler.generic.TypeParameter;
import me.kuwg.re.type.TypeRef;

import java.util.List;
import java.util.Map;

public class GenFunctionDeclarationNode extends ASTNode implements TopLevelNode {
    private final String name;
    private final List<TypeParameter> typeParameters;
    private final List<FunctionParameter> params;
    private TypeRef returnType;
    private final BlockNode block;

    private boolean registered = false;

    public GenFunctionDeclarationNode(final String fileName, final int line, final String name, final List<TypeParameter> typeParameters, final List<FunctionParameter> params, final TypeRef returnType, final BlockNode block) {
        super(fileName, line);
        this.name = name;
        this.typeParameters = typeParameters;
        this.params = params;
        this.returnType = returnType;
        this.block = block;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        returnType = replaceGenericType(returnType, generics, cctx);
        block.replaceGenerics(generics, cctx);
    }

    @Override
    public void compile(final CompilationContext cctx) {
        if (registered) return;

        cctx.declare("; Generic function declaration " + name);

        load(cctx);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Generic Function Declaration: ").append(NEWLINE)
                .append(indent).append(TAB).append("Name: ").append(name).append(NEWLINE)
                .append(indent).append(TAB).append("Type Parameters: ").append(typeParameters).append(NEWLINE)
                .append(indent).append(TAB).append("Parameters: ").append(NEWLINE);
        params.forEach(p -> p.write(sb, indent + TAB + TAB));
        sb.append(indent).append(TAB).append("Return Type: ").append(returnType.getName()).append(NEWLINE);
        block.write(sb, indent + TAB);
    }

    @Override
    public GenFunctionDeclarationNode clone() {
        var v = new GenFunctionDeclarationNode(fileName, line, name, typeParameters, params, returnType, block.clone());
        v.registered = registered;
        return v;
    }

    @Override
    public void load(final CompilationContext cctx) {
        if (registered) return;

        RFunction fn = new RGenFunction(
                RFunction.makeUnique(cctx.qualify(name)),
                cctx.qualify(name),
                typeParameters,
                returnType,
                params,
                block
        );

        cctx.addFunction(fn);
        registered = true;
    }
}
