package me.kuwg.re.type.undefined;

import me.kuwg.re.error.errors.RInternalError;
import me.kuwg.re.error.errors.type.RTypeNotResolvedError;
import me.kuwg.re.type.TypeRef;

import java.util.function.Function;

public record UndefinedType(String name, int line, String fileName) implements TypeRef {
    @Override
    public boolean isPrimitive() {
        throw new RInternalError();
    }

    @Override
    public boolean isCompatibleWith(final TypeRef other) {
        throw new RInternalError();
    }

    @Override
    public long getSize() {
        throw new RInternalError();
    }

    @Override
    public String getName() {
        throw new RInternalError();
    }

    @Override
    public String getLLVMName() {
        throw new RInternalError();
    }

    @Override
    public String getMangledName() {
        throw new RInternalError();
    }

    @Override
    public boolean equals(final TypeRef other) {
        throw new RInternalError();
    }

    @Override
    public String getZeroValue() {
        throw new RInternalError();
    }

    @Override
    public TypeRef resolve(final Function<String, TypeRef> resolver) {
        TypeRef resolved = resolver.apply(name);
        if (resolved == null) return new RTypeNotResolvedError(name, fileName, line).raise();
        return resolved;
    }
}
