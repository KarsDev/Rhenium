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
import me.kuwg.re.type.union.UnionType;
import me.kuwg.re.writer.Writeable;

import java.util.*;

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

    protected TypeRef replaceGenericType(TypeRef current, Map<String, TypeRef> generics, final CompilationContext cctx) {
        return replaceGenericType(current, generics, cctx, line);
    }

    public static TypeRef replaceGenericType(TypeRef current, Map<String, TypeRef> generics, final CompilationContext cctx, int line) {
        if (current instanceof GenericType genericType) {
            return generics.get(genericType.name());
        }

        if (current instanceof AppliedGenStructType a) {
            a.args().replaceAll(f -> replaceGenericType(f, generics, cctx, line));
            return ((RGenStruct) cctx.getStruct(a.base().getName())).instantiate(a.args(), cctx, line).type();
        }

        if (current instanceof PointerType p) {
            TypeRef newInner = replaceGenericType(p.getInner(), generics, cctx, line);
            if (newInner != p.getInner()) {
                return new PointerType(newInner);
            }
            return p;
        }

        if (current instanceof ArrayType a) {
            TypeRef newInner = replaceGenericType(a.getInner(), generics, cctx, line);
            if (newInner != a.getInner()) {
                return new ArrayType(a.size(), newInner);
            }
            return a;
        }

        if (current instanceof StructType s) {
            List<TypeRef> newFields = new ArrayList<>();

            for (TypeRef t : s.getFieldTypes()) {
                newFields.add(replaceGenericType(t, generics, cctx, line));
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
    public static <T extends TypeRef> T evalType(T type, CompilationContext cctx, final String fileName, final int line) {
        TypeRef resolved = evalType(type, cctx, fileName, line, new HashSet<>());

        if (resolved instanceof AppliedGenStructType) {
            throw new RInternalError();
        }

        return (T) resolved;
    }

    private static TypeRef evalType(TypeRef type, CompilationContext cctx, final String fileName, final int line, Set<String> expanding) {
        if (type instanceof AppliedGenStructType ags) {
            String key = ags.base().getName() + "<" + ags.args() + ">";

            if (!expanding.add(key)) {
                return type;
            }

            try {
                String structName = ags.base().getName();
                RGenStruct struct = (RGenStruct) cctx.getStruct(structName);
                TypeRef instantiated = struct.instantiate(ags.args(), cctx, line).type();
                return evalType(instantiated, cctx, fileName, line, expanding);
            } finally {
                expanding.remove(key);
            }
        }

        if (type instanceof PointerType ptr) {
            TypeRef inner = evalType(ptr.getInner(), cctx, fileName, line, expanding);
            return inner == ptr.getInner() ? ptr : new PointerType(inner);
        }

        if (type instanceof ArrayType arr) {
            TypeRef inner = evalType(arr.getInner(), cctx, fileName, line, expanding);
            return inner == arr.getInner() ? arr : new ArrayType(arr.size(), inner);
        }

        if (type instanceof UnionType union) {
            List<TypeRef> variants = new ArrayList<>(union.variants().size());
            for (TypeRef v : union.variants()) {
                variants.add(evalType(v, cctx, fileName, line, expanding));
            }
            return new UnionType(union.getName(), variants);
        }

        if (type instanceof StructType st) {
            if (!expanding.add("struct:" + st.getName())) {
                return st;
            }

            try {
                List<TypeRef> fields = new ArrayList<>();
                for (TypeRef field : st.getFieldTypes()) {
                    fields.add(evalType(field, cctx, fileName, line, expanding));
                }
                return new StructType(st.getName(), fields);
            } finally {
                expanding.remove("struct:" + st.getName());
            }
        }

        if (type instanceof TraitType) {
            return new RInheritanceError("Trait is not usable as a parameter", fileName, line).raise();
        }

        return type;
    }
}
