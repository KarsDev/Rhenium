package me.kuwg.re.ast.nodes.expression;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.expr.RUnsupportedUnaryExpressionError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;

import java.util.Map;

public class BitwiseNotNode extends ValueNode {
    private final ValueNode value;

    public BitwiseNotNode(final String fileName, final int line, final ValueNode value) {
        super(fileName, line);
        this.value = value;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        value.replaceGenerics(generics, cctx);
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        String valueReg = value.compileAndGet(cctx);
        TypeRef type = value.getType();

        if (!(type == BuiltinTypes.BYTE.getType()
                || type == BuiltinTypes.SHORT.getType()
                || type == BuiltinTypes.INT.getType()
                || type == BuiltinTypes.LONG.getType())) {
            return new RUnsupportedUnaryExpressionError("~", type, fileName, line).raise();
        }

        String mask;
        if (type == BuiltinTypes.BYTE.getType()) mask = "255";            // 0xFF
        else if (type == BuiltinTypes.SHORT.getType()) mask = "65535";    // 0xFFFF
        else if (type == BuiltinTypes.INT.getType()) mask = "4294967295"; // 0xFFFFFFFF
        else mask = "18446744073709551615";                               // 0xFFFFFFFFFFFFFFFF for long

        String resReg = cctx.nextRegister();
        cctx.emit("; Bitwise not");
        cctx.emit(resReg + " = xor " + type.getLLVMName() + " " + valueReg + ", " + mask);

        setType(type);
        return resReg;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        compileAndGet(cctx);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Unary Operator: Bitwise NOT").append(NEWLINE);
        value.write(sb, indent + TAB);
    }

    @Override
    public BitwiseNotNode clone() {
        return new BitwiseNotNode(fileName, line, value.clone());
    }

    @Override
    public boolean isConstant(final CompilationContext cctx) {
        return value.isConstant(cctx);
    }

    @Override
    public String compileToConstant(final CompilationContext cctx) {
        final TypeRef type = value.getType();

        if (!(type == BuiltinTypes.BYTE.getType()
                || type == BuiltinTypes.SHORT.getType()
                || type == BuiltinTypes.INT.getType()
                || type == BuiltinTypes.LONG.getType())) {
            return new RUnsupportedUnaryExpressionError("~", type, fileName, line).raise();
        }

        final String constant = value.compileToConstant(cctx);

        try {
            if (type == BuiltinTypes.BYTE.getType()) {
                return Byte.toString((byte) ~Byte.parseByte(constant));
            }

            if (type == BuiltinTypes.SHORT.getType()) {
                return Short.toString((short) ~Short.parseShort(constant));
            }

            if (type == BuiltinTypes.INT.getType()) {
                return Integer.toString(~Integer.parseInt(constant));
            }

            return Long.toString(~Long.parseLong(constant));
        } catch (NumberFormatException ignored) {
        }

        return new RUnsupportedUnaryExpressionError("~", type, fileName, line).raise();
    }
}
