package me.kuwg.re.ast.nodes.function.declaration;

import me.kuwg.re.type.TypeRef;
import me.kuwg.re.writer.Writeable;

import java.util.Objects;

public final class FunctionParameter implements Writeable {
    private final String name;
    private final boolean mutable;
    private TypeRef type;

    public FunctionParameter(String name, boolean mutable, TypeRef type) {
        this.name = name;
        this.mutable = mutable;
        this.type = type;
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Parameter: ").append(name).append(": ").append(mutable ? "mut " : "").append(type.getName()).append(NEWLINE);
    }

    public String name() {
        return name;
    }

    public boolean mutable() {
        return mutable;
    }

    public TypeRef type() {
        return type;
    }

    public void setType(final TypeRef type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (FunctionParameter) obj;
        return Objects.equals(this.name, that.name) &&
                this.mutable == that.mutable &&
                Objects.equals(this.type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, mutable, type);
    }

    @Override
    public String toString() {
        return "FunctionParameter[" +
                "name=" + name + ", " +
                "mutable=" + mutable + ", " +
                "type=" + type + ']';
    }

}
