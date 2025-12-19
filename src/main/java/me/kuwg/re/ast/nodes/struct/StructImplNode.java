package me.kuwg.re.ast.nodes.struct;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.function.BuiltinFunctionDeclarationNode;
import me.kuwg.re.ast.nodes.function.FunctionDeclarationNode;
import me.kuwg.re.ast.nodes.function.FunctionParameter;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.compiler.struct.RStruct;
import me.kuwg.re.error.errors.RInternalError;
import me.kuwg.re.error.errors.struct.RStructUndefinedError;
import me.kuwg.re.type.ptr.PointerType;

import java.util.ArrayList;
import java.util.List;

public class StructImplNode extends ASTNode {
    private final String name;
    private final List<ASTNode> functions;

    public StructImplNode(final int line, final String name, final List<ASTNode> functions) {
        super(line);
        this.name = name;
        this.functions = functions;
    }

    public static String generateName(String struct, String name) {
        String raw = struct + "." + name;

        String escaped = raw
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");

        return "\"" + escaped + "\"";
    }

    @Override
    public void compile(final CompilationContext cctx) {
        RStruct struct = cctx.getStruct(name);

        if (struct == null) {
            new RStructUndefinedError(name, line).raise();
            return;
        }

        for (ASTNode fn : functions) {
            RFunction compiled;

            if (fn instanceof FunctionDeclarationNode dec)
                compiled = compileFunction(cctx, struct, dec);
            else if (fn instanceof BuiltinFunctionDeclarationNode blt)
                compiled = compileBuiltin(cctx, struct, blt);
            else
                throw new RInternalError("internal error: not function declaration");

            struct.functions().add(compiled);
        }
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Struct Impl: ").append(NEWLINE)
                .append(TAB).append("Name: ").append(name).append(NEWLINE)
                .append(TAB).append("Functions:").append(NEWLINE);
        functions.forEach(f -> f.write(sb, indent + TAB + TAB));
    }

    private List<FunctionParameter> addSelfParam(RStruct struct, List<FunctionParameter> original) {
        List<FunctionParameter> newParams = new ArrayList<>(original.size() + 1);
        newParams.add(new FunctionParameter("self", false, new PointerType(struct.type())));
        newParams.addAll(original);
        return newParams;
    }

    private List<me.kuwg.re.type.TypeRef> extractTypes(List<FunctionParameter> params) {
        return params.stream().map(FunctionParameter::type).toList();
    }

    private RFunction compileFunction(CompilationContext cctx, RStruct struct, FunctionDeclarationNode original) {

        String mangledName = generateName(struct.type().getName(), original.getName());

        List<FunctionParameter> newParams = addSelfParam(struct, original.getParameters());

        FunctionDeclarationNode renamed = new FunctionDeclarationNode(
                original.getLine(),
                mangledName,
                newParams,
                original.getReturnType(),
                original.getBlock()
        );

        renamed.compile(cctx);

        return cctx.getFunction(mangledName, extractTypes(newParams));
    }

    private RFunction compileBuiltin(CompilationContext cctx, RStruct struct, BuiltinFunctionDeclarationNode original) {
        String mangledName = generateName(struct.type().getName(), original.getName());
        List<FunctionParameter> newParams = addSelfParam(struct, original.getParameters());

        BuiltinFunctionDeclarationNode renamed = new BuiltinFunctionDeclarationNode(
                original.getLine(),
                true,
                mangledName,
                newParams,
                original.getReturnType(),
                original.getLlvmBody()
        );

        renamed.compile(cctx);

        return cctx.getFunction(mangledName, extractTypes(newParams));
    }
}
