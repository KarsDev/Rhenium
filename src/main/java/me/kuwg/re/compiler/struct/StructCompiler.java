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
    public static String compile(final String fileName, int line, CompilationContext cctx, RDefaultStruct struct, List<RParamValue> values, ValueNode node) {
        List<String> valueRegs = new ArrayList<>();

        for (RParamValue param : values) {
            if (param.name() != null) {
                return new RStructInitParamsError("Named parameters are not supported in struct initialization", fileName, line).raise();
            }

            valueRegs.add(param.value().compileAndGet(cctx));
        }

        for (RFunction ctor : struct.constructors()) {
            int expected = ctor.parameters().size() - 1;
            if (expected == valueRegs.size()) {
                return compileConstructor(fileName, line, ctor, struct, valueRegs, values, cctx, node);
            }
        }

        return compileNoConstructor(fileName, line, struct, valueRegs, values, cctx, node);
    }

    private static String compileConstructor(final String fileName, int line, RFunction constructor, RDefaultStruct struct, List<String> valueRegs, List<RParamValue> values, CompilationContext cctx, ValueNode node) {
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
                valueReg = new CastNode(fileName, line, expected, valueNode).compileAndGet(cctx);
            }

            args.add(expected.getLLVMName() + " " + valueReg);
        }

        cctx.emit("call void @" + constructor.llvmName + "(" + String.join(", ", args) + ")");

        String loaded = cctx.nextRegister();
        cctx.emit(loaded + " = load " + struct.type().getLLVMName() + ", " + node.toPtr(struct.type.getLLVMName()) + structPtr);

        node.setType(struct.type());
        return loaded;
    }

    private static String compileNoConstructor(final String fileName, int line, RDefaultStruct struct, List<String> valueRegs, List<RParamValue> values, CompilationContext cctx, ValueNode node) {
        List<RStructField> fields = struct.fields();

        if (fields.size() != valueRegs.size()) {
            return new RStructInitParamsError("Expected " + fields.size() + " fields but got " + valueRegs.size(), fileName, line).raise();
        }

        String structPtr = cctx.nextRegister();
        cctx.emit(structPtr + " = alloca " + struct.type().getLLVMName());

        for (int i = 0; i < fields.size(); i++) {
            RStructField field = fields.get(i);
            ValueNode valueNode = values.get(i).value();
            String valueReg = valueRegs.get(i);

            if (!field.type().equals(valueNode.getType())) {
                valueReg = new CastNode(fileName, line, field.type(), valueNode).compileAndGet(cctx);
            }

            String fieldPtr = cctx.nextRegister();
            cctx.emit(fieldPtr + " = getelementptr inbounds " + struct.type().getLLVMName() + ", " + node.toPtr(struct.type.getLLVMName()) + structPtr + ", i32 0, i32 " + i);


            TypeRef concrete = cctx.resolveConcrete(field.type());
            String ftln = concrete.getLLVMName();

            cctx.emit("store " + ftln + " " + valueReg + ", " + ftln + "* " + fieldPtr);
        }

        String loaded = cctx.nextRegister();
        cctx.emit(loaded + " = load " + struct.type().getLLVMName() + ", " + node.toPtr(struct.type.getLLVMName()) + structPtr);

        node.setType(struct.type());
        return loaded;
    }
}