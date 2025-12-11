package me.kuwg.re.ast.nodes.len;

import me.kuwg.re.ast.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.len.RInvalidLenError;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.arr.ArrayType;
import me.kuwg.re.type.builtin.BuiltinTypes;
import me.kuwg.re.type.builtin.StrBuiltinType;

public class LenNode extends ValueNode {
    private final ValueNode value;

    public LenNode(final int line, final ValueNode value) {
        super(line, BuiltinTypes.INT.getType());
        this.value = value;
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        String valReg = value.compileAndGet(cctx);
        TypeRef valueType = value.getType();
        String longReg = cctx.nextRegister();

        if (valueType instanceof StrBuiltinType) {
            cctx.emit(longReg + " = call i64 @strlen(i8* " + valReg + ") ; compute string length");
        } else if (valueType instanceof ArrayType arrType) {
            int size = arrType.size();
            cctx.emit(longReg + " = add i64 0, " + size + " ; array length");
        } else {
            return new RInvalidLenError(valueType.getName(), line).raise();
        }

        String resultReg = cctx.nextRegister();

        cctx.emit(resultReg + " = trunc i64 " + longReg + " to i32");

        return resultReg;
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RValueMustBeUsedError("len", line).raise();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("LenNode:").append(NEWLINE);
        value.write(sb, indent + TAB);
    }
}
