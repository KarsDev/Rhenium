package me.kuwg.re.ast;

import me.kuwg.re.compiler.Compilable;
import me.kuwg.re.type.generic.GenericTypeEvaluator;
import me.kuwg.re.writer.Writeable;

public abstract class ASTNode implements Compilable, Writeable, GenericTypeEvaluator {
    protected final int line;

    protected ASTNode(int line) {
        this.line = line;
    }

    public int getLine() {
        return line;
    }
}
