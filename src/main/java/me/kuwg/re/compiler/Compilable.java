package me.kuwg.re.compiler;

import me.kuwg.re.type.TypeRef;

import java.util.Map;

public interface Compilable {
    void replaceGenerics(Map<String, TypeRef> generics);
    void compile(CompilationContext cctx);
}
