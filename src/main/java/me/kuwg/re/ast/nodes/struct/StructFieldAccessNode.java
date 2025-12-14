package me.kuwg.re.ast.nodes.struct;

import me.kuwg.re.ast.nodes.variable.VariableReference;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.struct.RStruct;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.error.errors.struct.RStructAccessError;
import me.kuwg.re.error.errors.struct.RStructUndefinedError;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.error.errors.variable.RVariableNotFoundError;
import me.kuwg.re.error.errors.variable.RVariableTypeError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.struct.StructType;

public class StructFieldAccessNode extends VariableReference {
    public final VariableReference struct;
    private final String fieldName;

    public StructFieldAccessNode(final int line, final VariableReference struct, final String fieldName) {
        super(line);
        this.struct = struct;
        this.fieldName = fieldName;
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        String structPtr = struct.getVariable(cctx).valueReg();

        RVariable structVar = struct.getVariable(cctx);
        if (structVar == null) {
            return new RStructAccessError(
                    "Struct access on non-variable: " + struct.getCompleteName(), line
            ).raise();
        }

        TypeRef structType = structVar.type();
        if (!(structType instanceof StructType st)) {
            return new RVariableTypeError("struct", structType.getName(), line).raise();
        }

        RStruct structV = cctx.getStruct(st.name());
        if (structV == null) {
            return new RStructUndefinedError(st.name(), line).raise();
        }

        var structFields = structV.fields();

        int index = -1;
        TypeRef fieldType = null;

        for (int i = 0; i < structFields.size(); i++) {
            if (structFields.get(i).name().equals(fieldName)) {
                index = i;
                fieldType = structFields.get(i).type();
                break;
            }
        }

        if (index == -1) {
            return new RStructAccessError(
                    "Struct '" + st.name() + "' has no field '" + fieldName +
                            "'", line
            ).raise();
        }

        setType(fieldType);

        String gepReg = cctx.nextRegister();
        cctx.emit("; Get pointer to struct field '" + fieldName + "'");
        cctx.emit(gepReg + " = getelementptr " +
                st.getLLVMName() + ", " +
                st.getLLVMName() + "* " + structPtr +
                ", i32 0, i32 " + index
        );

        String loadReg = cctx.nextRegister();
        cctx.emit("; Load struct field '" + fieldName + "'");
        cctx.emit(loadReg + " = load " +
                fieldType.getLLVMName() + ", " +
                fieldType.getLLVMName() + "* " + gepReg
        );

        return loadReg;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RValueMustBeUsedError("Struct Field", line).raise();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Struct Field Access: ").append(NEWLINE);
        sb.append(indent).append(TAB).append("Struct: ").append(NEWLINE);
        struct.write(sb, indent + TAB + TAB);
        sb.append(indent).append(TAB).append("Field: ").append(fieldName).append(NEWLINE);
    }

    @Override
    public RVariable getVariable(final CompilationContext cctx) {
        RVariable structVar = struct.getVariable(cctx);
        if (structVar == null) {
            return new RStructAccessError(
                    "Struct access on non-variable: " + struct.getCompleteName(), line
            ).raise();
        }

        TypeRef structType = structVar.type();
        if (!(structType instanceof StructType st)) {
            return new RVariableTypeError(
                    "struct", structType.getName(), line
            ).raise();
        }

        RStruct structV = cctx.getStruct(st.name());
        if (structV == null) {
            return new RStructUndefinedError(st.name(), line).raise();
        }

        var structFields = structV.fields();

        int index = -1;
        TypeRef fieldType = null;

        for (int i = 0; i < structFields.size(); i++) {
            if (structFields.get(i).name().equals(fieldName)) {
                index = i;
                fieldType = structFields.get(i).type();
                break;
            }
        }

        if (index == -1) {
            return new RStructAccessError(
                    "Struct '" + st.name() + "' has no field '" + fieldName +
                            "'", line
            ).raise();
        }

        String gepReg = cctx.nextRegister();
        cctx.emit("; Get pointer to struct field '" + fieldName + "'");
        cctx.emit(gepReg + " = getelementptr " +
                st.getLLVMName() + ", " +
                st.getLLVMName() + "* " + structVar.valueReg() +
                ", i32 0, i32 " + index
        );

        return new RVariable(fieldName, true, fieldType, gepReg);
    }

    @Override
    public String getCompleteName() {
        return struct.getCompleteName() + "." + fieldName;
    }

    @Override
    public String getSimpleName() {
        return fieldName;
    }
}
