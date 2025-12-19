package me.kuwg.re.ast.nodes.struct;

import me.kuwg.re.ast.nodes.cast.CastNode;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.struct.RStruct;
import me.kuwg.re.compiler.variable.RParamValue;
import me.kuwg.re.compiler.variable.RStructField;
import me.kuwg.re.error.errors.struct.RStructInitParamsError;
import me.kuwg.re.error.errors.struct.RStructUndefinedError;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;

import java.util.List;
import java.util.Objects;

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
        RStruct struct = cctx.getStruct(name);

        if (struct == null) {
            return new RStructUndefinedError(name, line).raise();
        }

        final List<RStructField> fields = struct.fields();
        final ValueNode[] resolved = new ValueNode[fields.size()];

        for (RParamValue param : values) {
            if (param.name() == null) continue;

            int idx = -1;
            for (int i = 0; i < fields.size(); i++) {
                if (fields.get(i).name().equals(param.name())) {
                    idx = i;
                    break;
                }
            }

            if (idx == -1) {
                return new RStructInitParamsError(name, values.size(), fields.size(), line).raise();
            }

            if (resolved[idx] != null) {
                return new RStructInitParamsError(name, values.size(), fields.size(), line).raise();
            }

            resolved[idx] = param.value();
        }

        int posIndex = 0;
        for (RParamValue param : values) {
            if (param.name() != null) continue;

            while (posIndex < resolved.length && resolved[posIndex] != null) {
                posIndex++;
            }

            if (posIndex >= resolved.length) {
                return new RStructInitParamsError(name, values.size(), fields.size(), line).raise();
            }

            resolved[posIndex++] = param.value();
        }

        for (int i = 0; i < fields.size(); i++) {
            if (resolved[i] == null) {
                ValueNode def = fields.get(i).defaultValue();
                if (def == null) {
                    return new RStructInitParamsError(name, values.size(), fields.size(), line).raise();
                }
                resolved[i] = def;
            }
        }

        cctx.emit("; Init struct");
        setType(struct.type());

        String aggReg = null;
        String structLLVM = struct.type().getLLVMName();

        for (int i = 0; i < fields.size(); i++) {
            RStructField field = fields.get(i);
            ValueNode v = resolved[i];
            String valueReg = v.compileAndGet(cctx);

            if (!v.getType().equals(field.type())) {
                v = new CastNode(line, field.type(), v);
                valueReg = v.compileAndGet(cctx);
            }

            String newReg = cctx.nextRegister();
            String base = (aggReg == null) ? "undef" : aggReg;

            cctx.emit(
                    newReg + " = insertvalue " +
                            structLLVM + " " + base + ", " +
                            field.type().getLLVMName() + " " +
                            valueReg + ", " + i
            );

            aggReg = newReg;
        }

        return Objects.requireNonNull(aggReg);
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
