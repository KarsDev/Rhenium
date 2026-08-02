package me.kuwg.re.error.thrower;

import me.kuwg.re.cli.ArgumentParser;
import me.kuwg.re.error.errors.compiler.RPreCompilationError;

public final class RThrower {
    public static <T> T throwError(String e) {
        ArgumentParser.printUsage();

        return new RPreCompilationError(e).raise();
    }
}
