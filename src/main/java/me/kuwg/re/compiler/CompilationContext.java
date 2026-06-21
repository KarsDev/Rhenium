package me.kuwg.re.compiler;

import me.kuwg.re.ast.nodes.variable.VariableReference;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.enums.REnum;
import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.compiler.struct.RDefaultStruct;
import me.kuwg.re.compiler.struct.RGenStruct;
import me.kuwg.re.compiler.struct.RStruct;
import me.kuwg.re.compiler.trait.Trait;
import me.kuwg.re.compiler.variable.RStructField;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.error.errors.RInternalError;
import me.kuwg.re.error.errors.function.RMainFunctionError;
import me.kuwg.re.module.ModuleLoadingHelper;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;
import me.kuwg.re.type.iterable.arr.ArrayType;
import me.kuwg.re.type.ptr.PointerType;
import me.kuwg.re.type.struct.AppliedGenStructType;
import me.kuwg.re.type.struct.GenStructType;
import me.kuwg.re.type.struct.StructType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

import static me.kuwg.re.constants.Constants.Lang.WIN;
import static me.kuwg.re.writer.Writeable.TAB;

public final class CompilationContext {
    private static final String ERROR_LINE = "@KNIGHT_OFFSETS = global i32** [8 de";

    private final String fileName;
    private final Map<String, TypeRef> typeMap;
    private final List<String> irCode = new ArrayList<>();
    private final StringBuilder declarations = new StringBuilder();
    private final StringBuilder globalCode = new StringBuilder();
    private final Deque<StringBuilder> codeStack = new ArrayDeque<>();
    private final Map<String, RVariable> variables = new HashMap<>();
    private final RFunctions functions = new RFunctions();
    private final List<String> includedModules = new ArrayList<>();
    private final Stack<LoopContext> loopStack = new Stack<>();
    private final Stack<Map<String, RVariable>> scopeStack = new Stack<>();
    private final Map<String, RDefaultStruct> structs = new HashMap<>();
    private final Stack<String> catchScopeStack = new Stack<>();
    private final List<Path> nativeCPPModules = new ArrayList<>();
    private final Set<String> declaredIR = new LinkedHashSet<>();
    private final Set<String> declaredStructs = new LinkedHashSet<>();
    private final Set<String> declaredGlobals = new HashSet<>();
    private final Deque<String> namespaceStack = new ArrayDeque<>();
    private final Map<String, REnum> enums = new HashMap<>();
    private final Map<String, Trait> traits = new HashMap<>();
    private int registerCounter = 1;
    private int indentLevel = 1;
    private int labelCounter = 0;

    public CompilationContext(final String fileName, Map<String, TypeRef> typeMap) {
        this.fileName = fileName;
        this.typeMap = typeMap;

        irCode.add("; Generated LLVM IR\n\n");
        codeStack.push(globalCode);
    }

    public void pushFunctionBody() {
        codeStack.push(new StringBuilder());
    }

    public String popFunctionBody() {
        return codeStack.pop().toString();
    }

    public String nextRegister() {
        return "%" + registerCounter++;
    }

    public void emit(String s) {
        if (s.contains(ERROR_LINE)) throw new RuntimeException();
        if (s.contains(" ptr*")) {
            System.err.println("WARNING: emitted invalid LLVM opaque pointer syntax:");
            System.err.println(s);

            s = s.replace("ptr*", "ptr");
        }
        if (s.strip().matches("^[A-Za-z_][A-Za-z0-9_]*_[0-9]+:$")) registerCounter++;
        Objects.requireNonNull(codeStack.peek()).append(TAB.repeat(indentLevel)).append(s).append('\n');
    }

    public void pushIndent() {
        indentLevel++;
    }

    public void popIndent() {
        if (indentLevel > 0) indentLevel--;
    }

    public boolean emptyScope() {
        return scopeStack.isEmpty();
    }

    public void pushScope() {
        scopeStack.push(new HashMap<>());
    }

    public void popScope() {
        if (!scopeStack.isEmpty()) scopeStack.pop();
    }

    public void addVariable(RVariable v) {
        if (!scopeStack.isEmpty()) {
            scopeStack.peek().put(v.name(), v);
        } else {
            pushScope();
            addVariable(v);
        }
    }

    public void addGlobal(RVariable v) {
        variables.put(v.name(), v);
    }

    public RVariable getVariable(String name) {
        for (int i = scopeStack.size() - 1; i >= 0; i--) {
            var v = scopeStack.get(i).get(name);
            if (v != null) return v;
        }

        var direct = variables.get(name);
        if (direct != null) return direct;

        if (name.contains("$$")) return null;

        String ns = currentNamespace();
        while (true) {
            String candidate = ns.isEmpty() ? name : ns + "$$" + name;
            RVariable v = variables.get(candidate);
            if (v != null) return v;

            int idx = ns.lastIndexOf("::");
            if (idx < 0) break;
            ns = ns.substring(0, idx);
        }

        return null;
    }

    public void addFunction(RFunction f) {
        functions.add(f);
    }

    public RFunction getFunction(String name, List<TypeRef> parameters) {
        return functions.get(name, parameters);
    }

    public @SuppressWarnings("unused") void writeAllFunctions() {
        functions.writeAll();
    }

    public RFunction getExact(String name, List<TypeRef> parameters) {
        return functions.getExact(name, parameters);
    }

    public void addStruct(boolean builtin, String name, final List<String> inherited, TypeRef type, List<RStructField> fields) {
        if (type instanceof GenStructType) {
            structs.put(name, new RGenStruct(fileName, inherited, type, fields));
        } else {
            structs.put(name, new RStruct(fileName, builtin, inherited, type, fields));
        }
    }

    public RDefaultStruct getStruct(String name) {
        return structs.get(name);
    }

    public void pushTryCatchScope(String catchLabel) {
        catchScopeStack.push(catchLabel);
    }

    public String popTryCatchScope() {
        if (catchScopeStack.isEmpty()) return null;
        return catchScopeStack.pop();
    }

    public void addNativeCPPModule(Path path) {
        nativeCPPModules.add(path);
    }

    public boolean declareOnce(String name) {
        return declaredGlobals.add(name);
    }

    public void addIR(String ir) {
        String[] lines = ir.split("\n");
        for (String line : lines) {
            String trimmed = line.split(";")[0].strip();
            if (trimmed.isEmpty()) continue;

            if (!declaredIR.contains(trimmed)) {
                irCode.add(line);
                declaredIR.add(trimmed);
            }
        }
    }

    public void declare(String declaration) {
        if (declaration.contains(ERROR_LINE)) throw new RuntimeException();
        declarations.append(declaration).append('\n');
    }

    public void include(int line, String sourceFile, String name, String pkg) {
        if (includedModules.contains(name)) return;
        String str = name + (pkg == null ? "" : " in " + pkg);
        declare(" ; USING MODULE " + str);
        includedModules.add(name);
        ModuleLoadingHelper.loadModule(fileName, line, typeMap, sourceFile, name, pkg, this);
    }

    public Stack<LoopContext> getLoopStack() {
        return loopStack;
    }

    public String nextLabel(String prefix) {
        return prefix + "_" + (labelCounter++);
    }

    public void addTypes(Map<String, TypeRef> typeMap) {
        this.typeMap.putAll(typeMap);
    }

    public boolean isStructDeclared(String struct) {
        return declaredStructs.contains(struct);
    }

    public void markStructDeclared(String struct) {
        declaredStructs.add(struct);
    }

    public String ensureValue(ValueNode node, String reg) {
        if (!(node.getType() instanceof StructType)) return reg;

        if (node instanceof VariableReference vr) {
            var var = vr.getVariable(this);
            if (var != null && reg.equals(var.addrReg())) {
                String loaded = nextRegister();
                emit(loaded + " = load " + node.getType().getLLVMName() + ", " + node.getType().getLLVMName() + "* " + reg);
                return loaded;
            }
        }

        return reg;
    }

    public void pushNamespace(String ns) {
        namespaceStack.push(ns);
    }

    public String popNamespace() {
        if (!namespaceStack.isEmpty()) return namespaceStack.pop();
        return null;
    }

    public String currentNamespace() {
        if (namespaceStack.isEmpty()) return "";
        var it = namespaceStack.descendingIterator();
        StringBuilder sb = new StringBuilder();
        while (it.hasNext()) {
            if (!sb.isEmpty()) sb.append("::");
            sb.append(it.next());
        }
        return sb.toString();
    }

    public String qualify(String name) {
        String ns = currentNamespace();
        return ns.isEmpty() ? name : ns + "$$" + name;
    }

    public String compileAndGet(File llvmFile, File executableFile, List<String> clangArgs) throws IOException {
        var main = getFunction("main", List.of());

        if (main == null) {
            noMain(llvmFile);
        } else {
            withMain(main, llvmFile);
        }

        return getCompilationCommand(llvmFile.getAbsolutePath(), executableFile.getAbsolutePath(), clangArgs);
    }

    private void noMain(File output) throws IOException {
        StringBuilder finalOutput = new StringBuilder();

        finalOutput.append("\n; IR DECLARATIONS\n");
        for (final String s : irCode) {
            finalOutput.append(s).append("\n");
        }

        finalOutput.append("\n; DECLARATIONS\n");
        finalOutput.append(declarations);

        finalOutput.append("\ndefine i32 @main() {\n");
        finalOutput.append("entry:\n");
        finalOutput.append(globalCode);
        finalOutput.append(TAB + "ret i32 0\n}\n\n; End of module\n");

        try (FileWriter writer = new FileWriter(output)) {
            writer.write(finalOutput.toString());
        }
    }

    private void withMain(RFunction main, File output) throws IOException {
        if (!main.returnType().equals(BuiltinTypes.INT.getType())) {
            throw new RInternalError("internal error: main function should return int");
        }

        if (!globalCode.isEmpty() && containsInvalidGlobalCode(globalCode.toString())) {
            new RMainFunctionError("You cannot declare code if you declared the main function", fileName, -1).raise();
            return;
        }

        StringBuilder finalOutput = new StringBuilder();

        finalOutput.append("\n; IR DECLARATIONS\n");
        for (final String s : irCode) {
            finalOutput.append(s).append("\n");
        }

        finalOutput.append("\n; DECLARATIONS\n");
        finalOutput.append(declarations);

        finalOutput.append(globalCode);

        finalOutput.append("; End of module\n");

        try (FileWriter writer = new FileWriter(output)) {
            writer.write(finalOutput.toString());
        }
    }

    private boolean containsInvalidGlobalCode(String code) {
        return Arrays.stream(code.split("\n")).map(String::trim).filter(line -> !line.isEmpty()).filter(line -> !line.startsWith(";")).anyMatch(line -> {
            String[] parts = line.split("\\s+");
            return parts.length == 0 || !parts[0].contains("_global_load");
        });
    }

    public void addEnum(String name, REnum rEnum) {
        enums.put(name, rEnum);
    }

    public REnum getEnum(String name) {
        return enums.get(name);
    }

    public boolean isEnumDeclared(String name) {
        return enums.containsKey(name);
    }

    public void addTrait(String name, Trait trait) {
        traits.put(name, trait);
    }

    public Trait getTrait(String name) {
        return traits.get(name);
    }

    public boolean isTraitDeclared(String name) {
        return traits.containsKey(name);
    }

    private String getCompilationCommand(String llvmFile, String executableFile, List<String> clangArgs) {
        final var quote = (Function<String, String>) s -> "\"" + s + "\"";

        final String extraClangArgs =
                (clangArgs == null || clangArgs.isEmpty())
                        ? ""
                        : " " + String.join(" ", clangArgs);

        String tempBase = executableFile;
        int lastDot = tempBase.lastIndexOf('.');
        if (lastDot > 0) tempBase = tempBase.substring(0, lastDot);

        String deleteCmd = WIN ? "del /f " : "rm -f ";
        String and = " && ";

        StringBuilder cmd = new StringBuilder();
        List<String> bcFiles = new ArrayList<>();

        for (Path p : nativeCPPModules) {
            String src = p.toString();
            String bc = src + ".bc";

            cmd.append("clang++ -O3 -march=native -mtune=native -flto -c -emit-llvm -std=c++17")
                    .append(extraClangArgs)
                    .append(" ")
                    .append(quote.apply(src))
                    .append(" -o ")
                    .append(quote.apply(bc))
                    .append(and);

            bcFiles.add(bc);
        }

        String linked = tempBase + ".linked.bc";

        if (!bcFiles.isEmpty()) {
            cmd.append("llvm-link -o ")
                    .append(quote.apply(linked))
                    .append(" ")
                    .append(quote.apply(llvmFile));

            for (String bc : bcFiles) {
                cmd.append(" ").append(quote.apply(bc));
            }

            cmd.append(and);
        } else {
            linked = llvmFile;
        }

        String optimized = tempBase + ".opt.bc";

        cmd.append("opt -passes=\"default<O3>\" ")
                .append(quote.apply(linked))
                .append(" -o ")
                .append(quote.apply(optimized))
                .append(and);

        cmd.append("clang++ ")
                .append("-O3 ")
                .append("-march=native ")
                .append("-mtune=native ")
                .append("-funroll-loops ")
                .append("-fomit-frame-pointer ")
                .append("-flto -fuse-ld=lld ")
                .append("-fno-exceptions ")
                .append("-fno-rtti ")
                .append(extraClangArgs)
                .append(" ")
                .append(quote.apply(optimized))
                .append(" -o ")
                .append(quote.apply(executableFile))
                .append(and);

        cmd.append(deleteCmd).append(quote.apply(optimized)).append(" ");

        if (!bcFiles.isEmpty()) {
            cmd.append(quote.apply(linked)).append(" ");
            for (String bc : bcFiles) {
                cmd.append(quote.apply(bc)).append(" ");
            }
        }

        return cmd.toString().trim();
    }

    public TypeRef resolveConcrete(TypeRef t) {
        if (t instanceof AppliedGenStructType a) {
            List<TypeRef> args = a.args().stream().map(this::resolveConcrete).toList();
            RGenStruct gen = (RGenStruct) getStruct(a.base().name());
            return gen.instantiate(args, this).type();
        }
        if (t instanceof PointerType p) return new PointerType(resolveConcrete(p.inner()));
        if (t instanceof ArrayType a) return new ArrayType(a.size(), resolveConcrete(a.inner()));
        return t;
    }
}
