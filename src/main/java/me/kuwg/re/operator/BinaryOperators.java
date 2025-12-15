package me.kuwg.re.operator;

import me.kuwg.re.error.errors.RInternalError;
import me.kuwg.re.operator.ops.add.AddBO;
import me.kuwg.re.operator.ops.add.SubBO;
import me.kuwg.re.operator.ops.bit.*;
import me.kuwg.re.operator.ops.comp.*;
import me.kuwg.re.operator.ops.log.AndBO;
import me.kuwg.re.operator.ops.log.OrBO;
import me.kuwg.re.operator.ops.mul.DivBO;
import me.kuwg.re.operator.ops.mul.ModBO;
import me.kuwg.re.operator.ops.mul.MulBO;

public final class BinaryOperators {
    private static final BinaryOperator[] OPERATORS;

    static {
        OPERATORS = new BinaryOperator[] {
                AddBO.INSTANCE,
                SubBO.INSTANCE,
                MulBO.INSTANCE,
                DivBO.INSTANCE,
                ModBO.INSTANCE,

                LessThanBO.INSTANCE,
                GreaterThanBO.INSTANCE,
                LessOrEqualBO.INSTANCE,
                GreaterOrEqualBO.INSTANCE,
                EqualsBO.INSTANCE,
                NotEqualsBO.INSTANCE,

                AndBO.INSTANCE,
                OrBO.INSTANCE,

                BitwiseAndBO.INSTANCE,
                BitwiseXorBO.INSTANCE,
                LeftShiftBO.INSTANCE,
                RightShiftBO.INSTANCE,
                UnsignedRightShiftBO.INSTANCE,
                BitwiseOrBO.INSTANCE,

        };
    }

    private BinaryOperators() {
        throw new RInternalError();
    }

    public static BinaryOperator getBySymbol(final String symbol) {
        for (final BinaryOperator operator : OPERATORS) {
            if (!operator.getSymbol().equals(symbol)) continue;
            return operator;
        }

        return null;
    }
}
