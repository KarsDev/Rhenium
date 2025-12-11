package me.kuwg.re.ast.nodes.variable;

import me.kuwg.re.ast.nodes.cast.CastNode;
import me.kuwg.re.ast.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.error.errors.variable.RVariableIsNotMutableError;
import me.kuwg.re.error.errors.variable.RVariableReassignmentTypeError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.arr.ArrayType;

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
        sb.append(indent).append("Variable Declaration: ").append(NEWLINE)
                .append(indent).append(TAB).append("Name: ").append(NEWLINE);
        variable.write(sb, indent + TAB + TAB);
        sb.append(indent).append(TAB).append("Value: ").append(NEWLINE);
        value.write(sb, indent + TAB + TAB);

        if (type != null)
            sb.append(indent).append(TAB).append("Type: ").append(mutable ? "mut " : "").append(type.getName()).append(NEWLINE);
        else
            sb.append(indent).append(TAB).append("Type: mut ?").append(NEWLINE);
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        var oldVar = variable.getVariable(cctx);

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

        cctx.emit("store " + varType.getLLVMName() + " " + valueReg + ", " + varType.getLLVMName() + "* %" + variable.getSimpleName() + " ; Reassign variable " + variable.getCompleteName());
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
            }
        }

        String varReg = "%" + variable.getSimpleName();
        cctx.emit(varReg + " = alloca " + varType.getLLVMName() + " ; allocate variable");

        if (varType instanceof ArrayType arrType) {
            String sizeConst = Integer.toString(arrType.size() * arrType.inner().getSize());
            cctx.emit("call void @llvm.memcpy.p0.p0.i64("
                    + "ptr " + varReg + ", "
                    + "ptr " + valueReg + ", "
                    + "i64 " + sizeConst + ", "
                    + "i1 false)"
            );
        } else {
            cctx.emit("store " + varType.getLLVMName() + " " + valueReg +
                    ", " + varType.getLLVMName() + "* " + varReg);
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
