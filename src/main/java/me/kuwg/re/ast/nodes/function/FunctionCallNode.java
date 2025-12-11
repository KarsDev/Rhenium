package me.kuwg.re.ast.nodes.function;

import me.kuwg.re.ast.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.error.errors.function.RFunctionIsVoidError;
import me.kuwg.re.error.errors.function.RFunctionNotFoundError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.NoneBuiltinType;

import java.util.ArrayList;
import java.util.List;

public class FunctionCallNode extends ValueNode {
    private final String name;
    private final List<ValueNode> parameters;

    public FunctionCallNode(final int line, final String name, final List<ValueNode> parameters) {
        super(line);
        this.name = name;
        this.parameters = parameters;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        compile0(cctx, false);
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        return compile0(cctx, true);
    }

    private String compile0(final CompilationContext cctx, boolean getting) {
        List<String> argRegs = new ArrayList<>(parameters.size());
        List<TypeRef> types = new ArrayList<>(parameters.size());
        for (ValueNode param : parameters) {
            argRegs.add(param.compileAndGet(cctx));
            types.add(param.getType());
        }

        RFunction func = cctx.getFunction(name, types);

        var pst = new StringBuilder("(");
        for (int i = 0; i < types.size(); i++) {
            pst.append(types.get(i).getName());
            if (i < types.size() - 1) pst.append(", ");
        }
        pst.append(")");
        String paramsString = pst.toString();

        if (func == null) {
            return new RFunctionNotFoundError(name, paramsString, line).raise();
        }

        if (func.returnType() instanceof NoneBuiltinType && getting) {
            return new RFunctionIsVoidError(name, paramsString, line).raise();
        }

        for (int i = 0; i < parameters.size(); i++) {
            var targetType = func.parameters().get(i).type();
            if (!types.get(i).equals(targetType)) {
                ValueNode castNode = new me.kuwg.re.ast.nodes.cast.CastNode(
                        line,
                        targetType,
                        parameters.get(i)
                );
                argRegs.set(i, castNode.compileAndGet(cctx));
                types.set(i, targetType);
            }
        }

        StringBuilder call = new StringBuilder();
        String resultReg = null;

        if (!(func.returnType() instanceof NoneBuiltinType)) {
            resultReg = cctx.nextRegister();
            call.append(resultReg).append(" = ");
        }

        call.append("call ").append(func.returnType().getLLVMName()).append(" @").append(func.llvmName()).append("(");

        for (int i = 0; i < parameters.size(); i++) {
            call.append(types.get(i).getLLVMName()).append(" ").append(argRegs.get(i));
            if (i < parameters.size() - 1) {
                call.append(", ");
            }
        }

        call.append(")");
        cctx.emit(call.toString());

        setType(func.returnType());

        if (func.returnType() instanceof NoneBuiltinType) {
            return "%void_" + cctx.nextRegister();
        }

        return resultReg;
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Function Call: ").append(name).append(NEWLINE).append(indent).append(TAB).append("Parameters: ").append(NEWLINE);
        parameters.forEach(p -> p.write(sb, indent + TAB + TAB));
    }
}
