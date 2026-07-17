package me.kuwg.re.type.builtin;

import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.iterable.IterableTypeRef;

public final class StrBuiltinType extends BuiltinType implements IterableTypeRef {
    static final BuiltinType INSTANCE = new StrBuiltinType();

    private StrBuiltinType() {
    }

    @Override
    public boolean isCompatibleWith(final TypeRef other) {
        return other instanceof StrBuiltinType;
    }

    @Override
    public long getSize() {
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

    @Override
    public String getZeroValue() {
        return "null";
    }
}
