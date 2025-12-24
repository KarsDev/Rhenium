package me.kuwg.re.type.builtin;

import me.kuwg.re.type.TypeRef;

public class ShortBuiltinType extends BuiltinType {
    static final BuiltinType INSTANCE = new ShortBuiltinType();

    private ShortBuiltinType() {
    }

    @Override
    public boolean isCompatibleWith(final TypeRef other) {
        return other instanceof ShortBuiltinType || ByteBuiltinType.INSTANCE.isCompatibleWith(other);
    }

    @Override
    public long getSize() {
        return 2;
    }

    @Override
    public String getName() {
        return "short";
    }

    @Override
    public String getLLVMName() {
        return "i16";
    }
}
