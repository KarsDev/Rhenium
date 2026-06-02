package me.kuwg.re.type.struct;

import me.kuwg.re.error.errors.RInternalError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.generic.GenericType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static me.kuwg.re.compiler.struct.RGenStruct.encodeType;

public record AppliedGenStructType(GenStructType base, List<TypeRef> args) implements TypeRef {
    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public boolean isCompatibleWith(final TypeRef other) {
        if (!(other instanceof AppliedGenStructType o)) return false;

        return Objects.equals(base.getName(), o.base.getName())
                && Objects.equals(args, o.args);
    }

    @Override
    public long getSize() {
        throw new RInternalError();
    }

    @Override
    public String getName() {
        return base.getName() + "<" +
                args.stream()
                        .map(t -> t instanceof GenericType g ? g.name() : t.getName())
                        .collect(Collectors.joining(", "))
                + ">";
    }

    @Override
    public String getLLVMName() {
        throw new RuntimeException();
    }

    @Override
    public @NotNull String toString() {
        return "struct " + base.name();
    }

    @Override
    public String getMangledName() {
        StringBuilder sb = new StringBuilder();
        sb.append(base.getName());
        sb.append("$");

        for (TypeRef t : args) {
            sb.append(encodeType(t));
        }

        return sb.toString();
    }

    @Override
    public boolean equals(final TypeRef other) {
        return equals((Object) other);
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof StructType st)) return false;
        if (!base.getName().equals(st.getName().split("\\$")[0])) return false;

        return st.fieldTypes().equals(args);
    }
}