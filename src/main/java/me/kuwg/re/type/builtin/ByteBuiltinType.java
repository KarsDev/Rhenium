package me.kuwg.re.type.builtin;

import me.kuwg.re.type.TypeRef;

public final class ByteBuiltinType extends BuiltinType {
    static final BuiltinType INSTANCE = new ByteBuiltinType();

    private ByteBuiltinType() {
    }

    @Override
    public boolean isCompatibleWith(final TypeRef other) {
        return other instanceof ByteBuiltinType;
    }

    @Override
    public long getSize() {
        return 1;
    }

    @Override
    public String getName() {
        return "byte";
    }

    @Override
    public String getLLVMName() {
        return "i8";
    }
}
