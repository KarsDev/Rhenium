package me.kuwg.re.ast.nodes.variable;

import me.kuwg.re.ast.nodes.cast.CastNode;
import me.kuwg.re.ast.nodes.struct.StructFieldAccessNode;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.error.errors.array.RArrayTypeIsNoneError;
import me.kuwg.re.error.errors.variable.RVariableIsNotMutableError;
import me.kuwg.re.error.errors.variable.RVariableNotFoundError;
import me.kuwg.re.error.errors.variable.RVariableReassignmentTypeError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.NoneBuiltinType;
import me.kuwg.re.type.iterable.arr.ArrayType;
import me.kuwg.re.type.struct.StructType;

public class VariableDeclarationNode extends ValueNode {
    private final VariableReference variable;
    private final boolean mutable;
    private final TypeRef type;
    private final ValueNode value;

    public VariableDeclarationNode(final int line, final VariableReference variable, final boolean mutable, final TypeRef type, final ValueNode value) {
        super(line);
        this.variable = variable;
        this.mutable = mutable;
        this.type = type;
        this.value = value;
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        var oldVar = variable.getVariable(cctx);

        if (variable instanceof StructFieldAccessNode && oldVar == null) {
            return new RVariableNotFoundError(variable.getCompleteName(), line).raise();
        }

        String valueReg = value.compileAndGet(cctx);
        TypeRef valueType = value.getType();

        TypeRef targetType = type != null ? type : (oldVar != null ? oldVar.type() : valueType);

        if (!targetType.equals(valueType)) {
            ValueNode castNode = new CastNode(line, targetType, value);
            valueReg = castNode.compileAndGet(cctx);
            valueType = targetType;
        }

        if (oldVar != null) {
            return compileReassignment(cctx, oldVar, valueReg, valueType);
        }

        return compileDeclaration(cctx, valueReg, valueType);
    }

    private String compileReassignment(final CompilationContext cctx, final RVariable oldVar, String valueReg, TypeRef valueType) {
        if (type != null) {
            new RVariableReassignmentTypeError(variable.getCompleteName(), line).raise();
        }

        if (!oldVar.mutable()) {
            new RVariableIsNotMutableError(variable.getCompleteName(), line).raise();
        }

        TypeRef varType = oldVar.type();

        if (!varType.equals(valueType)) {
            ValueNode castNode = new CastNode(line, varType, value);
            valueReg = castNode.compileAndGet(cctx);
        }

        if (varType instanceof StructType && value instanceof VariableReference) {
            String loaded = cctx.nextRegister();
            cctx.emit(loaded + " = load "
                    + varType.getLLVMName() + ", "
                    + varType.getLLVMName() + "* "
                    + valueReg);
            valueReg = loaded;
        }
        String targetAddr = oldVar.addrReg() != null ? oldVar.addrReg() : oldVar.valueReg();

        String storeVal = valueReg.equals("0") && varType.isPointer() ? "null" : valueReg;
        cctx.emit("store " + varType.getLLVMName() + " " + storeVal + ", " + varType.getLLVMName() + "* " + targetAddr + " ; Reassign variable " + variable.getCompleteName());

        return valueReg;
    }

    private String compileDeclaration(final CompilationContext cctx, String valueReg, TypeRef valueType) {
        TypeRef varType = evalType(type == null ? valueType : type, cctx);

        if (valueType instanceof ArrayType arrType) {
            varType = new ArrayType(arrType.size(), arrType.inner());
            if (arrType.inner() instanceof NoneBuiltinType) {
                return new RArrayTypeIsNoneError(line).raise();
            }
        }

        String addrReg = "%" + variable.getSimpleName() + RVariable.makeUnique(variable.getSimpleName());

        if (varType instanceof ArrayType arrType) {
            cctx.emit(addrReg + " = alloca " + varType.getLLVMName());
            if (arrType.size() == ArrayType.UNKNOWN_SIZE) {
                cctx.emit("store " + arrType.inner().getLLVMName() + "* " + valueReg + ", " + arrType.inner().getLLVMName() + "** " + addrReg + " ; dynamic array pointer");
            } else {
                String sizeConst = Long.toString(arrType.size() * arrType.inner().getSize());
                cctx.emit("call void @memcpy(ptr " + addrReg + ", ptr " + valueReg + ", i64 " + sizeConst + ", i1 false)");
            }

            String loaded = "%" + RVariable.makeUnique(variable.getSimpleName());
            cctx.emit(loaded + " = load " + varType.getLLVMName() + ", " + varType.getLLVMName() + "* " + addrReg);

            RVariable v = new RVariable(variable.getSimpleName(), mutable, true, varType, addrReg, loaded);
            cctx.addVariable(v);
            return valueReg;
        }

        if (varType instanceof StructType && value instanceof VariableReference) {
            String loaded = cctx.nextRegister();
            cctx.emit(loaded + " = load "
                    + varType.getLLVMName() + ", "
                    + varType.getLLVMName() + "* "
                    + valueReg);
            valueReg = loaded;
        }
        cctx.emit(addrReg + " = alloca " + varType.getLLVMName());
        String storeVal = valueReg.equals("0") && varType.isPointer() ? "null" : valueReg;
        cctx.emit("store " + varType.getLLVMName() + " " + storeVal + ", " + varType.getLLVMName() + "* " + addrReg);

        String loaded = "%" + RVariable.makeUnique(variable.getSimpleName());
        cctx.emit(loaded + " = load " + varType.getLLVMName() + ", " + varType.getLLVMName() + "* " + addrReg);

        RVariable v = new RVariable(variable.getSimpleName(), mutable, false, varType, addrReg, loaded);
        cctx.addVariable(v);

        return valueReg;
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
}
