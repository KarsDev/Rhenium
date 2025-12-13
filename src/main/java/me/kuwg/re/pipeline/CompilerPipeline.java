package me.kuwg.re.pipeline;

import me.kuwg.re.ast.AST;
import me.kuwg.re.cli.Arguments;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.frontend.Frontend;
import me.kuwg.re.runner.CommandRunner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@SuppressWarnings("ResultOfMethodCallIgnored")
public final class CompilerPipeline {
    private final Arguments args;

    public CompilerPipeline(Arguments args) {
        this.args = args;
    }

    public void run() {
        try {
            Frontend frontend = new Frontend(args.inputFile());
            var ast = frontend.parse();

            CompilationContext cctx = new CompilationContext();
            ast.compile(cctx);

            String command = cctx.compileAndGet(args.outputFile(), args.clangArgs());

            if (args.dumpAST()) dumpAST(ast);
            else if (args.runOutput()) CommandRunner.run(command);
            else if (!args.emitLLVM()) args.outputFile().delete();

        } catch (Exception e) {
            System.err.println("Compilation failed");
            e.printStackTrace(System.err);
        }
    }

    private void dumpAST(AST ast) {
        File dmp = new File("ast.txt");

        dmp.mkdirs();

        try (FileWriter w = new FileWriter(dmp)) {
            w.write(ast.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
