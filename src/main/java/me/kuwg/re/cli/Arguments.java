package me.kuwg.re.cli;

import me.kuwg.re.cli.annotations.IncompatibleWith;
import me.kuwg.re.error.thrower.RThrower;

import java.io.File;
import java.util.List;

import static me.kuwg.re.constants.Constants.Lang.WIN;

public final class Arguments {
    private final File inputFile;
    private final File llvmFile;
    private final File executableFile;

    @IncompatibleWith({"dumpAST"})
    private final boolean runOutput;

    @IncompatibleWith({"dumpAST"})
    private final boolean keepLLVM;

    @IncompatibleWith({"runOutput"})
    private final boolean dumpAST;

    private final List<String> clangArgs;

    private Arguments(Builder b) {
        this.inputFile = b.inputFile;
        this.llvmFile = b.llvmFile;
        this.executableFile = b.executableFile;
        this.runOutput = b.runOutput;
        this.keepLLVM = b.keepLLVM;
        this.dumpAST = b.dumpAST;
        this.clangArgs = List.copyOf(b.clangArgs);
    }

    public File inputFile() {
        return inputFile;
    }

    public File llvmFile() {
        return llvmFile;
    }

    public File executableFile() {
        return executableFile;
    }

    public boolean runOutput() {
        return runOutput;
    }

    public boolean keepLLVM() {
        return keepLLVM;
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
        File llvmFile;
        File executableFile;
        boolean runOutput = true;
        boolean keepLLVM = false;
        boolean dumpAST = false;
        List<String> clangArgs = List.of();

        public Builder inputFile(File f) {
            inputFile = f;
            return this;
        }

        public Builder llvmFile(File f) {
            llvmFile = f;
            return this;
        }

        public Builder executableFile(File f) {
            executableFile = f;
            return this;
        }

        public Builder runOutput(boolean v) {
            runOutput = v;
            return this;
        }

        public Builder keepLLVM(boolean v) {
            keepLLVM = v;
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

            if (llvmFile == null) {
                String base = executableFile != null
                        ? removeExtension(executableFile.getPath())
                        : new File(inputFile.getParent(), "output").getPath();

                llvmFile = new File(base + ".ll");
            }

            if (executableFile == null) {
                executableFile = new File(
                        inputFile.getParent(),
                        WIN ? "output.exe" : "output.out"
                );
            }

            return new Arguments(this);
        }
    }

    private static String removeExtension(String path) {
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        int dot = path.lastIndexOf('.');

        if (dot > slash) {
            return path.substring(0, dot);
        }

        return path;
    }
}
