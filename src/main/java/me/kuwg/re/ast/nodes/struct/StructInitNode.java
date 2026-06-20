package me.kuwg.re.ast.nodes.struct;

import me.kuwg.re.ast.types.value.ValueNode;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.compiler.function.RFunction;
import me.kuwg.re.compiler.struct.RDefaultStruct;
import me.kuwg.re.compiler.struct.RGenStruct;
import me.kuwg.re.compiler.struct.StructCompiler;
import me.kuwg.re.compiler.trait.Trait;
import me.kuwg.re.compiler.variable.RParamValue;
import me.kuwg.re.error.errors.struct.RGenStructInitError;
import me.kuwg.re.error.errors.struct.RStructAccessError;
import me.kuwg.re.error.errors.struct.RStructUndefinedError;
import me.kuwg.re.error.errors.trait.RInheritanceError;
import me.kuwg.re.error.errors.value.RValueMustBeUsedError;
import me.kuwg.re.type.TypeRef;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class StructInitNode extends ValueNode {
    private final String name;
    private final List<RParamValue> values;

    public StructInitNode(final String fileName, final int line, final String name, final List<RParamValue> values) {
        super(fileName, line);
        this.name = name;
        this.values = values;
    }

    @Override
    public void replaceGenerics(final Map<String, TypeRef> generics, final CompilationContext cctx) {
        values.forEach(v -> v.value().replaceGenerics(generics, cctx));
    }

    @Override
    public String compileAndGet(final CompilationContext cctx) {
        cctx.emit("; Struct initialization");

        RDefaultStruct struct = cctx.getStruct(name);
        if (struct == null) {
            return new RStructUndefinedError(name, fileName, line).raise();
        }

        if (struct.builtin()) {
            return new RStructAccessError("This struct can't be initialized: " + name, fileName, line).raise();
        }

        if (struct instanceof RGenStruct) {
            return new RGenStructInitError("Add generic types for generic struct initialization", fileName, line).raise();
        }

        checkInheritance(struct, cctx);

        return StructCompiler.compile(fileName, line, cctx, struct, values, this);
    }

    @Override
    public void compile(final CompilationContext cctx) {
        new RValueMustBeUsedError("Struct Initialization", fileName, line).raise();
    }

    @Override
    public void write(final StringBuilder sb, final String indent) {
        sb.append(indent).append("Struct Init: ").append(NEWLINE);
        sb.append(indent).append(TAB).append("Name: ").append(name).append(NEWLINE);
        sb.append(indent).append(TAB).append("Values: ").append(NEWLINE);
        values.forEach(v -> v.value().write(sb, indent + TAB + TAB));
    }

    @Override
    public StructInitNode clone() {
        List<RParamValue> valuesCloned = new ArrayList<>();
        IntStream.range(0, values.size()).forEach(i -> valuesCloned.add(i, values.get(i).clone()));
        return new StructInitNode(fileName, line, name, valuesCloned);
    }

    private void checkInheritance(RDefaultStruct struct, CompilationContext cctx) {
        struct.inherited().forEach(i -> {
            Trait trait = cctx.getTrait(i);
            if (trait == null) {
                new RInheritanceError("Trait not found: " + i, fileName, line).raise();
                return;
            }
            checkInherited(trait, struct);
        });
    }

    private void checkInherited(final Trait trait, final RDefaultStruct struct) {
        trait.functions().forEach((traitFunctionName, traitFunc) -> {
            boolean found = struct.functions().stream().anyMatch(f -> {
                String functionName = extractFunctionName(f);

                if (!functionName.equals(traitFunctionName)) {
                    return false;
                }

                if (f.parameters.size() - 1 != traitFunc.params().size()) {
                    return false;
                }

                for (int i = 0; i < f.parameters.size() - 1; i++) {
                    if (!f.parameters.get(i).type().isCompatibleWith(traitFunc.params().get(i).type())) {
                        return false;
                    }
                }

                return f.returnType.isCompatibleWith(traitFunc.getReturnType());
            });

            if (!found) {
                new RInheritanceError("Struct '" + this.name + "' does not implement trait function '" + traitFunctionName + "' from trait '" + trait.name() + "'", fileName, line).raise();
            }
        });
    }

    private static @NotNull String extractFunctionName(final RFunction f) {
        String functionName;
        {
            String s = f.name;
            if (s.length() >= 2 &&
                    ((s.startsWith("\"") && s.endsWith("\"")) ||
                            (s.startsWith("'") && s.endsWith("'")))) {
            s = s.substring(1, s.length() - 1);
            }

            int dotIndex = s.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < s.length() - 1) {
                s = s.substring(dotIndex + 1);
            }
            functionName = s;
        }
        return functionName;
    }
}
