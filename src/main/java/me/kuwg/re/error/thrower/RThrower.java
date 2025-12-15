package me.kuwg.re.error.thrower;

import me.kuwg.re.cli.ArgumentParser;
import me.kuwg.re.error.errors.RInternalError;

public class RThrower {
    public static <T> T throwError(String e) {
        System.err.println(e);
        System.exit(Integer.MIN_VALUE);

        ArgumentParser.printUsage();

        throw new RInternalError();
    }
}
