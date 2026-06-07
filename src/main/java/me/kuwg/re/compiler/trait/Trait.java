package me.kuwg.re.compiler.trait;

import java.util.Map;

public record Trait(String name, Map<String, TraitFunction> functions) {
}
