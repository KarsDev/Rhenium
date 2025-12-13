package me.kuwg.re.cli;

import java.util.function.BiConsumer;

record Option(
        String name,
        boolean takesValue,
        BiConsumer<Arguments.Builder, String> apply
) {}
