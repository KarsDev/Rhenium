package me.kuwg.re.ast.nodes.function.declaration;

import me.kuwg.re.type.TypeRef;
import me.kuwg.re.writer.Writeable;

public record FunctionParameter(String name, boolean mutable, TypeRef type) implements Writeable {

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Parameter: ").append(name).append(": ").append(mutable ? "mut " : "").append(type.getName()).append(NEWLINE);
    }
}
