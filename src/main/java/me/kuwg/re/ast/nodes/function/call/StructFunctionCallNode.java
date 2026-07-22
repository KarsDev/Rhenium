package me.kuwg.re.ast.nodes.function.call;

import me.kuwg.re.ast.nodes.struct.StructImplNode;
import me.kuwg.re.ast.nodes.variable.VariableReference;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.compiler.variable.RVariable;
import me.kuwg.re.error.errors.function.RFunctionNotFoundError;
import me.kuwg.re.error.errors.union.RUnionError;
import me.kuwg.re.error.errors.variable.RVariableNotFoundError;
import me.kuwg.re.error.errors.variable.RVariableTypeError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;
import me.kuwg.re.type.builtin.NoneBuiltinType;
import me.kuwg.re.type.ptr.PointerType;
import me.kuwg.re.type.struct.StructType;
import me.kuwg.re.type.union.UnionType;

import java.util.*;
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

            if (!(selfType instanceof StructType) && !(selfType instanceof UnionType)) {
                return new RVariableTypeError("struct", selfType.getName(), fileName, line).raise();
            }

            selfValue = var.addrReg();
        } else {
            String tmpVal = struct.compileAndGet(cctx);
            selfType = struct.getType();

            if (!(selfType instanceof StructType structType)) { // not var ref are always struct types
                return new RVariableTypeError("struct", selfType.getName(), fileName, line).raise();
            }

            String addr = cctx.nextRegister();
            cctx.emit(addr + " = alloca " + structType.getLLVMName());
            cctx.emit("store " + structType.getLLVMName() + " " + tmpVal + ", " + toPtr(structType.getLLVMName()) + addr);

            selfValue = addr;
        }

        TypeRef structType = selfType;

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

        if (structType instanceof UnionType ut) {
            return compileUnion(cctx, ut, selfValue, llvmArgs, argTypes);
        }

        mangled = StructImplNode.generateName(structType.getName(), name);
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
        sb.append(indent).append("Struct Function Call:").append(NEWLINE).append(indent).append(TAB).append("Struct:").append(NEWLINE);
        struct.write(sb, indent + TAB + TAB);
        sb.append(indent).append(TAB).append("Function Name: ").append(name).append(NEWLINE).append(indent).append(TAB).append("Params: ").append(NEWLINE);
        params.forEach(p -> p.write(sb, indent + TAB + TAB));
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

    private String compileUnion(final CompilationContext cctx, final UnionType unionType, final String selfValue, final List<String> llvmArgs, final List<TypeRef> argTypes) {
        Map<StructType, RFunction> functions = getUnionFunctions(unionType, name, argTypes, cctx);

        if (functions.isEmpty()) {
            setType(BuiltinTypes.NONE.getType());
            return new RFunctionNotFoundError(name, "()", fileName, line).raise();
        }

        TypeRef returnType = evalType(functions.values().iterator().next().returnType(), cctx, fileName, line);

        for (RFunction fn : functions.values()) {
            TypeRef rt = evalType(fn.returnType(), cctx, fileName, line);
            if (!Objects.equals(rt, returnType)) {
                return new RUnionError("Union member functions for '" + name + "' must have the same return type", fileName, line).raise();
            }
        }

        boolean returnsVoid = returnType instanceof NoneBuiltinType;
        String resultSlot = null;
        if (!returnsVoid) {
            resultSlot = cctx.nextRegister();
            cctx.emit(resultSlot + " = alloca " + returnType.getLLVMName());
        }

        String tagPtr = cctx.nextRegister();
        cctx.emit(tagPtr + " = getelementptr inbounds " + unionType.getLLVMName() + ", " + unionType.getLLVMName() + "* " + selfValue + ", i32 0, i32 0");

        String tag = cctx.nextRegister();
        cctx.emit(tag + " = load i32, i32* " + tagPtr);

        String endLabel = cctx.nextLabel("union.end");
        String badLabel = cctx.nextLabel("union.bad");

        List<String> caseLabels = new ArrayList<>(unionType.variants().size());

        cctx.emit("switch i32 " + tag + ", label %" + badLabel + " [");
        for (int i = 0; i < unionType.variants().size(); i++) {
            TypeRef variant = unionType.variants().get(i);
            if (!(variant instanceof StructType)) {
                return new RUnionError("Union variants used for method dispatch must be struct types", fileName, line).raise();
            }

            String caseLabel = cctx.nextLabel("union.case");
            caseLabels.add(caseLabel);
            cctx.emit("  i32 " + i + ", label %" + caseLabel);
        }
        cctx.emit("]");

        for (int i = 0; i < unionType.variants().size(); i++) {
            StructType variant = (StructType) unionType.variants().get(i);
            String caseLabel = caseLabels.get(i);

            cctx.emit(caseLabel + ":");

            String payloadPtr = cctx.nextRegister();
            cctx.emit(payloadPtr + " = getelementptr inbounds " + unionType.getLLVMName() + ", " + unionType.getLLVMName() + "* " + selfValue + ", i32 0, i32 1");

            String structPtr = cctx.nextRegister();
            cctx.emit(structPtr + " = bitcast [" + unionType.payloadSize() + " x i8]* " + payloadPtr + " to " + variant.getLLVMName() + "*");

            List<String> caseArgs = new ArrayList<>(llvmArgs);
            List<TypeRef> caseArgTypes = new ArrayList<>(argTypes);
            caseArgs.set(0, structPtr);
            caseArgTypes.set(0, new PointerType(variant));

            String mangled = StructImplNode.generateName(variant.getName(), name);
            RFunction fn = cctx.getFunction(mangled, caseArgTypes);

            if (fn == null) {
                return new RUnionError("Missing implementation for " + variant.getName() + "." + name, fileName, line).raise();
            }

            String callResult = "";
            StringBuilder call = new StringBuilder();
            call.append("call ").append(returnType.getLLVMName()).append(" @").append(fn.llvmName()).append("(");

            for (int i1 = 0; i1 < caseArgs.size(); i1++) {
                call.append(caseArgTypes.get(i1).getLLVMName()).append(" ").append(caseArgs.get(i1));
                if (i1 < caseArgs.size() - 1) call.append(", ");
            }

            call.append(")");

            if (returnType instanceof NoneBuiltinType) {
                cctx.emit(call.toString());
            } else {
                String result = cctx.nextRegister();
                cctx.emit(result + " = " + call);
                callResult = result;
            }

            if (!returnsVoid) {
                cctx.emit("store " + returnType.getLLVMName() + " " + callResult + ", " + returnType.getLLVMName() + "* " + resultSlot);
            }

            cctx.emit("br label %" + endLabel);
        }

        cctx.emit(badLabel + ":");
        cctx.emit("unreachable");

        cctx.emit(endLabel + ":");

        setType(returnType);

        if (returnsVoid) {
            return "";
        }

        String result = cctx.nextRegister();
        cctx.emit(result + " = load " + returnType.getLLVMName() + ", " + returnType.getLLVMName() + "* " + resultSlot);
        return result;
    }

    private Map<StructType, RFunction> getUnionFunctions(TypeRef selfType, String name, List<TypeRef> argTypes, CompilationContext cctx) {
        Map<StructType, RFunction> result = new LinkedHashMap<>();
        collectFunctions(selfType, name, argTypes, cctx, result);
        return result;
    }

    private void collectFunctions(TypeRef selfType, String name, List<TypeRef> argTypes, CompilationContext cctx, Map<StructType, RFunction> result) {
        if (selfType instanceof StructType structType) {
            List<TypeRef> params = new ArrayList<>(argTypes);
            params.set(0, new PointerType(structType));

            String mangled = StructImplNode.generateName(structType.getName(), name);
            RFunction fn = cctx.getFunction(mangled, params);

            if (fn == null) {
                throw new IllegalStateException("Missing implementation for " + structType.getName());
            }

            result.put(structType, fn);
            return;
        }

        if (selfType instanceof UnionType unionType) {
            for (TypeRef variant : unionType.variants()) {
                collectFunctions(variant, name, argTypes, cctx, result);
            }
        }
    }
}