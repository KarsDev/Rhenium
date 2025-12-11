package me.kuwg.re.ast.nodes.function;

import me.kuwg.re.ast.nodes.struct.StructImplNode;
import me.kuwg.re.ast.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.error.errors.function.RFunctionNotFoundError;
import me.kuwg.re.error.errors.variable.RVariableTypeError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.NoneBuiltinType;
import me.kuwg.re.type.struct.StructType;

import java.util.ArrayList;
import java.util.List;

public class StructFunctionCallNode extends ValueNode {

    private final ValueNode struct;
    private final String name;
    private final List<ValueNode> params;

    public StructFunctionCallNode(final int line, final ValueNode struct,
                                  final String name, final List<ValueNode> params) {
        super(line);
        this.struct = struct;
        this.name = name;
        this.params = params;
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        String selfValue = struct.compileAndGet(cctx);
        TypeRef structType = struct.getType();

        if (!(structType instanceof StructType structObj)) {
            return new RVariableTypeError("struct", structType.getName(), line).raise();
        }

        String mangled = StructImplNode.generateName(structObj.name(), name);

        List<String> llvmArgs = new ArrayList<>();
        List<TypeRef> argTypes = new ArrayList<>();

        llvmArgs.add(selfValue);
        argTypes.add(structType);

        for (ValueNode p : params) {
            llvmArgs.add(p.compileAndGet(cctx));
            argTypes.add(p.getType());
        }

        RFunction fn = cctx.getFunction(mangled, argTypes);

        if (fn == null) {
            var sb = new StringBuilder("(");
            for (int i = 0; i < argTypes.size(); i++) {
                sb.append(argTypes.get(i).getName());
                if (i < argTypes.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append(")");
            return new RFunctionNotFoundError(
                    name,
                    sb.toString(),
                    line
            ).raise();
        }

        StringBuilder call = new StringBuilder();
        call.append("call ")
                .append(fn.returnType().getLLVMName())
                .append(" @")
                .append(mangled)
                .append("(");

        for (int i = 0; i < llvmArgs.size(); i++) {
            call.append(argTypes.get(i).getLLVMName())
                    .append(" ")
                    .append(llvmArgs.get(i));

            if (i < llvmArgs.size() - 1)
                call.append(", ");
        }

        call.append(")");

        if (fn.returnType() instanceof NoneBuiltinType) {
            cctx.emit(call.toString());
            return "";
        }

        String result = cctx.nextRegister();
        cctx.emit(result + " = " + call);
        return result;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        compileAndGet(cctx);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent)
                .append("StructFunctionCall: ")
                .append(struct)
                .append(".")
                .append(name)
                .append("(...)\n");
    }
}
