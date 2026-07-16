package me.kuwg.re.ast.nodes.function.call;

import me.kuwg.re.ast.nodes.struct.StructImplNode;
import me.kuwg.re.ast.nodes.variable.VariableReference;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.error.errors.function.RFunctionNotFoundError;
import me.kuwg.re.error.errors.variable.RVariableNotFoundError;
import me.kuwg.re.error.errors.variable.RVariableTypeError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;
import me.kuwg.re.type.builtin.NoneBuiltinType;
import me.kuwg.re.type.ptr.PointerType;
import me.kuwg.re.type.struct.StructType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class StructFunctionCallNode extends VariableReference {
    private final ValueNode struct;
    private final String name;
    private final List<ValueNode> params;

    public StructFunctionCallNode(final String fileName, final int line, final ValueNode struct, final String name, final List<ValueNode> params) {
        super(fileName, line);
        this.struct = struct;
        this.name = name;
        this.params = params;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        struct.replaceGenerics(generics, cctx);
        params.forEach(p -> p.replaceGenerics(generics, cctx));
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        cctx.emit("; Struct (member) function call");
        RFunction fn;
        String mangled;

        String selfValue;
        TypeRef selfType;

        if (struct instanceof VariableReference vr) {
            var var = vr.getVariable(cctx);

            if (var == null) {
                return new RVariableNotFoundError(vr.getCompleteName(), fileName, line).raise();
            }

            selfType = evalType(var.type(), cctx, fileName, line);

            if (!(selfType instanceof StructType)) {
                return new RVariableTypeError("struct", selfType.getName(), fileName, line).raise();
            }

            selfValue = var.addrReg();
        } else {
            String tmpVal = struct.compileAndGet(cctx);
            selfType = struct.getType();

            if (!(selfType instanceof StructType structType)) {
                return new RVariableTypeError("struct", selfType.getName(), fileName, line).raise();
            }

            String addr = cctx.nextRegister();
            cctx.emit(addr + " = alloca " + structType.getLLVMName());
            cctx.emit("store " + structType.getLLVMName() + " " + tmpVal + ", " + toPtr(structType.getLLVMName()) + addr);

            selfValue = addr;
        }

        StructType structType = (StructType) selfType;

        if (!(struct instanceof VariableReference)) {
            String addr = cctx.nextRegister();
            cctx.emit(addr + " = alloca " + structType.getLLVMName());
            cctx.emit("store " + structType.getLLVMName() + " " + selfValue + ", " + toPtr(structType.getLLVMName()) + addr);
            selfValue = addr;
        }

        List<String> llvmArgs = new ArrayList<>();
        List<TypeRef> argTypes = new ArrayList<>();

        llvmArgs.add(selfValue);
        argTypes.add(new PointerType(structType));

        for (ValueNode p : params) {
            llvmArgs.add(p.compileAndGet(cctx));
            argTypes.add(p.getType());
        }

        mangled = StructImplNode.generateName(structType.name(), name);
        fn = cctx.getFunction(mangled, argTypes);

        if (fn != null) {
            var rt = evalType(fn.returnType(), cctx, fileName, line);
            setType(rt);

            StringBuilder call = new StringBuilder();
            call.append("call ").append(rt.getLLVMName()).append(" @").append(fn.llvmName()).append("(");

            for (int i = 0; i < llvmArgs.size(); i++) {
                call.append(argTypes.get(i).getLLVMName()).append(" ").append(llvmArgs.get(i));
                if (i < llvmArgs.size() - 1) call.append(", ");
            }

            call.append(")");

            if (rt instanceof NoneBuiltinType) {
                cctx.emit(call.toString());
                return "";
            }

            String result = cctx.nextRegister();
            cctx.emit(result + " = " + call);
            return result;
        }

        setType(BuiltinTypes.NONE.getType());
        if (name.equals("delete")) return "";

        StringBuilder sig = new StringBuilder("(");
        for (int i = 0; i < argTypes.size(); i++) {
            sig.append(argTypes.get(i).getName());
            if (i < argTypes.size() - 1) sig.append(", ");
        }
        sig.append(")");
        return new RFunctionNotFoundError(name, sig.toString(), fileName, line).raise();
    }

    @Override
    public void compile(final CompilationContext cctx) {
        compileAndGet(cctx);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("StructFunctionCall: ").append(struct).append(".").append(name).append("(...)\n");
    }

    @Override
    public RVariable getVariable(CompilationContext cctx) {
        String value = compileAndGet(cctx);
        TypeRef type = getType();

        String addr = cctx.nextRegister();

        cctx.emit(addr + " = alloca " + type.getLLVMName());
        cctx.emit("store " + type.getLLVMName() + " " + value + ", " + toPtr(type.getLLVMName()) + addr);

        return new RVariable("\"" + getCompleteName() + "\"", false, false, type, addr, value);
    }

    @Override
    public String getCompleteName() {
        return "SFC#" + name + "(...)";
    }

    @Override
    public String getSimpleName() {
        return name;
    }

    @Override
    public StructFunctionCallNode clone() {
        List<ValueNode> paramsCloned = new ArrayList<>();
        IntStream.range(0, params.size()).forEach(i -> paramsCloned.add(i, params.get(i).clone()));
        return new StructFunctionCallNode(fileName, line, struct, name, paramsCloned);
    }
}