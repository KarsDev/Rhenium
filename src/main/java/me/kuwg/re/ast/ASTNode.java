package me.kuwg.re.ast;

import me.kuwg.re.compiler.Compilable;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.generic.GenericType;
import me.kuwg.re.type.generic.GenericTypeEvaluator;
import me.kuwg.re.type.iterable.arr.ArrayType;
import me.kuwg.re.type.ptr.PointerType;
import me.kuwg.re.type.struct.StructType;
import me.kuwg.re.writer.Writeable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class ASTNode implements Compilable, Writeable, GenericTypeEvaluator, Cloneable {
    protected final int line;

    protected ASTNode(int line) {
        this.line = line;
    }

    public int getLine() {
        return line;
    }

    public static TypeRef replaceGenericType(TypeRef current, Map<String, TypeRef> generics) {
        if (current instanceof GenericType genericType) {
            return generics.get(genericType.name());
        }

        if (current instanceof PointerType p) {
            TypeRef newInner = replaceGenericType(p.inner(), generics);
            if (newInner != p.inner()) {
                return new PointerType(newInner);
            }
            return p;
        }

        if (current instanceof ArrayType a) {
            TypeRef newInner = replaceGenericType(a.inner(), generics);
            if (newInner != a.inner()) {
                return new ArrayType(a.size(), newInner);
            }
            return a;
        }

        if (current instanceof StructType s) {
            List<TypeRef> newFields = new ArrayList<>();

            for (TypeRef t : s.fieldTypes()) {
                newFields.add(replaceGenericType(t, generics));
            }

            return new StructType(s.getName(), newFields);
        }

        return current;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
