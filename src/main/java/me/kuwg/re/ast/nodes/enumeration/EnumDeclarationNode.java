package me.kuwg.re.ast.nodes.enumeration;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.types.global.GlobalNode;
import me.kuwg.re.ast.types.load.TopLevelNode;
import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.enums.REnum;
import me.kuwg.re.compiler.enums.REnumField;
import me.kuwg.re.error.errors.constant.RNotConstantError;
import me.kuwg.re.error.errors.enums.REnumFieldTypesError;
import me.kuwg.re.error.errors.enums.REnumIsAlreadyDeclaredError;
import me.kuwg.re.type.TypeRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EnumDeclarationNode extends ASTNode implements GlobalNode, TopLevelNode {
    private final String name;
    private final Map<String, ValueNode> fields;
    private boolean loaded;

    public EnumDeclarationNode(final String fileName, final int line, final String name, final Map<String, ValueNode> fields) {
        super(fileName, line);
        this.name = name;
        this.fields = fields;
    }

    @Override
    public ASTNode clone() {
        var v = new EnumDeclarationNode(fileName, line, name, fields);
        v.loaded = loaded;
        return v;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
    }

    @Override
    public void compile(final CompilationContext cctx) {
        if (!loaded) load(cctx);
        cctx.declare("; Enum Declaration " + name);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Enum Declaration").append(NEWLINE).append(indent).append(TAB).append("Name: ").append(name).append(NEWLINE).append(indent).append(TAB).append("Fields: ").append(fields);
    }

    @Override
    public void load(final CompilationContext cctx) {
        if (loaded) return;

        if (cctx.isEnumDeclared(name)) {
            new REnumIsAlreadyDeclaredError(name, fileName, line).raise();
            return;
        }

        List<REnumField> enumFields = new ArrayList<>();

        fields.forEach((fieldName, value) -> {
            if (!value.isConstant(cctx)) {
                new RNotConstantError("Expected constant value for enum declaration", fileName, line).raise();
                return;
            }

            String vr = value.compileToConstant(cctx);
            enumFields.add(new REnumField(fieldName, value.getType(), vr));
        });

        TypeRef type = null;

        for (REnumField enumField : enumFields) {
            if (type == null) {
                type = enumField.type();
                continue;
            }

            if (!type.isCompatibleWith(enumField.type())) {
                new REnumFieldTypesError("Enum fields cannot have multiple types", fileName, line).raise();
                return;
            }
        }

        cctx.addEnum(name, new REnum(REnum.mangleName(name, enumFields), enumFields));

        loaded = true;
    }
}
