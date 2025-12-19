package me.kuwg.re.ast.nodes.expression;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.expr.RUnsupportedUnaryExpressionError;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.ByteBuiltinType;
import me.kuwg.re.type.builtin.IntBuiltinType;
import me.kuwg.re.type.builtin.LongBuiltinType;
import me.kuwg.re.type.builtin.ShortBuiltinType;

public class BitwiseNotNode extends ValueNode {
    private final ValueNode value;

    public BitwiseNotNode(final int line, final ValueNode value) {
        super(line);
        this.value = value;
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        String valueReg = value.compileAndGet(cctx);
        TypeRef type = value.getType();

        if (!(type instanceof ByteBuiltinType
                || type instanceof ShortBuiltinType
                || type instanceof IntBuiltinType
                || type instanceof LongBuiltinType)) {
            return new RUnsupportedUnaryExpressionError("~", type, line).raise();
        }

        String mask;
        if (type instanceof ByteBuiltinType) mask = "255";            // 0xFF
        else if (type instanceof ShortBuiltinType) mask = "65535";    // 0xFFFF
        else if (type instanceof IntBuiltinType) mask = "4294967295"; // 0xFFFFFFFF
        else mask = "18446744073709551615";                           // 0xFFFFFFFFFFFFFFFF for long

        String resReg = cctx.nextRegister();
        cctx.emit(resReg + " = xor " + type.getLLVMName() + " " + valueReg + ", " + mask);

        setType(type);
        return resReg;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RValueMustBeUsedError("Bitwise NOT", line).raise();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Unary Operator: Bitwise NOT").append(NEWLINE);
        value.write(sb, indent + TAB);
    }
}
