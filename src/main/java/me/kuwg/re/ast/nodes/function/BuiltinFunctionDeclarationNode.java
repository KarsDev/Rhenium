package me.kuwg.re.ast.nodes.function;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.types.global.GlobalNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.error.errors.function.RFunctionAlreadyExistError;
import me.kuwg.re.type.TypeRef;

import java.util.ArrayList;
import java.util.List;

public class BuiltinFunctionDeclarationNode extends ASTNode implements GlobalNode {
    private final String llvmName;
    private final String name;
    private final List<FunctionParameter> parameters;
    private final TypeRef returnType;
    private final String llvmBody;

    public BuiltinFunctionDeclarationNode(final int line, final boolean keepName, final String name, final List<FunctionParameter> parameters, final TypeRef returnType, final String llvmBody) {
        super(line);
        this.llvmName = keepName ? name : RFunction.makeUnique(name);
        this.name = name;
        this.parameters = parameters;
        this.returnType = returnType;
        this.llvmBody = llvmBody;
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Builtin Function Declaration: ").append(NEWLINE)
                .append(indent).append(TAB).append("Name: ").append(name)
                .append(NEWLINE).append(indent).append(TAB).append("Parameters: ").append(NEWLINE);
        parameters.forEach(p -> p.write(sb, indent + TAB));
    }

    @Override
    public void compile(final CompilationContext cctx) {
        RFunction fnObj = new RFunction(llvmName, name, returnType, parameters);


        StringBuilder func = new StringBuilder();
        func.append("define ").append(returnType.getLLVMName()).append(" @").append(fnObj.llvmName()).append("(");

        for (int i = 0; i < parameters.size(); i++) {
            var param = parameters.get(i);
            func.append(param.type().getLLVMName()).append(" %").append(param.name());
            if (i < parameters.size() - 1) func.append(", ");
        }

        func.append(") { ; Builtin function\n");

        for (String s : llvmBody.split("\n")) {
            func.append(TAB).append(s).append('\n');
        }

        func.append("}\n\n");

        cctx.declare(func.toString());

        List<TypeRef> types = new ArrayList<>(parameters.size());
        for (int i = 0; i < parameters.size(); i++) {
            var p = parameters.get(i);
            types.add(i, p.type());
        }

        if (cctx.getFunction(name, types) != null) {
            String paramsToString = types.toString().replace("[", "(").replace("]", ")");
            String error = "While compiling a builtin function, a function with the same name and parameters was found existing: " + name + paramsToString;
            new RFunctionAlreadyExistError(error, line).raise();
        }

        cctx.addFunction(fnObj);
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
}
