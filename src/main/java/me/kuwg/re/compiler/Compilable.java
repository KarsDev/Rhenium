package me.kuwg.re.compiler;

import me.kuwg.re.type.TypeRef;

import java.util.Map;

public interface Compilable {
    void replaceGenerics(Map<String, TypeRef> generics, final CompilationContext cctx);
    void compile(CompilationContext cctx);

    default String toPtr(final String llvmName) {
        return llvmName + (llvmName.equals("ptr") ? " " : "* ");
    }
}
