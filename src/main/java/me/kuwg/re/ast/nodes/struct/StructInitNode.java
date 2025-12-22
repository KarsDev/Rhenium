package me.kuwg.re.ast.nodes.struct;

import me.kuwg.re.ast.nodes.cast.CastNode;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.struct.RDefStruct;
import me.kuwg.re.compiler.struct.RGenStruct;
import me.kuwg.re.compiler.struct.RStruct;
import me.kuwg.re.compiler.variable.RParamValue;
import me.kuwg.re.compiler.variable.RStructField;
import me.kuwg.re.error.errors.struct.RStructAccessError;
import me.kuwg.re.error.errors.struct.RStructInitParamsError;
import me.kuwg.re.error.errors.struct.RStructUndefinedError;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.iterable.arr.ArrayType;
import me.kuwg.re.type.ptr.PointerType;
import me.kuwg.re.type.struct.StructType;

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

        if (struct instanceof RGenStruct genStruct) {
            return compileGenericStruct(cctx, genStruct);
        }

        return compileConcreteStruct(cctx, struct);
    }

    private String compileConcreteStruct(CompilationContext cctx, RStruct struct) {
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
            if (idx == -1 || resolved[idx] != null) {
                return new RStructInitParamsError(name, values.size(), fields.size(), line).raise();
            }
            resolved[idx] = param.value();
        }

        int posIndex = 0;
        for (RParamValue param : values) {
            if (param.name() != null) continue;
            while (posIndex < resolved.length && resolved[posIndex] != null) posIndex++;
            if (posIndex >= resolved.length) {
                return new RStructInitParamsError(name, values.size(), fields.size(), line).raise();
            }
            resolved[posIndex++] = param.value();
        }

        for (int i = 0; i < fields.size(); i++) {
            if (resolved[i] == null) {
                ValueNode def = fields.get(i).defaultValue();
                if (def == null) return new RStructInitParamsError(name, values.size(), fields.size(), line).raise();
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

            cctx.emit(newReg + " = insertvalue " + structLLVM + " " + base + ", " +
                    field.type().getLLVMName() + " " + valueReg + ", " + i);

            aggReg = newReg;
        }

        String hReg = cctx.nextRegister();
        cctx.emit(hReg + " = alloca " + structLLVM);
        cctx.emit("store " + structLLVM + " " + aggReg + ", " + structLLVM + "* " + hReg);
        return hReg;
    }

    private String compileGenericStruct(CompilationContext cctx, RGenStruct genStruct) {
        List<TypeRef> bindings = new ArrayList<>();
        List<RStructField> concreteFields = new ArrayList<>();

        for (int i = 0; i < genStruct.fields().size(); i++) {
            RStructField field = genStruct.fields().get(i);
            ValueNode valueNode = i < values.size() ? values.get(i).value() : field.defaultValue();

            if (valueNode == null) {
                return new RStructInitParamsError(name, values.size(), genStruct.fields().size(), line).raise();
            }

            TypeRef resolvedType = valueNode.getType();
            TypeRef concreteType = substituteGeneric(field.type(), resolvedType);

            concreteFields.add(new RStructField(field.name(), concreteType, field.defaultValue()));
            bindings.add(concreteType);
        }

        RStruct inst = genStruct.getInstantiation(bindings);
        if (inst == null) {
            inst = new RDefStruct(false, new StructType(genStruct.type().getName(), bindings), concreteFields);
            genStruct.addInstantiation(bindings, inst);
        }

        String llvmStructName = inst.type().getLLVMName();
        if (!cctx.isStructDeclared(llvmStructName)) {
            StringBuilder fieldTypesCSV = new StringBuilder();
            for (int i = 0; i < concreteFields.size(); i++) {
                if (i > 0) fieldTypesCSV.append(", ");
                fieldTypesCSV.append(concreteFields.get(i).type().getLLVMName());
            }
            cctx.declare(llvmStructName + " = type { " + fieldTypesCSV + " }");
            cctx.markStructDeclared(llvmStructName);
        }

        cctx.emit("; Init generic struct");
        setType(inst.type());

        String aggReg = null;
        for (int i = 0; i < concreteFields.size(); i++) {
            RStructField field = concreteFields.get(i);
            ValueNode v = values.size() > i ? values.get(i).value() : field.defaultValue();

            assert v != null;

            String valueReg = v.compileAndGet(cctx);

            if (!v.getType().equals(field.type())) {
                v = new CastNode(line, field.type(), v);
                valueReg = v.compileAndGet(cctx);
            }

            String newReg = cctx.nextRegister();
            String base = (aggReg == null) ? "undef" : aggReg;

            cctx.emit(newReg + " = insertvalue " + llvmStructName + " " + base + ", " +
                    field.type().getLLVMName() + " " + valueReg + ", " + i);

            aggReg = newReg;
        }

        String hReg = cctx.nextRegister();
        cctx.emit(hReg + " = alloca " + llvmStructName);

        cctx.emit("store " + llvmStructName + " " + aggReg + ", " + llvmStructName + "* " + hReg);

        return hReg;

    }

    private TypeRef substituteGeneric(TypeRef type, TypeRef concrete) {
        if (type instanceof PointerType ptr) {
            return new PointerType(substituteGeneric(ptr.inner(), concrete));
        } else if (type instanceof ArrayType arr) {
            return new ArrayType(arr.size(), substituteGeneric(arr.inner(), concrete));
        }
        return concrete;
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
