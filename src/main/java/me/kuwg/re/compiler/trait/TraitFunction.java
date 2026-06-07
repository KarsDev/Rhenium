package me.kuwg.re.compiler.trait;

import me.kuwg.re.ast.nodes.function.declaration.FunctionParameter;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.writer.Writeable;

import java.util.List;

public final class TraitFunction implements Writeable {
    private final String name;
    private final List<FunctionParameter> params;
    private TypeRef returnType;

    public TraitFunction(String name, List<FunctionParameter> params, TypeRef returnType) {
        this.name = name;
        this.params = params;
        this.returnType = returnType;
    }

    public String getName() {
        return name;
    }

    public List<FunctionParameter> params() {
        return params;
    }

    public TypeRef getReturnType() {
        return returnType;
    }

    public void setReturnType(final TypeRef returnType) {
        this.returnType = returnType;
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Trait: ").append(NEWLINE)
                .append(indent).append(TAB).append("Name: ").append(name).append(NEWLINE)
                .append(indent).append(TAB).append("Param Types: ").append(NEWLINE);
        params.forEach(p -> p.write(sb, indent + TAB + TAB));
        sb.append(indent).append(TAB).append("Return Type: ").append(returnType).append(NEWLINE);
    }
}
