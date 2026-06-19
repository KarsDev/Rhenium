package me.kuwg.re.ast.nodes.enumeration;

import me.kuwg.re.ast.ASTNode;
import me.kuwg.re.ast.nodes.constants.ConstantNode;
import me.kuwg.re.ast.types.global.GlobalNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.enums.REnum;
import me.kuwg.re.compiler.enums.REnumField;
import me.kuwg.re.error.errors.enums.*;
import me.kuwg.re.type.TypeRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EnumDeclarationNode extends ASTNode implements GlobalNode {
    private final String name;
    private final Map<String, ConstantNode> fields;

    public EnumDeclarationNode(final int line, final String name, final Map<String, ConstantNode> fields) {
        super(line);
        this.name = name;
        this.fields = fields;
    }

    @Override
    public ASTNode clone() {
        return new EnumDeclarationNode(line, name, fields);
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
    }

    @Override
    public void compile(final CompilationContext cctx) {
        if (cctx.isEnumDeclared(name)) {
            new REnumIsAlreadyDeclaredError(name, line).raise();
            return;
        }
        List<REnumField> enumFields = new ArrayList<>();

        fields.forEach((name, value) -> {
            String vr = value.compileToConstant(cctx);
            enumFields.add(new REnumField(name, value.getType(), vr));
        });

        TypeRef type = null;
        for (final REnumField enumField : enumFields) {
            if (type == null) {
                type = enumField.type();
                continue;
            }
            if (!type.isCompatibleWith(enumField.type())) {
                new REnumFieldTypesError("Enum fields cannot have multiple types", line).raise();
                return;
            }
        }

        REnum rEnum = new REnum(REnum.mangleName(name, enumFields), enumFields);

        cctx.addEnum(name, rEnum);
        cctx.declare("; enum " + name + " = " + fields);
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Enum Declaration").append(NEWLINE)
                .append(indent).append(TAB).append("Name: ").append(name).append(NEWLINE)
                .append(indent).append(TAB).append("Fields: ").append(fields);
    }
}
