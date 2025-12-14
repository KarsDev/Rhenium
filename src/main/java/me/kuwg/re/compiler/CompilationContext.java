package me.kuwg.re.compiler;

import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.compiler.struct.RStruct;
import me.kuwg.re.compiler.variable.RStructField;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.error.errors.RInternalError;
import me.kuwg.re.error.errors.function.RMainFunctionError;
import me.kuwg.re.module.ModuleLoadingHelper;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

import static me.kuwg.re.constants.Constants.Lang.WIN;
import static me.kuwg.re.writer.Writeable.TAB;

public @SuppressWarnings("unused")
final class CompilationContext {
    private final List<String> irCode = new ArrayList<>();
    private final StringBuilder declarations = new StringBuilder();
    private final StringBuilder globalCode = new StringBuilder();
    private final Deque<StringBuilder> codeStack = new ArrayDeque<>();
    private final Map<String, RVariable> variables = new HashMap<>();
    private final RFunctions functions = new RFunctions();
    private final List<String> includedModules = new ArrayList<>();
    private final Stack<LoopContext> loopStack = new Stack<>();
    private final Stack<Map<String, RVariable>> scopeStack = new Stack<>();
    private final Map<String, RStruct> structs = new HashMap<>();
    private final Stack<String> catchScopeStack = new Stack<>();
    private final List<Path> nativeCPPModules = new ArrayList<>();
    private int registerCounter = 1;
    private int indentLevel = 1;
    private int labelCounter = 0;

    public CompilationContext() {
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
        if (s.strip().matches("^[A-Za-z_][A-Za-z0-9_]*_[0-9]+:$")) registerCounter++;
        Objects.requireNonNull(codeStack.peek()).append(TAB.repeat(indentLevel)).append(s).append('\n');
    }

    public void pushIndent() {
        indentLevel++;
    }

    public void popIndent() {
        if (indentLevel > 0) indentLevel--;
    }

    public int getIndentLevel() {
        return indentLevel;
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
            variables.put(v.name(), v);
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
        return variables.get(name);
    }

    public void addFunction(RFunction f) {
        functions.add(f);
    }

    public RFunction getFunction(String name, List<TypeRef> parameters) {
        return functions.get(name, parameters);
    }

    public List<RFunction> getFunctions(String name) {
        return functions.get(name);
    }

    public void addStruct(String name, TypeRef type, List<RStructField> fields) {
        structs.put(name, new RStruct(type, fields, new ArrayList<>()));
    }

    public RStruct getStruct(String name) {
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

    public void addIR(String ir) {
        String[] lines = ir.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith(";")) {
                irCode.add(line);
                continue;
            }

            if (irCode.contains(line)) continue;
            irCode.add(line);
        }
    }

    public void declare(String declaration) {
        declarations.append(declaration).append('\n');
    }

    public void include(int line, String sourceFile, String name, String pkg) {
        if (includedModules.contains(name)) return;
        String str = name + (pkg == null ? "" : " in " + pkg);
        declare(" ; USING MODULE " + str);
        includedModules.add(name);
        ModuleLoadingHelper.loadModule(line, sourceFile, name, pkg, this);
    }

    public Stack<LoopContext> getLoopStack() {
        return loopStack;
    }

    public String nextLabel(String prefix) {
        return prefix + "_" + (labelCounter++);
    }

    public String compileAndGet(File output, List<String> clangArgs) throws IOException {
        var main = getFunction("main", List.of());

        if (main == null) {
            noMain(output);
        } else {
            withMain(main, output);
        }

        return getCompilationCommand(output.getAbsolutePath(), clangArgs);
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

        if (!globalCode.isEmpty()) {
            new RMainFunctionError("You cannot declare code if you declared the main function", -1).raise();
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

    private String getCompilationCommand(String name, List<String> clangArgs) {
        final var quote = (Function<String, String>) s -> "\"" + s + "\"";
        final String extraClangArgs =
                (clangArgs == null || clangArgs.isEmpty())
                        ? ""
                        : " " + String.join(" ", clangArgs);

        String exeBase = name;
        int lastDot = exeBase.lastIndexOf('.');
        if (lastDot > 0) exeBase = exeBase.substring(0, lastDot);

        String finalExe = exeBase + (WIN ? ".exe" : ".out");
        String deleteCmd = WIN ? "del /f " : "rm -f ";
        String and = " && ";

        StringBuilder cmd = new StringBuilder();
        List<String> bcFiles = new ArrayList<>();

        for (Path p : nativeCPPModules) {
            String src = p.toString();
            String bc = src + ".bc";

            cmd.append("clang++ -O2 -c -emit-llvm -std=c++17")
                    .append(extraClangArgs)
                    .append(" ")
                    .append(quote.apply(src))
                    .append(" -o ")
                    .append(quote.apply(bc))
                    .append(and);

            bcFiles.add(bc);
        }

        String linked = exeBase + ".linked.bc";

        if (!bcFiles.isEmpty()) {
            cmd.append("llvm-link -o ")
                    .append(quote.apply(linked))
                    .append(" ")
                    .append(quote.apply(name));

            for (String bc : bcFiles) {
                cmd.append(" ").append(quote.apply(bc));
            }

            cmd.append(and);

            cmd.append("clang++")
                    .append(extraClangArgs)
                    .append(" ")
                    .append(quote.apply(linked))
                    .append(" -o ")
                    .append(quote.apply(finalExe))
                    .append(and);

            cmd.append(deleteCmd).append(quote.apply(linked)).append(" ");
            for (String bc : bcFiles) {
                cmd.append(quote.apply(bc)).append(" ");
            }

        } else {
            // Direct compile
            cmd.append("clang++")
                    .append(extraClangArgs)
                    .append(" ")
                    .append(quote.apply(name))
                    .append(" -o ")
                    .append(quote.apply(finalExe))
                    .append(and);

            String bc = name.replace(".ll", ".bc");
            cmd.append(deleteCmd).append(quote.apply(bc));
        }

        return cmd.toString().trim();
    }

    public Map<String, RVariable> getVariables() {
        return variables;
    }
}
