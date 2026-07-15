package me.kuwg.re.ast.nodes.lambda;

import me.kuwg.re.ast.nodes.blocks.BlockNode;
import me.kuwg.re.ast.nodes.blocks.ReturnNode;
import me.kuwg.re.ast.nodes.function.declaration.FunctionDeclarationNode;
import me.kuwg.re.ast.nodes.function.declaration.FunctionParameter;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.NoneBuiltinType;
import me.kuwg.re.type.lambda.LambdaType;

import java.util.List;
import java.util.Map;

public class LambdaDeclarationNode extends ValueNode {
    private final List<FunctionParameter> params;
    private final ValueNode func;

    public LambdaDeclarationNode(final String fileName, final int line, final List<FunctionParameter> params, final ValueNode func) {
        super(fileName, line);
        this.params = params;
        this.func = func;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        compileAndGet(cctx);
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        cctx.declare("; Lambda declaration");
        ValueNode cloned = func.clone();

        cctx.pushScope();
        cctx.pushFunctionBody();

        for (final FunctionParameter param : params) {
            cctx.addVariable(new RVariable(param.name(), param.mutable(), false, param.type(), "tva", "tvr"));
        }

        cloned.compile(cctx);

        cctx.popScope();
        cctx.popFunctionBody();

        TypeRef returnType = cloned.getType();

        String lambdaName = "__lambda_" + System.identityHashCode(this);

        BlockNode block = new BlockNode(fileName, List.of(returnType instanceof NoneBuiltinType ? func : new ReturnNode(fileName, line, func)));

        FunctionDeclarationNode fn = new FunctionDeclarationNode(fileName, line, false, lambdaName, params, returnType, block);

        fn.compile(cctx);

        setType(new LambdaType(params.stream().map(FunctionParameter::type).toList(), returnType));

        return "@" + fn.getLLVMName();
    }

    @Override
    public LambdaDeclarationNode clone() {
        return new LambdaDeclarationNode(fileName, line, params, func.clone());
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        params.replaceAll(param -> new FunctionParameter(param.name(), param.mutable(), replaceGenericType(param.type(), generics, cctx)));
        func.replaceGenerics(generics, cctx);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Lambda Declaration:").append(NEWLINE).append(indent).append(TAB).append("Params: ").append(params).append(NEWLINE).append(indent).append(TAB).append("Value: ").append(NEWLINE);
        func.write(sb, indent + TAB + TAB);
    }
}
