package me.kuwg.re.ast.nodes.extern;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.types.global.GlobalNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.error.errors.natv.RNativeCPPError;
import me.kuwg.re.resource.ResourceLoader;
import me.kuwg.re.type.TypeRef;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class NativeCPPNode extends ASTNode implements GlobalNode {
    private final boolean isNative;
    private final String name;
    private final List<RFunction> functions;

    public NativeCPPNode(final int line, final boolean isNative, final String name, final List<RFunction> functions) {
        super(line);
        this.isNative = isNative;
        this.name = name;
        this.functions = functions;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
    }

    @Override
    public void compile(final CompilationContext cctx) {
        functions.forEach(cctx::addFunction);

        for (RFunction f : functions) {
            StringBuilder decl = new StringBuilder();

            decl.append("declare ")
                    .append(f.returnType().getLLVMName())
                    .append(" @")
                    .append(f.name())
                    .append("(");

            for (int i = 0; i < f.parameters().size(); i++) {
                var param = f.parameters().get(i);
                decl.append(param.type().getLLVMName());
                if (i < f.parameters().size() - 1) {
                    decl.append(", ");
                }
            }

            decl.append(")");

            cctx.declare(decl.toString());
        }

        Path path;
        if (isNative)
            path = ResourceLoader.getResourcePath("/natives/cpp/" + name + ".cpp");
        else {
            File f = new File(name);
            if (!f.exists()) {
                new RNativeCPPError("Native C++ module not found: " + name, line).raise();
            }
            path = f.toPath();
        }
        if (path == null) {
            new RNativeCPPError("Native C++ module not found: " + name, line).raise();
        }
        cctx.addNativeCPPModule(path);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Native CPP: ").append(name).append(NEWLINE);
        functions.forEach(f -> sb.append(indent).append(TAB).append(f));
    }

    @Override
    public NativeCPPNode clone() {
        return this;
    }
}
