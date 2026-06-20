package me.kuwg.re.ast.nodes.trait;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.types.global.GlobalNode;
import me.kuwg.re.ast.types.load.TopLevelNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.trait.Trait;
import me.kuwg.re.compiler.trait.TraitFunction;
import me.kuwg.re.error.errors.trait.RTraitAlreadyDeclaredError;
import me.kuwg.re.type.TypeRef;

import java.util.Map;

public class TraitDeclarationNode extends ASTNode implements GlobalNode, TopLevelNode {
    private boolean loaded;

    private final String name;
    private final Map<String, TraitFunction> functions;

    public TraitDeclarationNode(final String fileName, final int line, final String name, final Map<String, TraitFunction> functions) {
        super(fileName, line);
        this.name = name;
        this.functions = functions;
    }

    @Override
    public ASTNode clone() {
        return this;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        for (final TraitFunction function : functions.values()) {
            function.params().forEach(p -> p.setType(replaceGenericType(p.type(), generics, cctx)));
            function.setReturnType(replaceGenericType(function.getReturnType(), generics, cctx));
        }
    }

    @Override
    public void compile(final CompilationContext cctx) {
        cctx.declare("; Trait declaration: " + name);
        if (!loaded) load(cctx);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Trait Declaration").append(NEWLINE)
                .append(indent).append(TAB).append("Name: ").append(name).append(NEWLINE)
                .append(indent).append(TAB).append("Functions:").append(NEWLINE);
        functions.values().forEach(f -> f.write(sb, indent + TAB + TAB));
    }

    @Override
    public void load(final CompilationContext cctx) {
        if (loaded) return;

        if (cctx.isTraitDeclared(name)) {
            new RTraitAlreadyDeclaredError(name, line, fileName).raise();
            return;
        }

        cctx.addTrait(
                name,
                new Trait(name, functions)
        );

        loaded = true;
    }
}
