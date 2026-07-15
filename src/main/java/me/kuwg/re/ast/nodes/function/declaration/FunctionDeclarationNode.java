package me.kuwg.re.ast.nodes.function.declaration;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.blocks.BlockNode;
import me.kuwg.re.ast.nodes.blocks.IBlockContainer;
import me.kuwg.re.ast.types.global.GlobalNode;
import me.kuwg.re.ast.types.load.TopLevelNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.function.RDefFunction;
import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.error.errors.function.RFunctionAlreadyExistError;
import me.kuwg.re.error.errors.function.RMainFunctionError;
import me.kuwg.re.error.errors.range.RRangeTypeError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;
import me.kuwg.re.type.builtin.NoneBuiltinType;
import me.kuwg.re.type.builtin.StrBuiltinType;
import me.kuwg.re.type.generic.GenericType;
import me.kuwg.re.type.iterable.arr.ArrayType;
import me.kuwg.re.type.iterable.range.RangeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FunctionDeclarationNode extends ASTNode implements GlobalNode, IBlockContainer, TopLevelNode {
    private final boolean isGeneric;
    private final String name;
    private final List<FunctionParameter> parameters;
    private final boolean inline;
    private final boolean extern;
    private final BlockNode block;
    private String llvmName;
    private TypeRef returnType;
    private boolean registered = false;

    public FunctionDeclarationNode(
            final String fileName,
            final int line,
            final boolean isGeneric,
            final String name,
            final List<FunctionParameter> parameters,
            final TypeRef returnType,
            final BlockNode block
    ) {
        this(fileName, line, isGeneric, name, parameters, false, false, returnType, block);
    }

    public FunctionDeclarationNode(
            final String fileName,
            final int line,
            final boolean isGeneric,
            final String name,
            final List<FunctionParameter> parameters,
            final boolean inline,
            final boolean extern,
            final TypeRef returnType,
            final BlockNode block
    ) {
        super(fileName, line);
        this.isGeneric = isGeneric;
        this.name = name;

        this.parameters = parameters;
        this.inline = inline;
        this.extern = extern;
        this.returnType = returnType;
        this.block = block.clone();

        if (returnType instanceof RangeType) {
            new RRangeTypeError(fileName, line).raise();
        }
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
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        parameters.replaceAll(param -> new FunctionParameter(param.name(), param.mutable(), replaceGenericType(param.type(), generics, cctx)));
        returnType = replaceGenericType(returnType, generics, cctx);
        block.replaceGenerics(generics, cctx);
    }

    @Override
    public BlockNode getBlock() {
        return block;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        if (!registered) register(cctx);

        boolean main = isMain();

        llvmName = registered ? this.llvmName : getEmissionLLVMName(cctx);

        StringBuilder func = new StringBuilder("; Function declaration\n");


        func.append("define ").append(evalType(returnType, cctx, fileName, line).getLLVMName()).append(" @").append(llvmName).append("(");

        List<TypeRef> types = new ArrayList<>(parameters.size());

        for (int i = 0; i < parameters.size(); i++) {
            var param = parameters.get(i);
            var pt = evalType(param.type(), cctx, fileName, line);
            types.add(i, pt);

            func.append(pt.getLLVMName()).append(" %").append(param.name());
            if (i < parameters.size() - 1) func.append(", ");
        }

        func.append(") ");
        if (inline) {
            cctx.declare("attributes #0 = { alwaysinline }");
            func.append("#0 ");
        }
        func.append("{\n");
        func.append("entry:\n");

        cctx.pushIndent();
        cctx.pushScope();
        cctx.pushFunctionBody();

        for (int i = 0, parametersSize = parameters.size(); i < parametersSize; i++) {
            final FunctionParameter param = parameters.get(i);

            String paramPtr = "%" + param.name() + ".addr";
            TypeRef pt = types.get(i);
            cctx.emit(paramPtr + " = alloca " + pt.getLLVMName());
            cctx.emit("store " + pt.getLLVMName() + " %" + param.name() + ", " + toPtr(pt.getLLVMName()) + paramPtr);

            RVariable paramVar = new RVariable(param.name(), param.mutable(), true, pt, paramPtr, "%" + param.name());
            cctx.addVariable(paramVar);
        }

        String ns = cctx.popNamespace();

        block.compile(cctx);

        if (returnType instanceof NoneBuiltinType) {
            cctx.emit("ret void");
        }

        if (!main) {
            block.checkTypes(cctx, returnType, true);
        }

        if (ns != null) {
            cctx.pushNamespace(ns);
        }

        String body = cctx.popFunctionBody();
        StringBuilder bodySB = new StringBuilder(body);

        if (main && appendMainReturn(bodySB)) {
            bodySB.append(TAB).append("ret ").append(returnType.getLLVMName()).append(" 0\n");
        }

        cctx.popScope();
        cctx.popIndent();

        func.append(bodySB);
        func.append("}\n\n");

        cctx.declare(func.toString());

        if (main && !returnType.equals(BuiltinTypes.INT.getType())) {
            new RMainFunctionError("main() function should return int", fileName, line).raise();
        }
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Function Declaration: ").append(NEWLINE).append(indent).append(TAB).append("Name: ").append(name).append(NEWLINE).append(indent).append(TAB).append("Parameters: ").append(NEWLINE);

        parameters.forEach(p -> p.write(sb, indent + TAB + TAB));

        sb.append(indent).append(TAB).append("Return Type: ").append(returnType.getName()).append(NEWLINE);
        block.write(sb, indent + TAB);
    }

    @Override
    public FunctionDeclarationNode clone() {
        var v = new FunctionDeclarationNode(fileName, line, isGeneric, name, parameters, returnType, block.clone());
        v.registered = registered;
        return v;
    }

    public void register(final CompilationContext cctx) {
        if (registered) return;

        String qualifiedName = getQualifiedName(cctx);
        llvmName = getEmissionLLVMName(cctx);

        List<TypeRef> types = new ArrayList<>(parameters.size());
        for (FunctionParameter parameter : parameters) {
            types.add(parameter.type());
        }

        var oldFunc = cctx.getExact(qualifiedName, types);

        if (oldFunc != null && !(isGeneric && oldFunc.parameters.stream().anyMatch(p -> p.type() instanceof GenericType))) {
            new RFunctionAlreadyExistError("While compiling a function, a function with the same name and parameters was found existing: " + name + types.toString().replace("[", "(").replace("]", ")"), fileName, line).raise();
        }

        RFunction fnObj = new RDefFunction(llvmName, qualifiedName, returnType, parameters);
        cctx.addFunction(fnObj);
        registered = true;
    }

    public String getQualifiedName(CompilationContext cctx) {
        return isMain() ? "main" : cctx.qualify(name);
    }

    private String getLLVMName(CompilationContext cctx) {
        String qualified = getQualifiedName(cctx);

        if (qualified.startsWith("\"") && qualified.endsWith("\"")) {
            String clean = qualified.substring(1, qualified.length() - 1);
            return "\"" + RFunction.makeUnique(clean) + "\"";
        }

        return RFunction.makeUnique(qualified);
    }

    private String getEmissionLLVMName(CompilationContext cctx) {
        if (isMain()) return "main";

        if (extern) {
            String qualified = getQualifiedName(cctx);

            if (qualified.startsWith("\"") && qualified.endsWith("\"")) {
                return qualified;
            }

            return qualified;
        }

        return getLLVMName(cctx);
    }

    public boolean isMain() {
        if (!name.equals("main")) return false;
        if (parameters.isEmpty()) return true;
        if (parameters.size() != 1) return false;
        return parameters.get(0).type() instanceof ArrayType arr && arr.inner() instanceof StrBuiltinType;
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

    public String getLLVMName() {
        return llvmName;
    }

    @Override
    public void load(final CompilationContext cctx) {
        register(cctx);
    }
}
