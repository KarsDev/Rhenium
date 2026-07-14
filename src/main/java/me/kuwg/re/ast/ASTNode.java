package me.kuwg.re.ast;

import me.kuwg.re.compiler.Compilable;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.struct.RGenStruct;
import me.kuwg.re.error.errors.RInternalError;
import me.kuwg.re.error.errors.trait.RInheritanceError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.generic.GenericType;
import me.kuwg.re.type.iterable.arr.ArrayType;
import me.kuwg.re.type.ptr.PointerType;
import me.kuwg.re.type.struct.AppliedGenStructType;
import me.kuwg.re.type.struct.StructType;
import me.kuwg.re.type.trait.TraitType;
import me.kuwg.re.writer.Writeable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class ASTNode implements Compilable, Writeable, Cloneable {
    protected final String fileName;
    protected final int line;

    protected ASTNode(final String fileName, int line) {
        this.fileName = fileName;
        this.line = line;
    }

    public int getLine() {
        return line;
    }

    public static TypeRef replaceGenericType(TypeRef current, Map<String, TypeRef> generics, final CompilationContext cctx) {
        if (current instanceof GenericType genericType) {
            return generics.get(genericType.name());
        }

        if (current instanceof AppliedGenStructType a) {
            a.args().replaceAll(f -> replaceGenericType(f, generics, cctx));
            return ((RGenStruct) cctx.getStruct(a.base().name())).instantiate(a.args(), cctx).type();
        }

        if (current instanceof PointerType p) {
            TypeRef newInner = replaceGenericType(p.inner(), generics, cctx);
            if (newInner != p.inner()) {
                return new PointerType(newInner);
            }
            return p;
        }

        if (current instanceof ArrayType a) {
            TypeRef newInner = replaceGenericType(a.inner(), generics, cctx);
            if (newInner != a.inner()) {
                return new ArrayType(a.size(), newInner);
            }
            return a;
        }

        if (current instanceof StructType s) {
            List<TypeRef> newFields = new ArrayList<>();

            for (TypeRef t : s.fieldTypes()) {
                newFields.add(replaceGenericType(t, generics, cctx));
            }

            return new StructType(s.getName(), newFields);
        }

        return current;
    }

    @Override
    public abstract ASTNode clone();

    public String getFileName() {
        return fileName;
    }

    @SuppressWarnings("unchecked")
    public static <T extends TypeRef>  T evalType(T type, CompilationContext cctx, final String fileName, final int line) {
        if (type instanceof AppliedGenStructType ags) {
            String structName = ags.base().name();
            RGenStruct struct = (RGenStruct) cctx.getStruct(structName);
            type = (T) struct.instantiate(ags.args(), cctx).type();
        } else if (type instanceof PointerType ptr) {
            type = (T) new PointerType(evalType(ptr.inner(), cctx, fileName, line));
        } else if (type instanceof ArrayType arr) {
            type = (T) new ArrayType(arr.size(), evalType(arr.inner(), cctx, fileName, line));
        } else if (type instanceof TraitType) {
            return new RInheritanceError("Trait is not usable as a parameter", fileName, line).raise();
        }

        if (type instanceof AppliedGenStructType) throw new RInternalError();

        return type;
    }
}
