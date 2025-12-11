package me.kuwg.re.type.builtin;

import me.kuwg.re.type.TypeRef;

public class StrBuiltinType extends BuiltinType {
    static final BuiltinType INSTANCE = new StrBuiltinType();

    private StrBuiltinType() {
    }

    @Override
    public boolean isCompatibleWith(final TypeRef other) {
        return other instanceof StrBuiltinType;
    }

    @Override
    public int getSize() {
        return 8;
    }

    @Override
    public String getName() {
        return "str";
    }

    @Override
    public String getLLVMName() {
        return "i8*";
    }
}
