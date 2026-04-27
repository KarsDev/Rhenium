package me.kuwg.re.ast.nodes.struct;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.struct.RDefaultStruct;
import me.kuwg.re.compiler.struct.RGenStruct;
import me.kuwg.re.compiler.struct.StructCompiler;
import me.kuwg.re.compiler.variable.RParamValue;
import me.kuwg.re.error.errors.struct.RGenStructInitError;
import me.kuwg.re.error.errors.struct.RStructAccessError;
import me.kuwg.re.error.errors.struct.RStructUndefinedError;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;

import java.util.List;

public class StructInitNode extends ValueNode {
    private final String name;
    private final List<RParamValue> values;

    public StructInitNode(final int line, final String name, final List<RParamValue> values) {
        super(line);
        this.name = name;
        this.values = values;
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        RDefaultStruct struct = cctx.getStruct(name);
        if (struct == null) {
            return new RStructUndefinedError(name, line).raise();
        }

        if (struct.builtin()) {
            return new RStructAccessError("This struct can't be initialized: " + name, line).raise();
        }

        if (struct instanceof RGenStruct) {
            return new RGenStructInitError("Add generic types for generic struct initialization", line).raise();
        }

        return StructCompiler.compile(line, cctx, struct, values, this);
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RValueMustBeUsedError("Struct Initialization", line).raise();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Struct Init: ").append(NEWLINE);
        sb.append(indent).append(TAB).append("Name: ").append(name).append(NEWLINE);
        sb.append(indent).append(TAB).append("Values: ").append(NEWLINE);
        values.forEach(v -> v.value().write(sb, indent + TAB + TAB));
    }
}
