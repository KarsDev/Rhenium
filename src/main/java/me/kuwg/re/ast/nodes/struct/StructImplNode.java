package me.kuwg.re.ast.nodes.struct;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.blocks.BlockNode;
import me.kuwg.re.ast.nodes.function.declaration.BuiltinFunctionDeclarationNode;
import me.kuwg.re.ast.nodes.function.declaration.FunctionDeclarationNode;
import me.kuwg.re.ast.nodes.function.declaration.FunctionParameter;
import me.kuwg.re.ast.types.global.GlobalNode;
import me.kuwg.re.ast.types.load.TopLevelNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.compiler.struct.RConstructor;
import me.kuwg.re.compiler.struct.RDefaultStruct;
import me.kuwg.re.error.errors.RInternalError;
import me.kuwg.re.error.errors.struct.RStructUndefinedError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;
import me.kuwg.re.type.ptr.PointerType;
import me.kuwg.re.type.struct.StructType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class StructImplNode extends ASTNode implements GlobalNode, TopLevelNode {
    private final StructType struct;
    private final List<RConstructor> constructors;
    private final List<ASTNode> functions;
    private final BlockNode destructor;

    private final List<PreparedFunction> loadedConstructors = new ArrayList<>();
    private final List<PreparedFunction> loadedFunctions = new ArrayList<>();
    private final List<PreparedBuiltinFunction> loadedBuiltinFunctions = new ArrayList<>();
    private final List<RFunction> eagerFunctions = new ArrayList<>();
    private PreparedFunction loadedDestructor;
    private boolean loaded = false;

    public StructImplNode(final String fileName, final int line, final StructType struct, final List<RConstructor> constructors, final List<ASTNode> functions, final BlockNode destructor) {
        super(fileName, line);
        this.struct = struct;
        this.constructors = constructors;
        this.functions = functions;
        this.destructor = destructor;
    }

    public static String generateName(String struct, String name) {
        String raw = struct + "." + name;
        String escaped = raw.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + escaped + "\"";
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        constructors.forEach(c -> c.block().replaceGenerics(generics, cctx));
        functions.forEach(f -> f.replaceGenerics(generics, cctx));
        destructor.replaceGenerics(generics, cctx);
    }

    @Override
    public void load(final CompilationContext cctx) {
        if (loaded) return;

        RDefaultStruct cctxStruct = cctx.getStruct(struct.name());
        if (cctxStruct == null) {
            new RStructUndefinedError(struct.name(), fileName, line).raise();
            return;
        }

        for (RConstructor constructor : constructors) {
            String lookupName = methodName(cctxStruct, constructor.llvmName());
            List<FunctionParameter> params = addSelfParam(cctxStruct, constructor.parameters());

            FunctionDeclarationNode ctor = new FunctionDeclarationNode(
                    fileName,
                    constructor.block().getNodes().get(0).getLine(),
                    false,
                    lookupName,
                    params,
                    BuiltinTypes.NONE.getType(),
                    constructor.block()
            );

            ctor.register(cctx);
            loadedConstructors.add(new PreparedFunction(ctor, lookupName));
        }

        for (ASTNode fn : functions) {
            if (fn instanceof FunctionDeclarationNode dec) {
                String lookupName = methodName(cctxStruct, dec.getName());
                List<FunctionParameter> params = addSelfParam(cctxStruct, dec.getParameters());

                FunctionDeclarationNode renamed = new FunctionDeclarationNode(
                        fileName,
                        dec.getLine(),
                        false,
                        lookupName,
                        params,
                        dec.getReturnType(),
                        dec.getBlock()
                );

                renamed.load(cctx);
                loadedFunctions.add(new PreparedFunction(renamed, lookupName));
                continue;
            }

            if (fn instanceof BuiltinFunctionDeclarationNode blt) {
                String lookupName = methodName(cctxStruct, blt.getName());
                List<FunctionParameter> params = addSelfParam(cctxStruct, blt.getParameters());

                BuiltinFunctionDeclarationNode renamed = new BuiltinFunctionDeclarationNode(
                        fileName,
                        blt.getLine(),
                        true,
                        lookupName,
                        params,
                        blt.getReturnType(),
                        blt.getLlvmBody()
                );

                renamed.load(cctx);
                loadedBuiltinFunctions.add(new PreparedBuiltinFunction(renamed, lookupName));
                List<TypeRef> types = extractTypes(params);
                RFunction compiled = cctx.getFunction(lookupName, types);
                if (compiled != null) {
                    eagerFunctions.add(compiled);
                }
                continue;
            }

            throw new RInternalError("Not function declaration: " + fn);
        }

        if (destructor != null) {
            List<FunctionParameter> params = List.of(
                    new FunctionParameter(
                            "self",
                            false,
                            cctxStruct.type()
                    )
            );

            String fname = "\"" + struct.getMangledName() + ":destructor\"";

            FunctionDeclarationNode dtor = new FunctionDeclarationNode(
                    fileName,
                    line,
                    false,
                    fname,
                    params,
                    BuiltinTypes.NONE.getType(),
                    destructor
            );

            dtor.load(cctx);
            loadedDestructor = new PreparedFunction(dtor, fname);
        }

        loaded = true;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        if (!loaded) {
            load(cctx);
        }

        cctx.emit("; Struct impl");

        RDefaultStruct cctxStruct = cctx.getStruct(struct.name());
        if (cctxStruct == null) {
            new RStructUndefinedError(struct.name(), fileName, line).raise();
            return;
        }

        for (PreparedFunction constructor : loadedConstructors) {
            constructor.node.compile(cctx);

            RFunction compiledCtor = cctx.getFunction(
                    constructor.lookupName,
                    extractTypes(constructor.node.getParameters())
            );

            if (compiledCtor != null) {
                cctxStruct.constructors().add(compiledCtor);
            }
        }

        for (PreparedFunction function : loadedFunctions) {
            function.node.compile(cctx);

            RFunction compiled = cctx.getFunction(
                    function.lookupName,
                    extractTypes(function.node.getParameters())
            );

            if (compiled != null) {
                cctxStruct.functions().add(compiled);
            }
        }

        for (PreparedBuiltinFunction function : loadedBuiltinFunctions) {
            function.node.compile(cctx);

            RFunction compiled = cctx.getFunction(
                    function.lookupName,
                    extractTypes(function.node.getParameters())
            );

            if (compiled != null) {
                cctxStruct.functions().add(compiled);
            }
        }

        for (RFunction fn : eagerFunctions) {
            cctxStruct.functions().add(fn);
        }

        if (loadedDestructor != null) {
            loadedDestructor.node.compile(cctx);

            RFunction compiled = cctx.getFunction(
                    loadedDestructor.lookupName,
                    extractTypes(loadedDestructor.node.getParameters())
            );

            if (compiled != null) {
                cctxStruct.setDestructor(compiled);
            }
        }
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Struct Impl: ")
                .append(NEWLINE)
                .append(TAB).append("Name: ").append(struct.name()).append(NEWLINE)
                .append(TAB).append("Functions:").append(NEWLINE);
        functions.forEach(f -> f.write(sb, indent + TAB + TAB));
    }

    private List<FunctionParameter> addSelfParam(RDefaultStruct struct, List<FunctionParameter> original) {
        List<FunctionParameter> newParams = new ArrayList<>(original.size() + 1);
        newParams.add(new FunctionParameter("self", false, new PointerType(struct.type())));
        newParams.addAll(original);
        return newParams;
    }

    private List<TypeRef> extractTypes(List<FunctionParameter> params) {
        return params.stream().map(FunctionParameter::type).toList();
    }

    private String methodName(RDefaultStruct struct, String name) {
        return generateName(struct.type().getName(), name);
    }

    @Override
    public StructImplNode clone() {
        List<RConstructor> constructorsCloned = new ArrayList<>();
        IntStream.range(0, constructors.size()).forEach(i -> constructorsCloned.add(i, constructors.get(i).clone()));

        List<ASTNode> functionsCloned = new ArrayList<>();
        IntStream.range(0, functions.size()).forEach(i -> functionsCloned.add(i, functions.get(i).clone()));

        return new StructImplNode(fileName, line, struct, constructorsCloned, functionsCloned, destructor.clone());
    }

    private static final class PreparedFunction {
        private final FunctionDeclarationNode node;
        private final String lookupName;

        private PreparedFunction(FunctionDeclarationNode node, String lookupName) {
            this.node = node;
            this.lookupName = lookupName;
        }
    }

    private static final class PreparedBuiltinFunction {
        private final BuiltinFunctionDeclarationNode node;
        private final String lookupName;

        private PreparedBuiltinFunction(BuiltinFunctionDeclarationNode node, String lookupName) {
            this.node = node;
            this.lookupName = lookupName;
        }
    }
}
