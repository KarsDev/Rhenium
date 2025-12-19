package me.kuwg.re.type;

import me.kuwg.re.type.builtin.AnyPointerType;
import me.kuwg.re.type.ptr.PointerType;

public interface TypeRef {

    boolean isPrimitive();

    boolean isCompatibleWith(TypeRef other);

    int getSize();

    String getName();

    String getLLVMName();

    boolean equals(TypeRef other);

    default boolean isPointer() {
        return this instanceof PointerType || this instanceof AnyPointerType;
    }
}
