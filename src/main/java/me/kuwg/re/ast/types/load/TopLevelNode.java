package me.kuwg.re.ast.types.load;

import me.kuwg.re.compiler.CompilationContext;

public interface TopLevelNode {
    void load(final CompilationContext cctx);
}
