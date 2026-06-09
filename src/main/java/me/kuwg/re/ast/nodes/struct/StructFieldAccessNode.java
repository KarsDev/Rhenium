package me.kuwg.re.ast.nodes.struct;

import me.kuwg.re.ast.nodes.variable.VariableReference;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.enums.REnum;
import me.kuwg.re.compiler.enums.REnumField;
import me.kuwg.re.compiler.struct.RDefaultStruct;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.error.errors.enums.REnumFieldNotFoundError;
import me.kuwg.re.error.errors.struct.RStructAccessError;
import me.kuwg.re.error.errors.struct.RStructUndefinedError;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.error.errors.variable.RVariableNotFoundError;
import me.kuwg.re.error.errors.variable.RVariableTypeError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.struct.StructType;

import java.util.Map;

public class StructFieldAccessNode extends VariableReference {
    public final VariableReference struct;
    private final String fieldName;

    public StructFieldAccessNode(final int line, final VariableReference struct, final String fieldName) {
        super(line);
        this.struct = struct;
        this.fieldName = fieldName;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        struct.replaceGenerics(generics, cctx);
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        cctx.emit("; Struct field access");

        RVariable structVar = struct.getVariable(cctx);
        if (structVar == null) {
            return attemptEnumAccess(cctx);
        }

        TypeRef structType = cctx.resolveConcrete(structVar.type());
        if (!(structType instanceof StructType st)) {
            return new RVariableTypeError("struct", structType.getName(), line).raise();
        }

        RDefaultStruct structDef = cctx.getStruct(st.name());
        if (structDef == null) {
            return new RStructUndefinedError(st.name(), line).raise();
        }

        int index = -1;
        TypeRef fieldType = null;

        var fields = structDef.fields();
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).name().equals(fieldName)) {
                index = i;
                fieldType = fields.get(i).type();
                break;
            }
        }

        if (index == -1) {
            return new RStructAccessError("Struct '" + st.name() + "' has no field '" + fieldName + "'", line).raise();
        }

        setType(fieldType);

        String structPtr = structVar.addrReg();

        String fieldPtr = cctx.nextRegister();
        cctx.emit(fieldPtr + " = getelementptr " + st.getLLVMName() + ", " + toPtr(st.getLLVMName()) + structPtr + ", i32 0, i32 " + index);

        if (fieldType instanceof StructType) {
            return fieldPtr;
        }

        String loadReg = cctx.nextRegister();
        cctx.emit(loadReg + " = load " + fieldType.getLLVMName() + ", " + toPtr(fieldType.getLLVMName()) + fieldPtr);

        return loadReg;
    }

    private String attemptEnumAccess(final CompilationContext cctx) {
        String enumName = struct.getCompleteName();
        if (enumName.contains(".")) return new RVariableNotFoundError(struct.getSimpleName(), line).raise();

        REnum members = cctx.getEnum(enumName);
        if (members == null) return new RVariableNotFoundError(enumName, null, line).raise();

        REnumField field = members.getField(fieldName);

        if (field == null) return new REnumFieldNotFoundError(fieldName, line).raise();

        setType(field.type());
        return field.valueReg();
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RValueMustBeUsedError("Struct Field", line).raise();
    }

    @Override
    public RVariable getVariable(final CompilationContext cctx) {
        RVariable structVar = struct.getVariable(cctx);
        if (structVar == null) {
            return new RStructAccessError("Struct access on non-variable: " + struct.getCompleteName(), line).raise();
        }

        TypeRef structType = structVar.type();
        if (!(structType instanceof StructType st)) {
            return new RVariableTypeError("struct", structType.getName(), line).raise();
        }

        RDefaultStruct structDef = cctx.getStruct(st.name());
        if (structDef == null) {
            return new RStructUndefinedError(st.name(), line).raise();
        }

        int index = -1;
        TypeRef fieldType = null;

        var fields = structDef.fields();
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).name().equals(fieldName)) {
                index = i;
                fieldType = fields.get(i).type();
                break;
            }
        }

        if (index == -1) {
            return new RStructAccessError("Struct '" + st.name() + "' has no field '" + fieldName + "'", line).raise();
        }

        setType(fieldType);
        String structPtr = structVar.addrReg();

        String fieldPtr = cctx.nextRegister();
        cctx.emit(fieldPtr + " = getelementptr " + st.getLLVMName() + ", " + toPtr(st.getLLVMName()) + structPtr + ", i32 0, i32 " + index);

        String loaded = cctx.nextRegister();

        TypeRef concrete = cctx.resolveConcrete(fieldType);
        String ftln = concrete.getLLVMName();

        cctx.emit(loaded + " = load " + ftln + ", " + toPtr(ftln) + fieldPtr);

        return new RVariable(fieldName, true, true, fieldType, fieldPtr, loaded);
    }

    @Override
    public String getCompleteName() {
        return struct.getCompleteName() + "." + fieldName;
    }

    @Override
    public String getSimpleName() {
        return fieldName;
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Struct Field Access: ").append(NEWLINE);
        sb.append(indent).append(TAB).append("Struct: ").append(NEWLINE);
        struct.write(sb, indent + TAB + TAB);
        sb.append(indent).append(TAB).append("Field: ").append(fieldName).append(NEWLINE);
    }

    @Override
    public StructFieldAccessNode clone() {
        return new StructFieldAccessNode(line, struct.clone(), fieldName);
    }
}