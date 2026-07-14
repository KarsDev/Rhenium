package me.kuwg.re.ast.types.value;

import me.kuwg.re.type.TypeRef;

public abstract class PointerValueNode extends ValueNode {
    protected PointerValueNode(final String fileName, final int line) {
        super(fileName, line);
    }
    protected PointerValueNode(final String fileName, final int line, final TypeRef type) {
        super(fileName, line, type);
    }
}
