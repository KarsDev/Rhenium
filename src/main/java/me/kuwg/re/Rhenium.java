package me.kuwg.re;

import me.kuwg.re.ast.AST;
import me.kuwg.re.compiler.CompilationContext;
import me.kuwg.re.parser.ASTParser;
import me.kuwg.re.token.Tokenizer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static me.kuwg.re.constants.Constants.Lang.*;

public class Rhenium {
    public static void main(String[] args) throws IOException {
        if (!WIN && !SUPPORT_NW) {
            System.err.println("This OS is not supported: " + OS);
            return;
        }

        if (args.length == 0) {
            System.err.println("Please provide at least one argument: input file");
            return;
        }

        File input = new File(args[0]);

        if (!input.exists()) {
            System.err.println("File not found: " + input.getName());
            return;
        }

        String source = new String(Files.readAllBytes(input.toPath()));

        var tokens = Tokenizer.tokenize(source);

        ASTParser parser = new ASTParser(input.getName(), tokens, true);
        AST ast = parser.parse();

        var cctx = new CompilationContext();

        ast.compile(cctx);

        File output = new File("files/output.llvm");

        String cmd = cctx.compileAndGet(output);

        try {
            ProcessBuilder builder = new ProcessBuilder(
                    WIN ? "cmd.exe" : "sh",
                    WIN ? "/c" : "-c",
                    cmd
            );

            builder.redirectErrorStream(true);

            Process process = builder.start();

            try (var out = process.getInputStream();
                 var err = process.getErrorStream()) {

                String stdout = new String(out.readAllBytes());
                String stderr = new String(err.readAllBytes());

                if (!stdout.isBlank()) System.out.println(stdout);
                if (!stderr.isBlank()) System.err.println(stderr);
            }

            int exitCode = process.waitFor();
            System.out.println("Compiler exited with code: " + exitCode);

            if (exitCode != 0) {
                System.err.println("Compilation failed with code " + exitCode);
            }

        } catch (Exception e) {
            System.err.println("Failed to run compilation command");
            e.printStackTrace(System.err);
        }
    }
}
