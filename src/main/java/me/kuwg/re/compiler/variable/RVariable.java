package me.kuwg.re.compiler.variable;

import me.kuwg.re.type.TypeRef;

import java.util.concurrent.atomic.AtomicInteger;

public record RVariable(String name, boolean mutable, TypeRef type, String valueReg) {
    private static final AtomicInteger COUNTER = new AtomicInteger();

    public static String makeUnique(String base) {
        int hash = Math.abs(base.hashCode() % 10_000);
        return "%s_%04d_%d".formatted(base, hash, COUNTER.getAndIncrement());
    }
}
