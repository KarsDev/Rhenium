package me.kuwg.re.type.builtin;

import me.kuwg.re.type.TypeRef;

public class NoneBuiltinType extends BuiltinType {
    public static final NoneBuiltinType INSTANCE = new NoneBuiltinType();

    private NoneBuiltinType() {
    }

    @Override
    public boolean isCompatibleWith(final TypeRef other) {
        return true;
    }

    @Override
    public int getSize() {
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
