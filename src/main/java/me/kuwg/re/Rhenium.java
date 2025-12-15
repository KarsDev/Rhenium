package me.kuwg.re;

import me.kuwg.re.cli.ArgumentParser;
import me.kuwg.re.cli.Arguments;
import me.kuwg.re.env.EnvironmentValidator;
import me.kuwg.re.error.thrower.RThrower;
import me.kuwg.re.pipeline.CompilerPipeline;

public final class Rhenium {
    public static void main(String[] args) {
        if (!EnvironmentValidator.isSupported()) {
            RThrower.throwError(EnvironmentValidator.errorMessage());
        }

        Arguments arguments = ArgumentParser.parse(args);

        CompilerPipeline pipeline = new CompilerPipeline(arguments);
        pipeline.run();
    }
}
