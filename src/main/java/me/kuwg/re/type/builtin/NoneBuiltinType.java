package me.kuwg.re.type.builtin;

import me.kuwg.re.type.TypeRef;

public final class NoneBuiltinType extends BuiltinType {
    static final NoneBuiltinType INSTANCE = new NoneBuiltinType();

    private NoneBuiltinType() {
    }

    @Override
    public boolean isCompatibleWith(final TypeRef other) {
        return true;
    }

    @Override
    public long getSize() {
        return 0;
    }

    @Override
    public String getName() {
        return "none";
    }

    @Override
    public String getLLVMName() {
        return "void";
    }
}
