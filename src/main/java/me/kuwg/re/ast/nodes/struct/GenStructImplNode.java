package me.kuwg.re.ast.nodes.struct;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.function.declaration.FunctionParameter;
import me.kuwg.re.ast.nodes.function.declaration.GenFunctionDeclarationNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.function.RGenFunction;
import me.kuwg.re.compiler.struct.RStruct;
import me.kuwg.re.error.errors.struct.RStructUndefinedError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.ptr.PointerType;
import me.kuwg.re.type.struct.GenStructType;

import java.util.ArrayList;
import java.util.List;

public class GenStructImplNode extends ASTNode {

    private final GenStructType struct;
    private final List<GenFunctionDeclarationNode> functions;

    public GenStructImplNode(final int line, final GenStructType struct, final List<GenFunctionDeclarationNode> functions) {
        super(line);
        this.struct = struct;
        this.functions = functions;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        RStruct genStruct = cctx.getStruct(struct.name());

        if (genStruct == null) {
            new RStructUndefinedError(struct.name(), line).raise();
            return;
        }

        for (GenFunctionDeclarationNode fn : functions) {
            RGenFunction compiled = compileFunction(cctx, genStruct, fn);
            genStruct.functions().add(compiled);
        }
    }

    private RGenFunction compileFunction(CompilationContext cctx, RStruct struct, GenFunctionDeclarationNode original) {
        String mangledName = StructImplNode.generateName(struct.type().getName(), original.getName());

        List<FunctionParameter> newParams = addSelfParam((GenStructType) struct.type(), original.getParams());

        GenFunctionDeclarationNode renamed = new GenFunctionDeclarationNode(original.getLine(), mangledName, original.getTypeParameters(), newParams, original.getReturnType(), original.getBlock());

        renamed.compile(cctx);

        return (RGenFunction) cctx.getFunction(mangledName, extractTypes(newParams));
    }

    private List<FunctionParameter> addSelfParam(GenStructType struct, List<FunctionParameter> original) {
        List<FunctionParameter> params = new ArrayList<>(original.size() + 1);

        params.add(new FunctionParameter("self", false, new PointerType(struct)));

        params.addAll(original);
        return params;
    }

    private List<TypeRef> extractTypes(List<FunctionParameter> params) {
        return params.stream().map(FunctionParameter::type).toList();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Generic Struct Impl:").append(NEWLINE).append(indent).append(TAB).append("Name: ").append(struct.name()).append(NEWLINE).append(indent).append(TAB).append("Generics: ").append(struct.genericTypes()).append(NEWLINE).append(indent).append(TAB).append("Functions:").append(NEWLINE);

        functions.forEach(f -> f.write(sb, indent + TAB + TAB));
    }
}
