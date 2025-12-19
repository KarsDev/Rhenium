package me.kuwg.re.type.builtin;

import me.kuwg.re.type.TypeRef;

public enum BuiltinTypes {
    BOOL(BoolBuiltinType.INSTANCE),
    BYTE(ByteBuiltinType.INSTANCE),
    SHORT(ShortBuiltinType.INSTANCE),
    INT(IntBuiltinType.INSTANCE),
    LONG(LongBuiltinType.INSTANCE),
    FLOAT(FloatBuiltinType.INSTANCE),
    DOUBLE(DoubleBuiltinType.INSTANCE),
    STR(StrBuiltinType.INSTANCE),
    CHAR(CharBuiltinType.INSTANCE),
    NONE(NoneBuiltinType.INSTANCE),
    ANYPTR(AnyPointerType.INSTANCE),

    ;

    private final BuiltinType type;

    BuiltinTypes(final BuiltinType type) {
        this.type = type;
    }

    public TypeRef getType() {
        return type;
    }

    public static TypeRef getByName(String name) {
        for (BuiltinTypes builtinType : values()) {
            if (!builtinType.getType().getName().equals(name)) continue;
            return builtinType.getType();
        }
        return null;
    }
}
