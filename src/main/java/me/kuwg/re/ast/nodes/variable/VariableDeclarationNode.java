package me.kuwg.re.ast.nodes.variable;

import me.kuwg.re.ast.nodes.cast.CastNode;
import me.kuwg.re.ast.nodes.struct.StructFieldAccessNode;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.struct.RDefaultStruct;
import me.kuwg.re.compiler.trait.Trait;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.error.errors.array.RArrayTypeIsNoneError;
import me.kuwg.re.error.errors.range.RRangeTypeError;
import me.kuwg.re.error.errors.trait.RInheritanceError;
import me.kuwg.re.error.errors.variable.RVariableIsNotMutableError;
import me.kuwg.re.error.errors.variable.RVariableNotFoundError;
import me.kuwg.re.error.errors.variable.RVariableReassignmentTypeError;
import me.kuwg.re.error.errors.variable.RVariableTypeError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.NoneBuiltinType;
import me.kuwg.re.type.iterable.arr.ArrayType;
import me.kuwg.re.type.iterable.range.RangeType;
import me.kuwg.re.type.struct.StructType;
import me.kuwg.re.type.trait.TraitType;

import java.util.Map;

public class VariableDeclarationNode extends ValueNode {
    private final VariableReference variable;
    private final boolean mutable;
    private final ValueNode value;
    private TypeRef type;

    public VariableDeclarationNode(final String fileName, final int line, final VariableReference variable, final boolean mutable, final TypeRef type, final ValueNode value) {
        super(fileName, line);
        this.variable = variable;
        this.mutable = mutable;
        this.type = type;
        this.value = value;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        variable.replaceGenerics(generics, cctx);
        type = replaceGenericType(type, generics, cctx);
        value.replaceGenerics(generics, cctx);
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        var oldVar = variable.getVariable(cctx);

        if (variable instanceof StructFieldAccessNode && oldVar == null) {
            return new RVariableNotFoundError(variable.getCompleteName(), fileName, line).raise();
        }

        String valueReg = value.compileAndGet(cctx);
        TypeRef valueType = value.getType();

        TypeRef targetType = type != null ? type : (oldVar != null ? oldVar.type() : valueType);

        if (targetType instanceof TraitType t) {
            checkTraitType(valueType, t, cctx);
        } else if (!targetType.equals(valueType)) {
            ValueNode castNode = new CastNode(fileName, line, targetType, value);
            valueReg = castNode.compileAndGet(cctx);
            valueType = targetType;
        }

        setType(valueType);

        if (oldVar != null) {
            cctx.emit("; Variable reassignment");
            return compileReassignment(cctx, oldVar, valueReg, valueType);
        }

        cctx.emit("; Variable declaration");
        return compileDeclaration(cctx, valueReg, valueType);
    }

    private String compileReassignment(final CompilationContext cctx, final RVariable oldVar, String valueReg, TypeRef valueType) {
        if (type != null) {
            new RVariableReassignmentTypeError(variable.getCompleteName(), fileName, line).raise();
        }

        if (!oldVar.mutable()) {
            new RVariableIsNotMutableError(variable.getCompleteName(), fileName, line).raise();
        }

        TypeRef varType = oldVar.type();

        if (!varType.equals(valueType)) {
            ValueNode castNode = new CastNode(fileName, line, varType, value);
            valueReg = castNode.compileAndGet(cctx);
        }

        valueReg = cctx.ensureValue(value, valueReg);

        String targetAddr = oldVar.addrReg();

        String storeVal = valueReg.equals("0") && varType.isPointer() ? "null" : valueReg;
        cctx.emit("store " + varType.getLLVMName() + " " + storeVal + ", " + toPtr(varType.getLLVMName()) + targetAddr + " ; Reassign variable " + variable.getCompleteName());

        return valueReg;
    }

    private String compileDeclaration(final CompilationContext cctx, String valueReg, TypeRef valueType) {
        TypeRef varType = evalType(type == null || type instanceof TraitType ? valueType : type, cctx, fileName, line);

        if (valueType instanceof ArrayType arrType) {
            varType = new ArrayType(arrType.size(), arrType.inner());
            if (arrType.inner() instanceof NoneBuiltinType) {
                return new RArrayTypeIsNoneError(fileName, line).raise();
            }
        }

        String addrReg = "%" + variable.getSimpleName() + RVariable.makeUnique(variable.getSimpleName());

        if (varType instanceof ArrayType arrType) {
            cctx.emit(addrReg + " = alloca " + varType.getLLVMName());
            if (arrType.size() == ArrayType.UNKNOWN_SIZE) {
                cctx.emit("store " + toPtr(arrType.inner().getLLVMName()) + valueReg + ", " + arrType.inner().getLLVMName() + "** " + addrReg + " ; dynamic array pointer");
            } else {
                cctx.emit("store " + varType.getLLVMName() + " " + valueReg + ", ptr " + addrReg);
            }

            String loaded = "%" + RVariable.makeUnique(variable.getSimpleName());
            cctx.emit(loaded + " = load " + varType.getLLVMName() + ", " + toPtr(varType.getLLVMName()) + addrReg);

            RVariable v = new RVariable(variable.getSimpleName(), mutable, true, varType, addrReg, loaded);
            cctx.addVariable(v);
            return valueReg;
        }

        if (varType instanceof StructType && value instanceof VariableReference) {
            RVariable src = ((VariableReference) value).getVariable(cctx);
            cctx.addVariable(new RVariable(variable.getSimpleName(), mutable, false, varType, src.addrReg(), src.valueReg()));
            return valueReg;
        }

        if (varType instanceof RangeType) {
            return new RRangeTypeError(fileName, line).raise();
        }

        cctx.emit(addrReg + " = alloca " + varType.getLLVMName());
        String storeVal = valueReg.equals("0") && varType.isPointer() ? "null" : valueReg;

        cctx.emit("store " + varType.getLLVMName() + " " + storeVal + ", " + toPtr(varType.getLLVMName()) + " " + addrReg);

        String loaded = "%" + RVariable.makeUnique(variable.getSimpleName());
        cctx.emit(loaded + " = load " + varType.getLLVMName() + ", " + toPtr(varType.getLLVMName()) + " " + addrReg);

        RVariable v = new RVariable(variable.getSimpleName(), mutable, false, varType, addrReg, loaded);
        cctx.addVariable(v);

        return valueReg;
    }

    private void checkTraitType(TypeRef type, TraitType traitType, CompilationContext cctx) {
        Trait t = cctx.getTrait(traitType.getName());
        if (t == null) {
            new RInheritanceError("Trait not found: " + traitType.name(), fileName, line).raise();
            return;
        }

        if (!(type instanceof StructType s)) {
            new RVariableTypeError(type.getName(), traitType.getName(), fileName, line).raise();
            return;
        }

        RDefaultStruct struct = cctx.getStruct(s.getName());
        if (!struct.inherited().contains(t.name()))
            new RVariableTypeError(type.getName(), traitType.getName(), fileName, line).raise();


    }

    @Override
    public void compile(final CompilationContext cctx) {
        compileAndGet(cctx);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Variable Declaration: ").append(NEWLINE).append(indent).append(TAB).append("Name: ").append(NEWLINE);
        variable.write(sb, indent + TAB + TAB);
        sb.append(indent).append(TAB).append("Value: ").append(NEWLINE);
        value.write(sb, indent + TAB + TAB);
        if (type != null)
            sb.append(indent).append(TAB).append("Type: ").append(mutable ? "mut " : "").append(type.getName()).append(NEWLINE);
        else sb.append(indent).append(TAB).append("Type: mut ?").append(NEWLINE);
    }

    @Override
    public String toString() {
        return "VariableDeclarationNode{" + "mutable=" + mutable + ", variable=" + variable + ", type=" + type + ", value=" + value + '}';
    }

    @Override
    public VariableDeclarationNode clone() {
        return new VariableDeclarationNode(fileName, line, variable.clone(), mutable, type, value.clone());
    }
}
