package me.kuwg.re.compiler.struct;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.function.declaration.BuiltinFunctionDeclarationNode;
import me.kuwg.re.ast.nodes.function.declaration.FunctionDeclarationNode;
import me.kuwg.re.ast.nodes.function.declaration.FunctionParameter;
import me.kuwg.re.ast.nodes.struct.StructImplNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.compiler.generic.TypeParameter;
import me.kuwg.re.compiler.variable.RStructField;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.NoneBuiltinType;
import me.kuwg.re.type.ptr.PointerType;
import me.kuwg.re.type.struct.GenStructType;
import me.kuwg.re.type.struct.StructType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static me.kuwg.re.ast.ASTNode.replaceGenericType;

public final class RGenStruct extends RDefaultStruct {
    private final Map<List<TypeRef>, RStruct> cache = new HashMap<>();
    private final List<ImplTemplate> impls = new ArrayList<>();

    public RGenStruct(final String fileName, final List<String> inherited, TypeRef type, List<RStructField> fields) {
        super(fileName, false, inherited, type, fields);
    }

    @Override
    public GenStructType type() {
        return (GenStructType) super.type();
    }

    public RStruct instantiate(List<TypeRef> rawTypes, CompilationContext cctx) {
        if (rawTypes.size() != type().genericTypes().size()) {
            throw new RuntimeException(
                    "Expected " + type().genericTypes().size() + " generic arguments, got " + rawTypes.size()
            );
        }

        List<TypeRef> types = rawTypes.stream().map(t -> ASTNode.evalType(t, cctx, fileName, -1)).collect(Collectors.toList());

        if (cache.containsKey(types)) {
            return cache.get(types);
        }

        validateGenericConstraints(cctx, types);

        Map<String, TypeRef> mapping = new HashMap<>();
        for (int i = 0; i < type().genericTypes().size(); i++) {
            mapping.put(type().genericTypes().get(i).name(), types.get(i));
        }

        List<RStructField> newFields = new ArrayList<>();
        for (RStructField field : fields) {
            TypeRef replaced = replaceGenericType(field.type(), mapping, cctx);
            newFields.add(new RStructField(field.name(), replaced));
        }

        String mangledName = mangleName(types);

        TypeRef newType = new StructType(mangledName, newFields.stream().map(RStructField::type).toList());

        RStruct specialized = new RStruct(fileName, false, inherited, newType, newFields);

        cache.put(List.copyOf(types), specialized);
        cctx.addStruct(false, mangledName, inherited, newType, newFields);

        declareStructIfNeeded(cctx, specialized);
        String ns = cctx.popNamespace();
        applyImpls(specialized, mapping, cctx);
        if (ns != null) cctx.pushNamespace(ns);

        return specialized;
    }

    private void declareStructIfNeeded(CompilationContext cctx, RStruct struct) {
        String name = struct.type().getLLVMName();

        if (cctx.isStructDeclared(name)) return;

        cctx.markStructDeclared(name);

        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" = type { ");

        List<String> fieldTypes = struct.fields().stream().map(f -> ASTNode.evalType(f.type(), cctx, fileName, -1).getLLVMName()).toList();

        sb.append(String.join(", ", fieldTypes));
        sb.append(" }");

        cctx.addIR(sb.toString());
    }

    private String mangleName(List<TypeRef> types) {
        StringBuilder sb = new StringBuilder();
        sb.append(type().getName());
        sb.append("$");

        for (TypeRef t : types) {
            sb.append(encodeType(t));
        }

        return sb.toString();
    }

    public static String encodeType(TypeRef type) {
        String name = sanitize(type.getName());

        String base = name.length() + name;

        if (type instanceof GenStructType gen) {
            StringBuilder sb = new StringBuilder();
            sb.append("G");
            sb.append(base);

            for (TypeRef inner : gen.fieldTypes()) {
                sb.append(encodeType(inner));
            }

            return sb.toString();
        }

        return base;
    }

    private void applyImpls(RStruct struct, Map<String, TypeRef> mapping, CompilationContext cctx) {
        for (ImplTemplate impl : impls) {
            Map<String, TypeRef> combined = new HashMap<>(mapping);
            for (TypeParameter gen : impl.generics) {
                if (!combined.containsKey(gen.name())) {
                    throw new RuntimeException("Unresolved impl generic: " + gen.name());
                }
            }

            for (RConstructor ctor : impl.constructors) {
                ctor = ctor.clone();

                List<FunctionParameter> substitutedParams = substituteParams(ctor.parameters(), combined, cctx);

                String mangledName = StructImplNode.generateName(struct.type().getName(), ctor.llvmName());

                List<FunctionParameter> withSelf = new ArrayList<>();
                withSelf.add(new FunctionParameter("self", false, new PointerType(struct.type())));
                withSelf.addAll(substitutedParams);

                FunctionDeclarationNode fn = new FunctionDeclarationNode(fileName, ctor.block().getNodes().get(0).getLine(), false, mangledName, withSelf, NoneBuiltinType.INSTANCE, ctor.block().clone());

                fn.replaceGenerics(combined, cctx);
                fn.compile(cctx);

                RFunction compiled = cctx.getFunction(mangledName, extractTypes(withSelf));
                struct.constructors().add(compiled);
            }

            for (ASTNode fnNode : impl.functions) {
                fnNode = fnNode.clone();

                RFunction compiled;

                if (fnNode instanceof FunctionDeclarationNode dec) {

                    List<FunctionParameter> params = substituteParams(dec.getParameters(), combined, cctx);

                    List<FunctionParameter> withSelf = new ArrayList<>();
                    withSelf.add(new FunctionParameter("self", false, new PointerType(struct.type())));
                    withSelf.addAll(params);

                    TypeRef original = dec.getReturnType();
                    TypeRef returnType = replaceGenericType(original, combined, cctx);

                    String mangledName = StructImplNode.generateName(struct.type().getName(), dec.getName());

                    FunctionDeclarationNode renamed = new FunctionDeclarationNode(fileName, dec.getLine(), false, mangledName, withSelf, returnType, dec.getBlock().clone());

                    renamed.replaceGenerics(combined, cctx);
                    renamed.compile(cctx);

                    compiled = cctx.getFunction(mangledName, extractTypes(withSelf));

                } else if (fnNode instanceof BuiltinFunctionDeclarationNode blt) {

                    List<FunctionParameter> params = substituteParams(blt.getParameters(), combined, cctx);

                    List<FunctionParameter> withSelf = new ArrayList<>();
                    withSelf.add(new FunctionParameter("self", false, new PointerType(struct.type())));
                    withSelf.addAll(params);

                    TypeRef original = blt.getReturnType();
                    TypeRef returnType = replaceGenericType(original, combined, cctx);

                    String mangledName = StructImplNode.generateName(struct.type().getName(), blt.getName());

                    BuiltinFunctionDeclarationNode renamed = new BuiltinFunctionDeclarationNode(fileName, blt.getLine(), true, mangledName, withSelf, returnType, blt.getLlvmBody());

                    renamed.replaceGenerics(combined, cctx);
                    renamed.compile(cctx);

                    compiled = cctx.getFunction(mangledName, extractTypes(withSelf));
                } else {
                    throw new RuntimeException("Invalid function node in impl");
                }

                struct.functions().add(compiled);
            }
        }
    }

    private List<FunctionParameter> substituteParams(List<FunctionParameter> params, Map<String, TypeRef> mapping, final CompilationContext cctx) {
        List<FunctionParameter> result = new ArrayList<>();

        for (FunctionParameter p : params) {
            result.add(new FunctionParameter(p.name(), p.mutable(), replaceGenericType(p.type(), mapping, cctx)));
        }

        return result;
    }

    private List<TypeRef> extractTypes(List<FunctionParameter> params) {
        return params.stream().map(FunctionParameter::type).toList();
    }

    public static String sanitize(String s) {
        return s.replaceAll("[^A-Za-z0-9_]", "_");
    }

    public void addImplTemplate(List<TypeParameter> generics, List<RConstructor> constructors, List<ASTNode> functions) {
        impls.add(new ImplTemplate(generics, constructors, functions));
    }

    private void validateGenericConstraints(CompilationContext cctx, List<TypeRef> actualTypes) {
        List<TypeParameter> params = type().genericTypes();

        for (int i = 0; i < params.size(); i++) {
            TypeParameter tp = params.get(i);
            if (tp.inherited() == null) continue;

            TypeRef actual = actualTypes.get(i);

            if (!satisfiesConstraint(cctx, actual.getName(), tp.inherited())) {
                throw new RuntimeException(
                        "Type '" + actual.getName() + "' does not satisfy constraint '" + tp.inherited() +
                                "' for generic '" + tp.name() + "'"
                );
            }
        }
    }

    private boolean satisfiesConstraint(CompilationContext cctx, String actual, String required) {
        if (actual.equals(required)) {
            return true;
        }

        RDefaultStruct struct = cctx.getStruct(actual);
        if (struct == null) {
            return false;
        }

        return inherits(cctx, struct, required);
    }

    private boolean inherits(CompilationContext cctx, RDefaultStruct struct, String required) {
        for (String parent : struct.inherited()) {
            if (parent.equals(required)) {
                return true;
            }

            RDefaultStruct inheritedStruct = cctx.getStruct(parent);
            if (inheritedStruct != null && inherits(cctx, inheritedStruct, required)) {
                return true;
            }
        }

        return false;
    }

    private static class ImplTemplate {
        final List<TypeParameter> generics;
        final List<RConstructor> constructors;
        final List<ASTNode> functions;

        ImplTemplate(List<TypeParameter> generics, List<RConstructor> constructors, List<ASTNode> functions) {
            this.generics = generics;
            this.constructors = constructors;
            this.functions = functions;
        }
    }
}
