package me.kuwg.re.runner;

import me.kuwg.re.env.OperatingSystem;

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
            System.err.println("Execution failed with code " + exitCode);
        }
        System.exit(exitCode);
    }
}
