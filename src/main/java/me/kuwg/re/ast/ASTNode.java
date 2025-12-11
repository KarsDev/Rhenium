package me.kuwg.re.ast;

import me.kuwg.re.compiler.Compilable;
import me.kuwg.re.error.warning.Warning;
import me.kuwg.re.writer.Writeable;

public abstract class ASTNode implements Compilable, Writeable {
    protected final int line;

    protected ASTNode(int line) {
        this.line = line;
    }

    protected final void warn(String message) {
        new Warning(message, line).print();
    }

    public int getLine() {
        return line;
    }
}
