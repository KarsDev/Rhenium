package me.kuwg.re.ast.nodes.struct;

import me.kuwg.re.ast.nodes.cast.CastNode;
import me.kuwg.re.ast.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.struct.RStruct;
import me.kuwg.re.compiler.variable.RStructField;
import me.kuwg.re.error.errors.struct.RStructInitParamsError;
import me.kuwg.re.error.errors.struct.RStructUndefinedError;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.type.TypeRef;

import java.util.List;
import java.util.Objects;

public class StructInitNode extends ValueNode {
    private final String name;
    private final List<ValueNode> values;

    public StructInitNode(final int line, final String name, final List<ValueNode> values) {
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

        final List<RStructField> structFields = struct.fields();

        if (values.size() != structFields.size()) {
            return new RStructInitParamsError(name, values.size(), structFields.size(), line).raise();
        }

        cctx.emit("; Init struct");

        setType(struct.type());

        if (structFields.isEmpty()) {
            return "undef";
        }

        String aggReg = null;
        String structLLVM = struct.type().getLLVMName();

        for (int i = 0; i < values.size(); i++) {
            ValueNode v = values.get(i);
            TypeRef valueType = v.getType();
            RStructField field = structFields.get(i);
            TypeRef expected = field.type();

            if (!valueType.equals(expected)) {
                v = new CastNode(line, expected, v);
            }

            String valueReg = v.compileAndGet(cctx);

            String newReg = cctx.nextRegister();
            String base = (aggReg == null) ? "undef" : aggReg;

            cctx.emit("; Define value for field");
            cctx.emit(newReg + " = insertvalue " + structLLVM + " " + base + ", " + expected.getLLVMName() + " " + valueReg + ", " + i);

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
        values.forEach(v -> v.write(sb, indent + TAB + TAB));
    }
}
