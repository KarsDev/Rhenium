package me.kuwg.re.module;

import me.kuwg.re.ast.AST;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.RInternalError;
import me.kuwg.re.error.errors.module.RModuleCouldNotBeLoadedError;
import me.kuwg.re.error.errors.module.RModuleNotFoundError;
import me.kuwg.re.parser.ASTParser;
import me.kuwg.re.resource.ResourceLoader;
import me.kuwg.re.token.Tokenizer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ModuleLoadingHelper {
    private ModuleLoadingHelper() {
        throw new RInternalError();
    }

    public static void loadModule(int line, String name, String pkg, CompilationContext cctx) {
        if (pkg == null) {
            loadNativeModule(line, name, cctx);
            return;
        }

        var file = new File(pkg, name + ".re");

        if (!file.exists()) {
            new RModuleNotFoundError(pkg + "->" + name, line).raise();
        }

        String src;
        try {
            src = new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            new RModuleCouldNotBeLoadedError(pkg + "->" + name, line).raise();
            return;
        }

        load(file.getName(), src, cctx);
    }

    private static void loadNativeModule(int line, String name, CompilationContext cctx) {
        String src = ResourceLoader.loadResourceAsString("/natives/modules/" + name + ".re");
        if (src == null) {
            new RModuleNotFoundError(name, line).raise();
            return;
        }

        load(name, src, cctx);
    }

    private static void load(String module, String src, CompilationContext cctx) {
        var tokens = Tokenizer.tokenize(src);
        ASTParser parser = new ASTParser(module, tokens, false);
        AST ast = parser.parse();

        ast.compile(cctx);
    }
}
