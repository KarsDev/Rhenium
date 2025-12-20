package me.kuwg.re.compiler.function;

import me.kuwg.re.ast.nodes.function.FunctionParameter;
import me.kuwg.re.type.TypeRef;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class RFunction {
    public final String llvmName;
    public final String name;
    public final TypeRef returnType;
    public final List<FunctionParameter> parameters;

    protected RFunction(String llvmName, String name, TypeRef returnType, List<FunctionParameter> parameters) {
        this.llvmName = llvmName;
        this.name = name;
        this.returnType = returnType;
        this.parameters = parameters;
    }

    public String llvmName() {
        return llvmName;
    }

    public String name() {
        return name;
    }

    public TypeRef returnType() {
        return returnType;
    }

    public List<FunctionParameter> parameters() {
        return parameters;
    }

    private static final AtomicInteger COUNTER = new AtomicInteger();

    public static String makeUnique(String base) {
        int hash = Math.abs(base.hashCode() % 10_000);
        return "%s_%04d_%d".formatted(base, hash, COUNTER.getAndIncrement());
    }

    @Override
    @NotNull
    public String toString() {
        return "RFunction(" +
                "llvmName='" + llvmName + '\'' +
                ", name='" + name + '\'' +
                ", returnType=" + returnType +
                ", parameters=" + parameters +
                ')';
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof final RFunction rFunction)) return false;

        return Objects.equals(name, rFunction.name) && Objects.equals(llvmName, rFunction.llvmName) &&
                Objects.equals(returnType, rFunction.returnType) && Objects.equals(parameters, rFunction.parameters);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(llvmName);
        result = 31 * result + Objects.hashCode(name);
        result = 31 * result + Objects.hashCode(returnType);
        result = 31 * result + Objects.hashCode(parameters);
        return result;
    }
}