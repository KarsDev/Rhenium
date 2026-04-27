package me.kuwg.re.ast.nodes.struct.gen;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.struct.RDefaultStruct;
import me.kuwg.re.compiler.struct.RGenStruct;
import me.kuwg.re.compiler.struct.RStruct;
import me.kuwg.re.compiler.struct.StructCompiler;
import me.kuwg.re.compiler.variable.RParamValue;
import me.kuwg.re.error.errors.struct.RGenStructInitError;
import me.kuwg.re.error.errors.struct.RStructUndefinedError;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.type.TypeRef;

import java.util.List;

public class GenStructInitNode extends ValueNode {
    private final String name;
    private final List<TypeRef> genericTypes;
    private final List<RParamValue> values;

    public GenStructInitNode(final int line, final String name, final List<TypeRef> genericTypes, final List<RParamValue> values) {
        super(line);
        this.name = name;
        this.genericTypes = genericTypes;
        this.values = values;
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        RDefaultStruct struct = cctx.getStruct(name);

        if (struct == null) {
            return new RStructUndefinedError(name, line).raise();
        }

        if (struct instanceof RStruct) {
            return new RGenStructInitError("Struct is not generic: " + name, line).raise();
        }

        RGenStruct genStruct = (RGenStruct) struct;

        if (genStruct.type().genericTypes().size() != genericTypes.size()) {
            return new RGenStructInitError(
                    "Expected " + genStruct.type().genericTypes().size() +
                            " generic types but got " + genericTypes.size(),
                    line
            ).raise();
        }

        RStruct specialized = genStruct.instantiate(genericTypes, cctx);

        return StructCompiler.compile(line, cctx, specialized, values, this);
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RValueMustBeUsedError("Generic Struct Initialization", line).raise();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Generic Struct Init: ").append(NEWLINE);
        sb.append(indent).append(TAB).append("Name: ").append(name).append(NEWLINE);
        sb.append(indent).append(TAB).append("Types: ").append(NEWLINE);
        genericTypes.forEach(t -> sb.append(indent).append(TAB).append(TAB).append("- ").append(t.getName()).append(NEWLINE));
        sb.append(indent).append(TAB).append("Values: ").append(NEWLINE);
        values.forEach(v -> v.value().write(sb, indent + TAB + TAB));
    }
}
