package me.kuwg.re.ast.nodes.variable;

import me.kuwg.re.ast.nodes.cast.CastNode;
import me.kuwg.re.ast.nodes.struct.StructFieldAccessNode;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.error.errors.RInternalError;
import me.kuwg.re.error.errors.array.RArrayTypeIsNoneError;
import me.kuwg.re.error.errors.variable.RVariableIsNotMutableError;
import me.kuwg.re.error.errors.variable.RVariableNotFoundError;
import me.kuwg.re.error.errors.variable.RVariableReassignmentTypeError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.NoneBuiltinType;
import me.kuwg.re.type.iterable.arr.ArrayType;

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
            valueReg = compileReassignment(cctx, oldVar, valueReg, valueType);
        } else {
            valueReg = compileDeclaration(cctx, valueReg, valueType);
        }

        setType(valueType);
        return valueReg;
    }

    private String compileReassignment(final CompilationContext cctx, final RVariable oldVar, String valueReg, TypeRef valueType) {
        if (type != null) {
            new RVariableReassignmentTypeError(variable.getCompleteName(), line).raise();
        }

        if (variable instanceof StructFieldAccessNode fieldRef) {
            RVariable structVar = fieldRef.struct.getVariable(cctx);

            if (structVar == null) {
                throw new RInternalError(); // already validated earlier
            }

            TypeRef fieldType = oldVar.type();

            if (!fieldType.equals(valueType)) {
                ValueNode castNode = new CastNode(line, fieldType, value);
                valueReg = castNode.compileAndGet(cctx);
            }

            cctx.emit("store " + fieldType.getLLVMName() + " " + valueReg + ", " + fieldType.getLLVMName() + "* " + oldVar.valueReg() + " ; Reassign struct field " + variable.getCompleteName());

            return valueReg;
        }

        if (!oldVar.mutable()) {
            new RVariableIsNotMutableError(variable.getCompleteName(), line).raise();
        }

        if (mutable) {
            warn("You don't need to specify mutability when reassigning a variable: " + variable.getSimpleName());
        }

        TypeRef varType = oldVar.type();

        if (!varType.equals(valueType)) {
            ValueNode castNode = new CastNode(line, varType, value);
            valueReg = castNode.compileAndGet(cctx);
        }

        cctx.emit("store " + varType.getLLVMName() + " " + valueReg + ", " + varType.getLLVMName() + "* " + oldVar.valueReg() + " ; Reassign variable " + variable.getCompleteName());

        return valueReg;
    }

    private String compileDeclaration(final CompilationContext cctx, String valueReg, TypeRef valueType) {
        TypeRef varType;

        if (type == null) {
            varType = valueType;
        } else {
            varType = type;
            if (!type.equals(valueType)) {
                ValueNode castNode = new CastNode(line, type, value);
                valueReg = castNode.compileAndGet(cctx);
                valueType = type;
            }

            if (valueType instanceof ArrayType arrType) {
                varType = new ArrayType(arrType.size(), ((ArrayType) type).inner());

                if (((ArrayType) varType).inner() instanceof NoneBuiltinType) {
                    return new RArrayTypeIsNoneError(line).raise();
                }
            }
        }

        String varReg = "%" + variable.getSimpleName();
        cctx.emit(varReg + " = alloca " + varType.getLLVMName() + " ; allocate variable");

        if (varType instanceof ArrayType arrType) {
            String sizeConst = Integer.toString(arrType.size() * arrType.inner().getSize());
            cctx.emit("call void @llvm.memcpy.p0.p0.i64(" + "ptr " + varReg + ", " + "ptr " + valueReg + ", " + "i64 " + sizeConst + ", " + "i1 false)");
        } else {
            cctx.emit("store " + varType.getLLVMName() + " " + valueReg + ", " + varType.getLLVMName() + "* " + varReg);
        }

        var v = new RVariable(variable.getSimpleName(), mutable, varType, varReg);
        cctx.addVariable(v);

        return valueReg;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        compileAndGet(cctx);
    }
}
