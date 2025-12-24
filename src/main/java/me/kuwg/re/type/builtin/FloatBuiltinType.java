package me.kuwg.re.type.builtin;

import me.kuwg.re.type.TypeRef;

public class FloatBuiltinType extends BuiltinType {
    static final BuiltinType INSTANCE = new FloatBuiltinType();

    private FloatBuiltinType() {
    }

    @Override
    public boolean isCompatibleWith(final TypeRef other) {
        return other instanceof FloatBuiltinType || IntBuiltinType.INSTANCE.isCompatibleWith(other);
    }

    @Override
    public long getSize() {
        return 4;
    }

    @Override
    public String getName() {
        return "float";
    }

    @Override
    public String getLLVMName() {
        return "float";
    }
}
