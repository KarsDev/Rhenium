package me.kuwg.re.compiler.enums;

import java.util.List;

public record REnum(String name, List<REnumField> fields) {
    public REnumField getField(String name) {
        return fields.stream().filter(f -> f.name().equals(name)).findFirst().orElse(null);
    }

    public static String mangleName(String name, List<REnumField> fields) {
        StringBuilder sb = new StringBuilder("enum_" + name);
        fields.forEach(f -> sb.append("_").append(f.type().getMangledName()));
        return sb.toString();
    }
}
