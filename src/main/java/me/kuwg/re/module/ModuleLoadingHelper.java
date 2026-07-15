package me.kuwg.re.module;

import me.kuwg.re.ast.AST;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.module.RModuleCouldNotBeLoadedError;
import me.kuwg.re.error.errors.module.RModuleNotFoundError;
import me.kuwg.re.parser.ASTParser;
import me.kuwg.re.resource.ResourceLoader;
import me.kuwg.re.token.Tokenizer;
import me.kuwg.re.type.TypeRef;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ModuleLoadingHelper {
    private final Set<String> loadedModules = new HashSet<>();
    private final Set<String> loadingModules = new HashSet<>();

    private final Set<String> collectedModules = new HashSet<>();
    private final Set<String> collectingModules = new HashSet<>();
    private static String fileKey(Path file) {
        return "file:" + file.toAbsolutePath().normalize();
    }

    private static String nativeKey(String name) {
        return "native:" + name;
    }

    public void loadModule(final String fileName, int line, Map<String, TypeRef> typeMap,
                           String sourceFile, String name, String pkg, CompilationContext cctx) {
        if (pkg == null) {
            loadNativeModule(fileName, line, typeMap, name, cctx);
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
            new RModuleNotFoundError(pkg + "->" + name, fileName, line).raise();
            return;
        }

        Path file = base.resolve(name + ".re").normalize().toAbsolutePath();
        String key = fileKey(file);

        if (loadedModules.contains(key) || loadingModules.contains(key)) {
            return;
        }

        loadingModules.add(key);
        try {
            if (!Files.exists(file)) {
                new RModuleNotFoundError(pkg + "->" + name, fileName, line).raise();
                return;
            }

            String src;
            try {
                src = Files.readString(file);
            } catch (IOException e) {
                new RModuleCouldNotBeLoadedError(pkg + "->" + name, fileName, line).raise();
                return;
            }

            load(typeMap, file.toString(), src, cctx);
            loadedModules.add(key);
        } finally {
            loadingModules.remove(key);
        }
    }

    private void loadNativeModule(final String fileName, int line, Map<String, TypeRef> typeMap, String name, CompilationContext cctx) {
        String key = nativeKey(name);

        if (loadedModules.contains(key) || loadingModules.contains(key)) {
            return;
        }

        loadingModules.add(key);
        try {
            String src = ResourceLoader.loadResourceAsString("/natives/modules/" + name + ".re");
            if (src == null) {
                new RModuleNotFoundError(name, fileName, line).raise();
                return;
            }

            load(typeMap, name, src, cctx);
            loadedModules.add(key);
        } finally {
            loadingModules.remove(key);
        }
    }

    private void load(Map<String, TypeRef> typeMap, String module, String src, CompilationContext cctx) {
        var tokens = Tokenizer.tokenize(src);
        ASTParser parser = new ASTParser(module, tokens, typeMap, this);
        cctx.addTypes(parser.typeMap);
        AST ast = parser.parse();
        ast.compile(cctx);
    }

    public Map<String, TypeRef> collectModuleTypes(final String fileName, int line,
                                                   String sourceFile, String name, String pkg,
                                                   Map<String, TypeRef> typeMap) {
        if (pkg == null) {
            return collectNativeModuleTypes(fileName, line, name, typeMap);
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
            return new RModuleNotFoundError(pkg + "->" + name, fileName, line).raise();
        }

        Path file = base.resolve(name + ".re").normalize().toAbsolutePath();
        String key = fileKey(file);

        if (collectedModules.contains(key) || collectingModules.contains(key)) {
            return typeMap;
        }

        collectingModules.add(key);
        try {
            if (!Files.exists(file)) {
                return new RModuleNotFoundError(pkg + "->" + name, fileName, line).raise();
            }

            String src;
            try {
                src = Files.readString(file);
            } catch (IOException e) {
                return new RModuleCouldNotBeLoadedError(pkg + "->" + name, fileName, line).raise();
            }

            Map<String, TypeRef> out = collectTypes(file.toString(), src, typeMap);
            collectedModules.add(key);
            return out;
        } finally {
            collectingModules.remove(key);
        }
    }

    private Map<String, TypeRef> collectNativeModuleTypes(final String fileName, int line,
                                                          String name, Map<String, TypeRef> typeMap) {
        String key = nativeKey(name);

        if (collectedModules.contains(key) || collectingModules.contains(key)) {
            return typeMap;
        }

        collectingModules.add(key);
        try {
            String src = ResourceLoader.loadResourceAsString("/natives/modules/" + name + ".re");
            if (src == null) {
                return new RModuleNotFoundError(name, fileName, line).raise();
            }

            Map<String, TypeRef> out = collectTypes(name, src, typeMap);
            collectedModules.add(key);
            return out;
        } finally {
            collectingModules.remove(key);
        }
    }

    private Map<String, TypeRef> collectTypes(String module, String src, Map<String, TypeRef> typeMap) {
        var tokens = Tokenizer.tokenize(src);
        ASTParser parser = new ASTParser(module, tokens, typeMap, this);

        parser.collectTypesOnly();
        return parser.typeMap;
    }
}