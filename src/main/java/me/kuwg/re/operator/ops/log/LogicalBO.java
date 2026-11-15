package me.kuwg.re.operator.ops.log;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.expr.RUnsupportedBinaryExpressionError;
import me.kuwg.re.operator.BinaryOperator;
import me.kuwg.re.operator.BinaryOperatorContext;
import me.kuwg.re.operator.result.BOResult;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.BuiltinTypes;

/// Shared implementation of the boolean operators {@code and} / {@code or}.
abstract class LogicalBO extends BinaryOperator {
    private final String llvmOp;

    LogicalBO(final int precedence, final String symbol, final String llvmOp) {
        super(precedence, symbol);
        this.llvmOp = llvmOp;
    }

    protected abstract boolean apply(boolean l, boolean r);

    private static boolean isBool(final TypeRef t) {
        return t == BuiltinTypes.BOOL.getType();
    }

    private static Boolean parse(final String s) {
        if ("true".equals(s)) return Boolean.TRUE;
        if ("false".equals(s)) return Boolean.FALSE;
        return null;
    }

    @Override
    public BOResult compile(final BinaryOperatorContext c) {
        final TypeRef leftType = c.leftType();
        final TypeRef rightType = c.rightType();

        if (!isBool(leftType) || !isBool(rightType)) {
            return new RUnsupportedBinaryExpressionError(
                    leftType.getName(), getSymbol(), rightType.getName(), c.fileName(), c.line()
            ).raise();
        }

        final String resultReg = c.cctx().nextRegister();
        c.cctx().emit(resultReg + " = " + llvmOp + " i1 " + c.leftReg() + ", " + c.rightReg());

        return res(resultReg, BuiltinTypes.BOOL.getType());
    }

    @Override
    public String compileToConstant(final ValueNode left, final ValueNode right, final CompilationContext cctx) {
        final TypeRef leftType = left.getType();
        final TypeRef rightType = right.getType();

        if (isBool(leftType) && isBool(rightType)) {
            final Boolean l = parse(left.compileToConstant(cctx));
            final Boolean r = parse(right.compileToConstant(cctx));

            if (l != null && r != null) {
                return Boolean.toString(apply(l, r));
            }
        }

        return new RUnsupportedBinaryExpressionError(
                leftType.getName(), getSymbol(), rightType.getName(), left.getFileName(), left.getLine()
        ).raise();
    }
}
