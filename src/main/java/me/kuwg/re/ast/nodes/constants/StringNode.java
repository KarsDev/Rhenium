package me.kuwg.re.ast.nodes.constants;

import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.type.builtin.BuiltinTypes;

import java.util.HashMap;
import java.util.Map;

public class StringNode extends ConstantNode {
    private static final Map<String, String> valueToGlobalName = new HashMap<>();
    private static int globalNameCounter = 0;
    private final String value;
    private final String globalName;

    public StringNode(final int line, final String value) {
        super(line, BuiltinTypes.STR.getType());
        this.value = value;

        String existing = valueToGlobalName.get(value);
        if (existing != null) {
            this.globalName = existing;
        } else {
            this.globalName = "@.str." + globalNameCounter++;
            valueToGlobalName.put(value, this.globalName);
        }
    }

    private void ensureDeclared(CompilationContext cctx) {
        if (cctx.declareOnce(globalName)) {
            String escaped = escapeForLLVM(value);
            int length = value.length() + 1;

            cctx.declare(globalName + " = private unnamed_addr constant [" + length + " x i8] c\"" + escaped + "\\00\"");
        }
    }

    @Override
    public String compileToConstant(final CompilationContext cctx) {
        ensureDeclared(cctx);

        int length = value.length() + 1;

        return "getelementptr inbounds ([" + length + " x i8], [" + length + " x i8]* " + globalName + ", i32 0, i32 0)";
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        ensureDeclared(cctx);

        int length = value.length() + 1;

        String reg = cctx.nextRegister();
        cctx.emit(reg + " = getelementptr inbounds [" + length + " x i8], [" + length + " x i8]* " + globalName + ", i32 0, i32 0");

        return reg;
    }

    private String escapeForLLVM(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '\n' -> sb.append("\\0A");
                case '\r' -> sb.append("\\0D");
                case '\t' -> sb.append("\\09");
                case '\"' -> sb.append("\\22");
                case '\\' -> sb.append("\\5C");
                default -> {
                    if (c < 32 || c > 126) {
                        sb.append(String.format("\\%02X", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RValueMustBeUsedError("String", line).raise();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("String: \"").append(value.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")).append("\"").append(NEWLINE);
    }
}
