package me.kuwg.re.compiler.variable;

import me.kuwg.re.type.TypeRef;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

public record RVariable(@NotNull String name, boolean mutable, boolean addressBacked, @NotNull TypeRef type, String addrReg, @NotNull String valueReg) {
    private static final AtomicInteger COUNTER = new AtomicInteger();

    public static String makeUnique(String base) {
        int hash = Math.abs(base.hashCode() % 10_000);
        return "%s_%04d_%d".formatted(base, hash, COUNTER.getAndIncrement());
    }
}
