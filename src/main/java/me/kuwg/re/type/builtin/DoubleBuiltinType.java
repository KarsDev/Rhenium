package me.kuwg.re.type.builtin;

import me.kuwg.re.type.TypeRef;

public class DoubleBuiltinType extends BuiltinType {
    static final BuiltinType INSTANCE = new DoubleBuiltinType();

    private DoubleBuiltinType() {
    }

    @Override
    public boolean isCompatibleWith(final TypeRef other) {
        return other instanceof DoubleBuiltinType || FloatBuiltinType.INSTANCE.isCompatibleWith(other);
    }

    @Override
    public long getSize() {
        return 8;
    }

    @Override
    public String getName() {
        return "double";
    }

    @Override
    public String getLLVMName() {
        return "double";
    }
}
