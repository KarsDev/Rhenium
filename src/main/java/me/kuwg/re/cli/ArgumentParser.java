package me.kuwg.re.cli;

import me.kuwg.re.error.thrower.RThrower;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

public final class ArgumentParser {

    private static final Map<String, Option> OPTIONS = Map.ofEntries(
            opt("-out", true, (b, v) -> b.outputFile(new File(v))),
            opt("-no-run", false, (b, ignore) -> b.runOutput(false)),
            opt("-keep-llvm", false, (b, ignore) -> b.keepLLVM(true)),
            opt("-dump-ast", false, (b, ignore) -> b.dumpAST(true)),
            opt("-clang-args", true, (b, v) -> b.clangArgs(Arrays.asList(v.split(","))))

    );

    public static Arguments parse(String[] args) {
        if (args.length == 0) {
            return RThrower.throwError("Missing input file");
        }

        Arguments.Builder builder = new Arguments.Builder();

        File input = new File(args[0]);
        if (!input.exists()) {
            return RThrower.throwError("File not found: " + input);
        }

        builder.inputFile(input);

        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            Option option = OPTIONS.get(arg);

            if (option == null) {
                return RThrower.throwError("Unknown option: " + arg);
            }

            String value = null;
            if (option.takesValue()) {
                if (i + 1 >= args.length) {
                    return RThrower.throwError("Missing value for " + arg);
                }
                value = args[++i];
            }

            option.apply().accept(builder, value);
        }

        Arguments arguments = builder.build();
        ArgumentValidator.validate(arguments);
        return arguments;
    }

    private static Map.Entry<String, Option> opt(
            String name,
            boolean takesValue,
            java.util.function.BiConsumer<Arguments.Builder, String> fn
    ) {
        return Map.entry(name, new Option(name, takesValue, fn));
    }

    public static void printUsage() {
        System.out.println("""
                Usage: rhenium <input.re> [options]
                
                Options:
                  -out <file>           Output LLVM file
                  -keep-llvm            Keep LLVM IR
                  -dump-ast             Print AST
                  -clang-args a,b,c     Extra clang arguments
                  -no-run               Do not compile output LLVM
                """);
    }
}
