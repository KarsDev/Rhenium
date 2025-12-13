package me.kuwg.re.runner;

import static me.kuwg.re.constants.Constants.Lang.*;

public final class CommandRunner {

    public static void run(String command) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(
                WIN ? "cmd.exe" : "sh",
                WIN ? "/c" : "-c",
                command
        );

        builder.redirectErrorStream(true);
        Process process = builder.start();

        String output = new String(process.getInputStream().readAllBytes());
        if (!output.isBlank()) {
            System.out.println(output);
        }

        int exitCode = process.waitFor();
        System.out.println("Compiler exited with code: " + exitCode);

        if (exitCode != 0) {
            throw new RuntimeException("Execution failed with code " + exitCode);
        }
    }
}
