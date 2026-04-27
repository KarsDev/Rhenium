package me.kuwg.re.compiler.struct;

import me.kuwg.re.ast.nodes.cast.CastNode;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.compiler.variable.RParamValue;
import me.kuwg.re.compiler.variable.RStructField;
import me.kuwg.re.error.errors.struct.RStructInitParamsError;
import me.kuwg.re.type.TypeRef;

import java.util.ArrayList;
import java.util.List;

public final class StructCompiler {
    public static String compile(int line, CompilationContext cctx, RDefaultStruct struct, List<RParamValue> values, ValueNode node) {
        List<String> valueRegs = new ArrayList<>();

        for (RParamValue param : values) {
            if (param.name() != null) {
                return new RStructInitParamsError("Named parameters are not supported in struct initialization", line).raise();
            }

            valueRegs.add(param.value().compileAndGet(cctx));
        }

        for (RFunction ctor : struct.constructors()) {
            int expected = ctor.parameters().size() - 1;
            if (expected == valueRegs.size()) {
                return compileConstructor(line, ctor, struct, valueRegs, values, cctx, node);
            }
        }

        return compileNoConstructor(line, struct, valueRegs, values, cctx, node);
    }

    private static String compileConstructor(int line, RFunction constructor, RDefaultStruct struct, List<String> valueRegs, List<RParamValue> values, CompilationContext cctx, ValueNode node) {
        String structPtr = cctx.nextRegister();
        cctx.emit(structPtr + " = alloca " + struct.type().getLLVMName());

        var params = constructor.parameters();
        List<String> args = new ArrayList<>();
        args.add(struct.type().getLLVMName() + "* " + structPtr);

        for (int i = 1; i < params.size(); i++) {
            TypeRef expected = params.get(i).type();
            ValueNode valueNode = values.get(i - 1).value();
            String valueReg = valueRegs.get(i - 1);

            if (!expected.equals(valueNode.getType())) {
                valueReg = new CastNode(line, expected, valueNode).compileAndGet(cctx);
            }

            args.add(expected.getLLVMName() + " " + valueReg);
        }

        cctx.emit("call void @" + constructor.llvmName + "(" + String.join(", ", args) + ")");

        node.setType(struct.type());
        return structPtr;
    }

    private static String compileNoConstructor(int line, RDefaultStruct struct, List<String> valueRegs, List<RParamValue> values, CompilationContext cctx, ValueNode node) {
        List<RStructField> fields = struct.fields();

        if (fields.size() != valueRegs.size()) {
            return new RStructInitParamsError("Expected " + fields.size() + " fields but got " + valueRegs.size(), line).raise();
        }

        String structPtr = cctx.nextRegister();
        cctx.emit(structPtr + " = alloca " + struct.type().getLLVMName());

        for (int i = 0; i < fields.size(); i++) {
            RStructField field = fields.get(i);
            ValueNode valueNode = values.get(i).value();
            String valueReg = valueRegs.get(i);

            if (!field.type().equals(valueNode.getType())) {
                valueReg = new CastNode(line, field.type(), valueNode).compileAndGet(cctx);
            }

            String fieldPtr = cctx.nextRegister();
            cctx.emit(fieldPtr + " = getelementptr inbounds " + struct.type().getLLVMName() + ", " + struct.type().getLLVMName() + "* " + structPtr + ", i32 0, i32 " + i);

            cctx.emit("store " + field.type().getLLVMName() + " " + valueReg + ", " + field.type().getLLVMName() + "* " + fieldPtr);
        }

        node.setType(struct.type());
        return structPtr;
    }
}