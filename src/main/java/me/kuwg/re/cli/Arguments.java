package me.kuwg.re.cli;

import me.kuwg.re.cli.annotations.IncompatibleWith;
import me.kuwg.re.error.thrower.RThrower;

import java.io.File;
import java.util.List;

public final class Arguments {
    private final File inputFile;
    private final File outputFile;

    @IncompatibleWith({"emitLLVM"})
    private final boolean runOutput;

    @IncompatibleWith({"runOutput"})
    private final boolean emitLLVM;

    @IncompatibleWith({"runOutput"})
    private final boolean dumpAST;

    private final List<String> clangArgs;

    private Arguments(Builder b) {
        this.inputFile = b.inputFile;
        this.outputFile = b.outputFile;
        this.runOutput = b.runOutput;
        this.emitLLVM = b.emitLLVM;
        this.dumpAST = b.dumpAST;
        this.clangArgs = List.copyOf(b.clangArgs);
    }

    public File inputFile() {
        return inputFile;
    }

    public File outputFile() {
        return outputFile;
    }

    public boolean runOutput() {
        return runOutput;
    }

    public boolean emitLLVM() {
        return emitLLVM;
    }

    public boolean dumpAST() {
        return dumpAST;
    }

    public List<String> clangArgs() {
        return clangArgs;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static final class Builder {
        File inputFile;
        File outputFile = new File("output.llvm");
        boolean runOutput = true;
        boolean emitLLVM = false;
        boolean dumpAST = false;
        List<String> clangArgs = List.of();

        public Builder inputFile(File f) {
            inputFile = f;
            return this;
        }

        public Builder outputFile(File f) {
            outputFile = f;
            return this;
        }

        public Builder runOutput(boolean v) {
            runOutput = v;
            return this;
        }

        public Builder emitLLVM(boolean v) {
            emitLLVM = v;
            return this;
        }

        public Builder dumpAST(boolean v) {
            dumpAST = v;
            return this;
        }

        public Builder clangArgs(List<String> v) {
            clangArgs = v;
            return this;
        }

        public Arguments build() {
            if (inputFile == null) {
                return RThrower.throwError("Input file not specified");
            }
            return new Arguments(this);
        }
    }
}
