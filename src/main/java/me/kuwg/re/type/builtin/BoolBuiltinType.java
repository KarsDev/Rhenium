package me.kuwg.re.type.builtin;

import me.kuwg.re.type.TypeRef;

public class BoolBuiltinType extends BuiltinType {
    static final BuiltinType INSTANCE = new BoolBuiltinType();

    private BoolBuiltinType(){
    }

    @Override
    public boolean isCompatibleWith(final TypeRef other) {
        return other instanceof BoolBuiltinType;
    }

    @Override
    public long getSize() {
        return 1;
    }

    @Override
    public String getName() {
        return "bool";
    }

    @Override
    public String getLLVMName() {
        return "i1";
    }
}
