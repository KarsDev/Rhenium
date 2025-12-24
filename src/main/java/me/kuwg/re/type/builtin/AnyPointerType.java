package me.kuwg.re.type.builtin;

import me.kuwg.re.type.TypeRef;

public class AnyPointerType extends BuiltinType {
    static final BuiltinType INSTANCE = new AnyPointerType();

    private AnyPointerType() {
    }

    @Override
    public boolean isCompatibleWith(final TypeRef other) {
        return other.isPointer();
    }

    @Override
    public long getSize() {
        return 8;
    }

    @Override
    public String getName() {
        return "anyptr";
    }

    @Override
    public String getLLVMName() {
        return "i8*";
    }

    @Override
    public boolean equals(final TypeRef other) {
        return other instanceof AnyPointerType;
    }
}
