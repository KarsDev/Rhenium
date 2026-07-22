package me.kuwg.re.ast.nodes.union;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.types.load.TopLevelNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.error.errors.union.RUnionAlreadyExistsError;
import me.kuwg.re.type.TypeRef;
import me.kuwg.re.type.struct.StructType;
import me.kuwg.re.type.union.UnionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UnionDeclarationNode extends ASTNode implements TopLevelNode {
    private boolean loaded;
    private UnionType type;

    public UnionDeclarationNode(final String fileName, final int line, final UnionType type) {
        super(fileName, line);
        this.type = type;
    }

    @Override
    public ASTNode clone() {
        return this;
    }

    @Override
    public void load(final CompilationContext cctx) {
        if (loaded) return;

        type = evalType(type, cctx, fileName, line);

        if (cctx.getUnion(type.getName()) != null) {
            new RUnionAlreadyExistsError(type.getName(), fileName, line).raise();
            return;
        }

        cctx.addUnion(type.getName(), type);
        loaded = true;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        final List<TypeRef> resolved = new ArrayList<>(type.variants().size());

        for (final TypeRef variant : type.variants()) {
            final TypeRef replaced = replaceGenericType(variant, generics, cctx);
            StructType t = (StructType) cctx.resolveConcrete(replaced, line);
            resolved.add(t);
        }

        this.type = new UnionType(type.getName(), resolved);
    }

    @Override
    public void compile(final CompilationContext cctx) {
        if (!loaded) load(cctx);
        cctx.addIR(type.getLLVMName() + " = type { i32, [" + type.payloadSize() + " x i8] }");
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Union Declaration").append(NEWLINE)
                .append(indent).append(TAB).append("Name: ").append(type.getName()).append(NEWLINE)
                .append(indent).append(TAB).append("Variants:").append(NEWLINE);

        for (final TypeRef variant : type.variants()) {
            sb.append(indent).append(TAB).append(TAB)
                    .append("- ").append(variant.getName()).append(NEWLINE);
        }
    }
}