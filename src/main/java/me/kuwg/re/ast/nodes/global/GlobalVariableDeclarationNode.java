package me.kuwg.re.ast.nodes.global;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.global.GlobalNode;
import me.kuwg.re.ast.nodes.constants.ConstantNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.error.errors.variable.RGlobalVariableScopeError;
import me.kuwg.re.error.errors.variable.RVariableTypeError;
import me.kuwg.re.type.TypeRef;

public class GlobalVariableDeclarationNode extends ASTNode implements GlobalNode {
    private final String name;
    private final TypeRef type;
    private final ConstantNode value;

    public GlobalVariableDeclarationNode(final int line, final String name, final TypeRef type, final ConstantNode value) {
        super(line);
        this.name = name;
        this.type = type;
        this.value = value;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        if (!cctx.emptyScope()) {
            new RGlobalVariableScopeError(name, line);
        }

        String initialValue = value.compileToConstant(cctx);
        TypeRef varType = type != null ? type : value.getType();

        if (type != null && !type.isCompatibleWith(value.getType())) {
            new RVariableTypeError(value.getType().getName(), type.getName(), line).raise();
        }

        String llvmDecl = "@" + name + " = global " + varType.getLLVMName() + " " + initialValue;
        cctx.declare(llvmDecl + " ; Global variable " + name);

        RVariable globalVar = new RVariable(name, false, varType, "@" + name);
        cctx.addGlobal(globalVar);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Global Variable Declaration: ").append(name).append(NEWLINE);
        if (type != null) {
            sb.append(indent).append(TAB).append("Type: ").append(type.getName()).append(NEWLINE);
        }

        sb.append(indent).append(TAB).append("Value:").append(NEWLINE);
        value.write(sb, indent + TAB + TAB);
    }
}
