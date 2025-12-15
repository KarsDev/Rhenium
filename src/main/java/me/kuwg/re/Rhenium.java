package me.kuwg.re;

import me.kuwg.re.cli.ArgumentParser;
import me.kuwg.re.cli.Arguments;
import me.kuwg.re.env.EnvironmentValidator;
import me.kuwg.re.pipeline.CompilerPipeline;

public final class Rhenium {
    public static void main(String[] args) {
        if (!EnvironmentValidator.isSupported()) {
            System.err.println(EnvironmentValidator.errorMessage());
            return;
        }

        Arguments arguments;
        try {
            arguments = ArgumentParser.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            ArgumentParser.printUsage();
            return;
        }

        CompilerPipeline pipeline = new CompilerPipeline(arguments);
        pipeline.run();
    }
}
