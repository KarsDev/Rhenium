package me.kuwg.re.compiler.struct;

import me.kuwg.re.compiler.function.RFunction;

public record RPendingDestructor(String addrReg, RFunction destructor) {
}