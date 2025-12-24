package me.kuwg.re.type.builtin;

import me.kuwg.re.type.TypeRef;

public class LongBuiltinType extends BuiltinType {
    static final BuiltinType INSTANCE = new LongBuiltinType();

    private LongBuiltinType() {
    }

    @Override
    public boolean isCompatibleWith(final TypeRef other) {
        return other instanceof LongBuiltinType || IntBuiltinType.INSTANCE.isCompatibleWith(other);
    }

    @Override
    public long getSize() {
        return 8;
    }

    @Override
    public String getName() {
        return "long";
    }

    @Override
    public String getLLVMName() {
        return "i64";
    }
}
