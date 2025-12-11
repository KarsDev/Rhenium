package me.kuwg.re.type.builtin;

import me.kuwg.re.type.TypeRef;

public class IntBuiltinType extends BuiltinType {
    static final BuiltinType INSTANCE = new IntBuiltinType();

    private IntBuiltinType() {
    }

    @Override
    public boolean isCompatibleWith(final TypeRef other) {
        return other instanceof IntBuiltinType || ShortBuiltinType.INSTANCE.isCompatibleWith(other);
    }

    @Override
    public int getSize() {
        return 4;
    }

    @Override
    public String getName() {
        return "int";
    }

    @Override
    public String getLLVMName() {
        return "i32";
    }
}
