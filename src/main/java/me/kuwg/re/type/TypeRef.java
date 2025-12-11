package me.kuwg.re.type;

public interface TypeRef {

    boolean isPrimitive();

    boolean isCompatibleWith(TypeRef other);

    int getSize();

    String getName();

    String getLLVMName();

    boolean equals(TypeRef other);
}
