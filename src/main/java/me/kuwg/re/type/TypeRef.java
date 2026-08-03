package me.kuwg.re.type;

import me.kuwg.re.type.builtin.BuiltinTypes;
import me.kuwg.re.type.ptr.PointerType;

import java.util.function.Function;

public interface TypeRef {

    boolean isPrimitive();

    boolean isCompatibleWith(TypeRef other);

    long getSize();

    String getName();

    String getLLVMName();

    String getMangledName();

    @Override boolean equals(Object other);

    String getZeroValue();

    default long getAlignment() {
        return getSize();
    }

    default boolean isPointer() {
        return this instanceof PointerType || this  == BuiltinTypes.ANYPTR.getType();
    }

    TypeRef resolve(Function<String, TypeRef> resolver);
}
