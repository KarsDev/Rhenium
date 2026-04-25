package me.kuwg.re.ast.nodes.struct;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.function.declaration.BuiltinFunctionDeclarationNode;
import me.kuwg.re.ast.nodes.function.declaration.FunctionDeclarationNode;
import me.kuwg.re.ast.nodes.function.declaration.FunctionParameter;
import me.kuwg.re.ast.types.global.GlobalNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.compiler.struct.RConstructor;
import me.kuwg.re.compiler.struct.RStruct;
import me.kuwg.re.error.errors.RInternalError;
import me.kuwg.re.error.errors.struct.RStructUndefinedError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.NoneBuiltinType;
import me.kuwg.re.type.ptr.PointerType;
import me.kuwg.re.type.struct.StructType;

import java.util.ArrayList;
import java.util.List;

public class StructImplNode extends ASTNode implements GlobalNode {
    private final StructType struct;
    private final List<RConstructor> constructors;
    private final List<ASTNode> functions;

    public StructImplNode(final int line, final StructType struct, final List<RConstructor> constructors, final List<ASTNode> functions) {
        super(line);
        this.struct = struct;
        this.constructors = constructors;
        this.functions = functions;
    }

    public static String generateName(String struct, String name) {
        String raw = struct + "." + name;

        String escaped = raw.replace("\\", "\\\\").replace("\"", "\\\"");

        return "\"" + escaped + "\"";
    }

    @Override
    public void compile(final CompilationContext cctx) {
        RStruct cctxStruct = cctx.getStruct(struct.name());


        if (cctxStruct == null) {
            new RStructUndefinedError(struct.name(), line).raise();
            return;
        }

        constructors.forEach(constructor -> compileConstructor(constructor, cctx));

        for (ASTNode fn : functions) {
            RFunction compiled;

            if (fn instanceof FunctionDeclarationNode dec) compiled = compileFunction(cctx, cctxStruct, dec);
            else if (fn instanceof BuiltinFunctionDeclarationNode blt) compiled = compileBuiltin(cctx, cctxStruct, blt);
            else throw new RInternalError("internal error: not function declaration");

            cctxStruct.functions().add(compiled);
        }
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Struct Impl: ").append(NEWLINE).append(TAB).append("Name: ").append(struct.name()).append(NEWLINE).append(TAB).append("Functions:").append(NEWLINE);
        functions.forEach(f -> f.write(sb, indent + TAB + TAB));
    }

    private void compileConstructor(RConstructor constructor, CompilationContext cctx) {
        RStruct structCtx = cctx.getStruct(struct.name());
        if (structCtx == null) {
            new RStructUndefinedError(struct.name(), line).raise();
            return;
        }

        String mangledName = generateName(struct.getMangledName(), constructor.llvmName());

        List<FunctionParameter> newParams = addSelfParam(structCtx, constructor.parameters());

        FunctionDeclarationNode ctorNode = new FunctionDeclarationNode(constructor.block().getNodes().get(0).getLine(), false, mangledName, newParams, NoneBuiltinType.INSTANCE, constructor.block());

        ctorNode.compile(cctx);

        RFunction compiledCtor = cctx.getFunction(mangledName, extractTypes(newParams));
        structCtx.constructors().add(compiledCtor);
    }

    private List<FunctionParameter> addSelfParam(RStruct struct, List<FunctionParameter> original) {
        List<FunctionParameter> newParams = new ArrayList<>(original.size() + 1);
        newParams.add(new FunctionParameter("self", false, new PointerType(struct.type())));
        newParams.addAll(original);
        return newParams;
    }

    private List<TypeRef> extractTypes(List<FunctionParameter> params) {
        return params.stream().map(FunctionParameter::type).toList();
    }

    private RFunction compileFunction(CompilationContext cctx, RStruct struct, FunctionDeclarationNode original) {

        String mangledName = generateName(struct.type().getName(), original.getName());

        List<FunctionParameter> newParams = addSelfParam(struct, original.getParameters());

        FunctionDeclarationNode renamed = new FunctionDeclarationNode(original.getLine(), false, mangledName, newParams, original.getReturnType(), original.getBlock());

        renamed.compile(cctx);

        return cctx.getFunction(mangledName, extractTypes(newParams));
    }

    private RFunction compileBuiltin(CompilationContext cctx, RStruct struct, BuiltinFunctionDeclarationNode original) {
        String mangledName = generateName(struct.type().getName(), original.getName());
        List<FunctionParameter> newParams = addSelfParam(struct, original.getParameters());

        BuiltinFunctionDeclarationNode renamed = new BuiltinFunctionDeclarationNode(original.getLine(), true, mangledName, newParams, original.getReturnType(), original.getLlvmBody());

        renamed.compile(cctx);

        return cctx.getFunction(mangledName, extractTypes(newParams));
    }
}
