package me.kuwg.re.ast.nodes.function;

import me.kuwg.re.ast.nodes.struct.StructImplNode;
import me.kuwg.re.ast.nodes.variable.VariableReference;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.error.errors.function.RFunctionNotFoundError;
import me.kuwg.re.error.errors.variable.RVariableTypeError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.NoneBuiltinType;
import me.kuwg.re.type.ptr.PointerType;
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
        if (!(struct instanceof VariableReference vr)) {
            return new RVariableTypeError("struct", struct.getType().getName(), line).raise();
        }

        RVariable selfVar = vr.getVariable(cctx);
        if (selfVar == null) {
            return new RVariableTypeError("struct", "unknown", line).raise();
        }

        TypeRef selfVarType = selfVar.type();
        if (!(selfVarType instanceof StructType structType)) {
            return new RVariableTypeError("struct", selfVarType.getName(), line).raise();
        }

        RFunction fn;
        String mangled;

        StructType current = structType;

        while (true) {
            List<String> llvmArgs = new ArrayList<>();
            List<TypeRef> argTypes = new ArrayList<>();

            // self pointer
            llvmArgs.add(selfVar.valueReg());
            argTypes.add(new PointerType(current));

            // parameters
            for (ValueNode p : params) {
                llvmArgs.add(p.compileAndGet(cctx));
                argTypes.add(p.getType());
            }

            mangled = StructImplNode.generateName(current.name(), name);
            fn = cctx.getFunction(mangled, argTypes);

            if (fn != null) {
                setType(fn.returnType());

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
                    if (i < llvmArgs.size() - 1) call.append(", ");
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

            if (current.inherited() == null) {
                StringBuilder sig = new StringBuilder("(");
                for (int i = 0; i < argTypes.size(); i++) {
                    sig.append(argTypes.get(i).getName());
                    if (i < argTypes.size() - 1) sig.append(", ");
                }
                sig.append(")");
                return new RFunctionNotFoundError(name, sig.toString(), line).raise();
            }

            current = current.inherited();
        }
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
