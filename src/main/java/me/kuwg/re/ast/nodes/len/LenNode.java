package me.kuwg.re.ast.nodes.len;

import me.kuwg.re.ast.nodes.constants.StringNode;
import me.kuwg.re.ast.nodes.function.call.StructFunctionCallNode;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.constant.RNotConstantError;
import me.kuwg.re.error.errors.len.RInvalidLenError;
import me.kuwg.re.error.errors.variable.RVariableTypeError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;
import me.kuwg.re.type.iterable.arr.ArrayType;
import me.kuwg.re.type.struct.StructType;

import java.util.ArrayList;
import java.util.Map;

public class LenNode extends ValueNode {
    private final ValueNode value;

    public LenNode(final String fileName, final int line, final ValueNode value) {
        super(fileName, line, BuiltinTypes.INT.getType());
        this.value = value;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        value.replaceGenerics(generics, cctx);
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        String valReg = value.compileAndGet(cctx);
        TypeRef valueType = value.getType();
        String longReg = cctx.nextRegister();

        cctx.emit("; Len of " + valueType.getName());

        if (valueType == BuiltinTypes.STR.getType()) {
            cctx.emit(longReg + " = call i64 @strlen(i8* " + valReg + ") ; compute string length");
        } else if (valueType instanceof ArrayType arrType) {
            long size = arrType.size();

            if (arrType.isDynamic()) {
                new RInvalidLenError("Dynamic array", fileName, line).raise();
            } else {
                cctx.emit(longReg + " = add i64 0, " + size + " ; array length");
            }
        } else if (valueType instanceof StructType) {
            StructFunctionCallNode call = new StructFunctionCallNode(fileName, line, value, "length", new ArrayList<>());

            String result = call.compileAndGet(cctx);
            if (!call.getType().isCompatibleWith(BuiltinTypes.INT.getType())) {
                return new RVariableTypeError("Expected 'int' type for len function", fileName, line).raise();
            }

            return result;
        } else {
            return new RInvalidLenError(valueType.getName(), fileName, line).raise();
        }

        String resultReg = cctx.nextRegister();

        cctx.emit(resultReg + " = trunc i64 " + longReg + " to i32");

        return resultReg;
    }

    @Override
    public boolean isConstant(final CompilationContext cctx) {
        return value.isConstant(cctx);
    }

    @Override
    public String compileToConstant(final CompilationContext cctx) {
        if (value instanceof StringNode stringNode) {
            return String.valueOf(stringNode.getValue().length());
        }

        TypeRef valueType = value.getType();

        if (valueType instanceof ArrayType arrType) {
            if (arrType.isDynamic()) {
                return new RInvalidLenError(
                        "Dynamic array", fileName, line
                ).raise();
            }

            return String.valueOf(arrType.size());
        }

        return new RNotConstantError(
                "Cannot compile to constant the len value. Only string literals and static array types are supported.",
                fileName,
                line
        ).raise();
    }

    @Override
    public void compile(final CompilationContext cctx) {
        compileAndGet(cctx);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("LenNode:").append(NEWLINE);
        value.write(sb, indent + TAB);
    }

    @Override
    public LenNode clone() {
        return new LenNode(fileName, line, value.clone());
    }
}
