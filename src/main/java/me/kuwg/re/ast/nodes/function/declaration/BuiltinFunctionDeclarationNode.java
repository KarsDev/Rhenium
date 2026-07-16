package me.kuwg.re.ast.nodes.function.declaration;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.types.global.GlobalNode;
import me.kuwg.re.ast.types.load.TopLevelNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.function.RDefFunction;
import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.error.errors.function.RFunctionAlreadyExistError;
import me.kuwg.re.error.errors.function.RGlobalFunctionInNamespace;
import me.kuwg.re.error.errors.range.RRangeTypeError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.iterable.range.RangeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BuiltinFunctionDeclarationNode extends ASTNode implements GlobalNode, TopLevelNode {
    private final boolean keepName;
    private final String name;
    private final List<FunctionParameter> parameters;
    private final String llvmBody;
    private boolean registered = false;
    private String llvmName;
    private TypeRef returnType;

    public BuiltinFunctionDeclarationNode(final String fileName, final int line, final boolean keepName, final String name, final List<FunctionParameter> parameters, final TypeRef returnType, final String llvmBody) {
        super(fileName, line);
        this.keepName = keepName;
        this.name = name;
        this.parameters = parameters;
        this.returnType = returnType;
        this.llvmBody = llvmBody;

        if (returnType instanceof RangeType) {
            new RRangeTypeError(fileName, line).raise();
        }
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        returnType = replaceGenericType(returnType, generics, cctx);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Builtin Function Declaration: ").append(NEWLINE)
                .append(indent).append(TAB).append("Name: ").append(name).append(NEWLINE)
                .append(indent).append(TAB).append("Parameters: ").append(NEWLINE);
        parameters.forEach(p -> p.write(sb, indent + TAB));
    }

    @Override
    public void compile(final CompilationContext cctx) {
        if (!registered) load(cctx);

        String llvmName = registered
                ? this.llvmName
                : (keepName ? name : getLLVMName(cctx));

        StringBuilder func =
                new StringBuilder("; Builtin function declaration\n");

        func.append("define ")
                .append(returnType.getLLVMName())
                .append(" @")
                .append(llvmName)
                .append("(");

        for (int i = 0; i < parameters.size(); i++) {
            var param = parameters.get(i);

            func.append(param.type().getLLVMName())
                    .append(" %")
                    .append(param.name());

            if (i < parameters.size() - 1) {
                func.append(", ");
            }
        }

        func.append(") {\n");

        for (String s : llvmBody.split("\n")) {
            func.append(TAB).append(s).append('\n');
        }

        func.append("}\n\n");

        cctx.declare(func.toString());
    }

    @Override
    public BuiltinFunctionDeclarationNode clone() {
        return this;
    }

    private String getLLVMName(CompilationContext cctx) {
        String qualified = cctx.qualify(name);

        if (qualified.startsWith("\"") && qualified.endsWith("\"")) {
            String clean = qualified.substring(1, qualified.length() - 1);
            return "\"" + RFunction.makeUnique(clean) + "\"";
        }

        return RFunction.makeUnique(qualified);
    }


    public String getLlvmBody() {
        return llvmBody;
    }

    public String getName() {
        return name;
    }

    public List<FunctionParameter> getParameters() {
        return parameters;
    }

    public TypeRef getReturnType() {
        return returnType;
    }

    @Override
    public void load(final CompilationContext cctx) {
        if (registered) return;

        String qualifiedName = cctx.qualify(name);

        if (keepName) {
            llvmName = name;

            if (!cctx.currentNamespace().isEmpty()) {
                new RGlobalFunctionInNamespace(fileName, line).raise();
            }
        } else {
            llvmName = getLLVMName(cctx);
        }

        List<TypeRef> types = new ArrayList<>(parameters.size());
        for (FunctionParameter parameter : parameters) {
            types.add(parameter.type());
        }

        if (cctx.getExact(qualifiedName, types) != null) {
            String paramsToString = types.toString().replace("[", "(").replace("]", ")");

            new RFunctionAlreadyExistError("Builtin function already exists: " + name + paramsToString, fileName, line).raise();
        }

        cctx.addFunction(new RDefFunction(llvmName, qualifiedName, returnType, parameters));

        registered = true;
    }
}
