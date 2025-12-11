package me.kuwg.re.ast.nodes.function;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.blocks.BlockNode;
import me.kuwg.re.ast.nodes.blocks.IBlockContainer;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.error.errors.function.RFunctionAlreadyExistError;
import me.kuwg.re.error.errors.function.RMainFunctionError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;
import me.kuwg.re.type.builtin.NoneBuiltinType;

import java.util.ArrayList;
import java.util.List;

public class FunctionDeclarationNode extends ASTNode implements IBlockContainer {
    private final String llvmName;
    private final String name;
    private final List<FunctionParameter> parameters;
    private final TypeRef returnType;
    private final BlockNode block;

    private boolean registered = false;

    public FunctionDeclarationNode(final int line, final String name, final List<FunctionParameter> parameters, final TypeRef returnType, final BlockNode block) {
        super(line);
        this.llvmName = RFunction.makeUnique(name);
        this.name = name;
        this.parameters = parameters;
        this.returnType = returnType;
        this.block = block;
    }

    private static boolean appendMainReturn(StringBuilder sb) {
        String[] lines = sb.toString().split("\\r?\\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            return !line.startsWith("ret");
        }
        return true;
    }

    @Override
    public BlockNode getBlock() {
        return block;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        boolean main = name.equals("main") && parameters.isEmpty();

        List<TypeRef> types = new ArrayList<>(parameters.size());
        for (int i = 0; i < parameters.size(); i++) {
            var p = parameters.get(i);
            types.add(i, p.type());
        }

        StringBuilder func = new StringBuilder();
        func.append("define ").append(returnType.getLLVMName()).append(" @").append(main ? "main" : llvmName).append("(");

        for (int i = 0; i < parameters.size(); i++) {
            var param = parameters.get(i);
            func.append(param.type().getLLVMName()).append(" %").append(param.name());
            if (i < parameters.size() - 1) func.append(", ");
        }

        func.append(") { ; Function declaration\n");
        func.append("entry:\n");

        cctx.pushIndent();
        cctx.pushScope();
        cctx.pushFunctionBody();

        for (FunctionParameter param : parameters) {

            String paramPtr = "%" + param.name() + ".addr";
            cctx.emit(paramPtr + " = alloca " + param.type().getLLVMName() + " ; allocate parameter");
            cctx.emit("store " + param.type().getLLVMName() + " %" + param.name() + ", " + param.type().getLLVMName() + "* " + paramPtr + " ; store parameter value");

            RVariable paramVar = new RVariable(param.name(), param.mutable(), param.type(), paramPtr);
            cctx.addVariable(paramVar);
        }

        block.compile(cctx);
        if (returnType instanceof NoneBuiltinType) {
            cctx.emit("ret void");
        }

        block.checkTypes(returnType, true);


        String body = cctx.popFunctionBody();
        StringBuilder bodySB = new StringBuilder(body);

        if (appendMainReturn(bodySB)) {
            bodySB.append(TAB).append("ret ").append(returnType.getLLVMName()).append(" 0\n");
        }

        cctx.popScope();
        cctx.popIndent();

        func.append(bodySB);

        func.append("}\n\n");

        cctx.declare(func.toString());

        if (main && !returnType.equals(BuiltinTypes.INT.getType())) {
            new RMainFunctionError("main() function should return int", line).raise();
        }

        if (registered) return;

        if (cctx.getFunction(name, types) != null) {
            String paramsToString = types.toString().replace("[", "(").replace("]", ")");
            String error = "While compiling a function, a function with the same name and parameters was found existing: " + name + paramsToString;
            new RFunctionAlreadyExistError(error, line).raise();
        }

        RFunction fnObj = new RFunction(llvmName, name, returnType, parameters);
        cctx.addFunction(fnObj);
        registered = true;
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Function Declaration: ").append(NEWLINE).append(indent).append(TAB).append("Name: ").append(name).append(NEWLINE).append(indent).append(TAB).append("Parameters: ").append(NEWLINE);

        parameters.forEach(p -> p.write(sb, indent + TAB + TAB));

        sb.append(indent).append(TAB).append("Return Type: ").append(returnType.getName()).append(NEWLINE);
        block.write(sb, indent + TAB);
    }

    public void register(final CompilationContext cctx) {
        if (registered) return;
        RFunction fnObj = new RFunction(llvmName, name, returnType, parameters);
        cctx.addFunction(fnObj);
        registered = true;
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
