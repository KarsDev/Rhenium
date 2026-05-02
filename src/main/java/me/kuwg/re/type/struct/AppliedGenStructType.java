package me.kuwg.re.type.struct;

import me.kuwg.re.error.errors.RInternalError;
import me.kuwg.re.type.TypeRef;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
                        .map(TypeRef::getName)
                        .collect(Collectors.joining(", "))
                + ">";
    }

    @Override
    public String getLLVMName() {
        throw new RInternalError();
    }

    @Override
    public String getMangledName() {
        StringBuilder sb = new StringBuilder();
        sb.append(base.getName());
        sb.append("$");

        for (TypeRef t : args) {
            sb.append(encode(t));
        }

        return sb.toString();
    }

    private String encode(TypeRef t) {
        String name = sanitize(t.getName());
        return name.length() + name;
    }

    private String sanitize(String s) {
        return s.replaceAll("[^A-Za-z0-9_]", "_");
    }

    @Override
    public boolean equals(final TypeRef other) {
        if (!(other instanceof StructType st)) return false;
        if (!base.getName().equals(st.getName().split("\\$")[0])) return false;

        return st.fieldTypes().equals(args);
    }
}