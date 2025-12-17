package me.kuwg.re.module;

import me.kuwg.re.ast.AST;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.RInternalError;
import me.kuwg.re.error.errors.module.RModuleCouldNotBeLoadedError;
import me.kuwg.re.error.errors.module.RModuleNotFoundError;
import me.kuwg.re.parser.ASTParser;
import me.kuwg.re.resource.ResourceLoader;
import me.kuwg.re.token.Tokenizer;
import me.kuwg.re.type.TypeRef;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ModuleLoadingHelper {
    private ModuleLoadingHelper() {
        throw new RInternalError();
    }

    public static void loadModule(int line, Map<String, TypeRef> typeMap, String sourceFile, String name, String pkg, CompilationContext cctx) {
        if (pkg == null) {
            loadNativeModule(line, typeMap, name, cctx);
            return;
        }

        final Path srcPath = Path.of(sourceFile);
        Path base = pkg.equals("self") ? srcPath.getParent() : Path.of(pkg);

        if (pkg.equals("..")) {
            Path sourcePath = srcPath.getParent();
            if (sourcePath != null) {
                base = sourcePath.getParent();
            }
        }


        if (base == null) {
            new RModuleNotFoundError(pkg + "->" + name, line).raise();
            return;
        }

        Path file = base.resolve(name + ".re");

        if (!Files.exists(file)) {
            new RModuleNotFoundError(pkg + "->" + name, line).raise();
            return;
        }

        String src;
        try {
            src = Files.readString(file);
        } catch (IOException e) {
            new RModuleCouldNotBeLoadedError(pkg + "->" + name, line).raise();
            return;
        }

        load(typeMap, file.getFileName().toString(), src, cctx);
    }

    private static void loadNativeModule(int line, Map<String, TypeRef> typeMap, String name, CompilationContext cctx) {
        String src = ResourceLoader.loadResourceAsString("/natives/modules/" + name + ".re");
        if (src == null) {
            new RModuleNotFoundError(name, line).raise();
            return;
        }

        load(typeMap, name, src, cctx);
    }

    private static void load(Map<String, TypeRef> typeMap, String module, String src, CompilationContext cctx) {
        var tokens = Tokenizer.tokenize(src);
        ASTParser parser = new ASTParser(module, tokens, typeMap);

        cctx.addTypes(parser.typeMap);

        AST ast = parser.parse();

        ast.compile(cctx);
    }
}
