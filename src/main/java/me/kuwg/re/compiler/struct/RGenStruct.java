package me.kuwg.re.compiler.struct;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.function.declaration.BuiltinFunctionDeclarationNode;
import me.kuwg.re.ast.nodes.function.declaration.FunctionDeclarationNode;
import me.kuwg.re.ast.nodes.function.declaration.FunctionParameter;
import me.kuwg.re.ast.nodes.struct.StructImplNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.compiler.variable.RStructField;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.builtin.NoneBuiltinType;
import me.kuwg.re.type.generic.GenericType;
import me.kuwg.re.type.ptr.PointerType;
import me.kuwg.re.type.struct.GenStructType;
import me.kuwg.re.type.struct.StructType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RGenStruct extends RDefaultStruct {
    private final Map<List<TypeRef>, RStruct> cache = new HashMap<>();
    private final List<ImplTemplate> impls = new ArrayList<>();

    public RGenStruct(TypeRef type, List<RStructField> fields) {
        super(false, type, fields);
    }

    @Override
    public GenStructType type() {
        return (GenStructType) super.type();
    }

    public RStruct instantiate(List<TypeRef> types, CompilationContext cctx) {
        if (cache.containsKey(types)) {
            return cache.get(types);
        }

        Map<String, TypeRef> mapping = new HashMap<>();

        for (int i = 0; i < type().genericTypes().size(); i++) {
            mapping.put(type().genericTypes().get(i), types.get(i));
        }

        List<RStructField> newFields = new ArrayList<>();

        for (RStructField field : fields) {
            TypeRef replaced = replace(field.type(), mapping);
            newFields.add(new RStructField(field.name(), replaced));
        }

        String mangledName = mangleName(types);

        TypeRef newType = new StructType(mangledName, newFields.stream().map(RStructField::type).toList());

        RStruct specialized = new RStruct(false, newType, newFields);

        cache.put(types, specialized);
        cctx.addStruct(false, mangledName, newType, newFields);

        declareStructIfNeeded(cctx, specialized);

        applyImpls(specialized, mapping, cctx);

        return specialized;
    }

    private void declareStructIfNeeded(CompilationContext cctx, RStruct struct) {
        String name = struct.type().getLLVMName();

        if (cctx.isStructDeclared(name)) return;

        cctx.markStructDeclared(name);

        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" = type { ");

        List<String> fieldTypes = struct.fields().stream().map(f -> f.type().getLLVMName()).toList();

        sb.append(String.join(", ", fieldTypes));
        sb.append(" }");

        cctx.addIR(sb.toString());
    }

    private TypeRef replace(TypeRef original, Map<String, TypeRef> mapping) {
        if (original instanceof GenericType gen) {
            TypeRef resolved = mapping.get(gen.name());

            if (resolved == null) {
                throw new RuntimeException("Unmapped generic type: " + gen.name());
            }

            return resolved;
        }

        if (original instanceof GenStructType genType) {
            List<TypeRef> replacedFields = new ArrayList<>();

            for (TypeRef fieldType : genType.fieldTypes()) {
                replacedFields.add(replace(fieldType, mapping));
            }

            return new GenStructType(genType.genericTypes(), genType.getName(), replacedFields);
        }

        return original;
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

    private String encodeType(TypeRef type) {
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
            for (String gen : impl.generics) {
                if (!combined.containsKey(gen)) {
                    throw new RuntimeException("Unresolved impl generic: " + gen);
                }
            }

            for (RConstructor ctor : impl.constructors) {

                List<FunctionParameter> substitutedParams =
                        substituteParams(ctor.parameters(), combined);

                String mangledName = StructImplNode.generateName(
                        struct.type().getName(),
                        ctor.llvmName()
                );

                List<FunctionParameter> withSelf = new ArrayList<>();
                withSelf.add(new FunctionParameter(
                        "self",
                        false,
                        new PointerType(struct.type())
                ));
                withSelf.addAll(substitutedParams);

                FunctionDeclarationNode fn = new FunctionDeclarationNode(
                        ctor.block().getNodes().get(0).getLine(),
                        false,
                        mangledName,
                        withSelf,
                        NoneBuiltinType.INSTANCE,
                        ctor.block()
                );

                fn.compile(cctx);

                RFunction compiled = cctx.getFunction(mangledName, extractTypes(withSelf));
                struct.constructors().add(compiled);
            }

            for (ASTNode fnNode : impl.functions) {

                RFunction compiled;

                if (fnNode instanceof FunctionDeclarationNode dec) {

                    List<FunctionParameter> params =
                            substituteParams(dec.getParameters(), combined);

                    List<FunctionParameter> withSelf = new ArrayList<>();
                    withSelf.add(new FunctionParameter(
                            "self",
                            false,
                            new PointerType(struct.type())
                    ));
                    withSelf.addAll(params);

                    TypeRef returnType = replace(dec.getReturnType(), combined);

                    String mangledName = StructImplNode.generateName(
                            struct.type().getName(),
                            dec.getName()
                    );

                    FunctionDeclarationNode renamed = new FunctionDeclarationNode(
                            dec.getLine(),
                            false,
                            mangledName,
                            withSelf,
                            returnType,
                            dec.getBlock()
                    );

                    renamed.compile(cctx);

                    compiled = cctx.getFunction(mangledName, extractTypes(withSelf));

                } else if (fnNode instanceof BuiltinFunctionDeclarationNode blt) {

                    List<FunctionParameter> params =
                            substituteParams(blt.getParameters(), combined);

                    List<FunctionParameter> withSelf = new ArrayList<>();
                    withSelf.add(new FunctionParameter(
                            "self",
                            false,
                            new PointerType(struct.type())
                    ));
                    withSelf.addAll(params);

                    TypeRef returnType = replace(blt.getReturnType(), combined);

                    String mangledName = StructImplNode.generateName(
                            struct.type().getName(),
                            blt.getName()
                    );

                    BuiltinFunctionDeclarationNode renamed =
                            new BuiltinFunctionDeclarationNode(
                                    blt.getLine(),
                                    true,
                                    mangledName,
                                    withSelf,
                                    returnType,
                                    blt.getLlvmBody()
                            );

                    renamed.compile(cctx);

                    compiled = cctx.getFunction(mangledName, extractTypes(withSelf));

                } else {
                    throw new RuntimeException("Invalid function node in impl");
                }

                struct.functions().add(compiled);
            }
        }
    }

    private List<FunctionParameter> substituteParams(List<FunctionParameter> params, Map<String, TypeRef> mapping) {
        List<FunctionParameter> result = new ArrayList<>();

        for (FunctionParameter p : params) {
            result.add(new FunctionParameter(p.name(), p.mutable(), replace(p.type(), mapping)));
        }

        return result;
    }

    private List<TypeRef> extractTypes(List<FunctionParameter> params) {
        return params.stream().map(FunctionParameter::type).toList();
    }

    private String sanitize(String s) {
        return s.replaceAll("[^A-Za-z0-9_]", "_");
    }

    public void addImplTemplate(List<String> generics, List<RConstructor> constructors, List<ASTNode> functions) {
        impls.add(new ImplTemplate(generics, constructors, functions));
    }

    private static class ImplTemplate {
        final List<String> generics;
        final List<RConstructor> constructors;
        final List<ASTNode> functions;

        ImplTemplate(List<String> generics, List<RConstructor> constructors, List<ASTNode> functions) {
            this.generics = generics;
            this.constructors = constructors;
            this.functions = functions;
        }
    }
}
