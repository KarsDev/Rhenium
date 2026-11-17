package me.kuwg.re.runner;

import me.kuwg.re.env.OperatingSystem;
import me.kuwg.re.error.errors.RInternalError;

public final class CommandRunner {
    public static void run(String command) throws Exception {
        boolean win = OperatingSystem.isWindows();
        ProcessBuilder builder = new ProcessBuilder(
                win ? "cmd.exe" : "sh",
                win ? "/c" : "-c",
                command
        );

        builder.redirectErrorStream(true);
        Process process = builder.start();

        String output = new String(process.getInputStream().readAllBytes());
        if (!output.isBlank()) {
            System.out.println(output);
        }

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RInternalError("Execution failed with code " + exitCode);
        }
        System.exit(exitCode);
    }
}
