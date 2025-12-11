package me.kuwg.re.type.builtin;

import me.kuwg.re.type.TypeRef;

public class CharBuiltinType extends BuiltinType {
    static final BuiltinType INSTANCE = new CharBuiltinType();

    private CharBuiltinType() {
    }

    @Override
    public boolean isCompatibleWith(final TypeRef other) {
        return other instanceof CharBuiltinType || ByteBuiltinType.INSTANCE.isCompatibleWith(other);
    }

    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public String getName() {
        return "char";
    }

    @Override
    public String getLLVMName() {
        return "i8";
    }
}
