package me.kuwg.re.ast.nodes.struct;

import me.kuwg.re.ast.nodes.cast.CastNode;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.compiler.struct.RStruct;
import me.kuwg.re.compiler.variable.RParamValue;
import me.kuwg.re.compiler.variable.RStructField;
import me.kuwg.re.error.errors.struct.RStructAccessError;
import me.kuwg.re.error.errors.struct.RStructInitParamsError;
import me.kuwg.re.error.errors.struct.RStructUndefinedError;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.type.TypeRef;

import java.util.ArrayList;
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
        RStruct struct = cctx.getStruct(name);
        if (struct == null) {
            return new RStructUndefinedError(name, line).raise();
        }

        if (struct.builtin()) {
            return new RStructAccessError("This struct can't be initialized: " + name, line).raise();
        }

        return compileStruct(cctx, struct);
    }

    private String compileStruct(CompilationContext cctx, RStruct struct) {
        List<String> valueRegs = new ArrayList<>();

        for (RParamValue param : values) {
            if (param.name() != null) {
                return new RStructInitParamsError("Named parameters are not supported in struct initialization", line).raise();
            }

            valueRegs.add(param.value().compileAndGet(cctx));
        }

        for (RFunction ctor : struct.constructors()) {
            int expected = ctor.parameters().size() - 1;
            if (expected == valueRegs.size()) {
                return compileConstructor(ctor, struct, valueRegs, cctx);
            }
        }

        return compileNoConstructor(valueRegs, struct, cctx);
    }

    private String compileConstructor(RFunction constructor, RStruct struct, List<String> valueRegs, CompilationContext cctx) {
        String structPtr = cctx.nextRegister();
        cctx.emit(structPtr + " = alloca " + struct.type().getLLVMName());

        var params = constructor.parameters();
        List<String> args = new ArrayList<>();
        args.add(struct.type().getLLVMName() + "* " + structPtr);

        for (int i = 1; i < params.size(); i++) {
            TypeRef expected = params.get(i).type();
            ValueNode valueNode = values.get(i - 1).value();
            String valueReg = valueRegs.get(i - 1);

            if (!expected.equals(valueNode.getType())) {
                valueReg = new CastNode(line, expected, valueNode).compileAndGet(cctx);
            }
            args.add(expected.getLLVMName() + " " + valueReg);
        }

        cctx.emit("call void @" + constructor.llvmName + "(" + String.join(", ", args) + ")");

        setType(struct.type());
        return structPtr;
    }

    private String compileNoConstructor(List<String> valueRegs, RStruct struct, CompilationContext cctx) {
        List<RStructField> fields = struct.fields();

        if (fields.size() != valueRegs.size()) {
            return new RStructInitParamsError("Expected " + fields.size() + " fields but got " + valueRegs.size(), line).raise();
        }

        String structPtr = cctx.nextRegister();
        cctx.emit(structPtr + " = alloca " + struct.type().getLLVMName());

        for (int i = 0; i < fields.size(); i++) {
            RStructField field = fields.get(i);
            ValueNode valueNode = values.get(i).value();
            String valueReg = valueRegs.get(i);

            if (!field.type().equals(valueNode.getType())) {
                valueReg = new CastNode(line, field.type(), valueNode).compileAndGet(cctx);
            }

            String fieldPtr = cctx.nextRegister();
            cctx.emit(fieldPtr + " = getelementptr inbounds " + struct.type().getLLVMName() + ", " + struct.type().getLLVMName() + "* " + structPtr + ", i32 0, i32 " + i);

            cctx.emit("store " + field.type().getLLVMName() + " " + valueReg + ", " + field.type().getLLVMName() + "* " + fieldPtr);
        }

        setType(struct.type());
        return structPtr;
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
