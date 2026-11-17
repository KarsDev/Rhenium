package me.kuwg.re.env;

import java.io.IOException;

public final class EnvironmentValidator {
    public static boolean isSupported() {
        return OperatingSystem.isWindows() || OperatingSystem.isLinux() || OperatingSystem.isMacOS();
    }

    public static String notSupportedErrorMessage() {
        return "This OS is not supported: " + OperatingSystem.current().name();
    }

    public static boolean isClangInstalled() {
        try {
            Process process = new ProcessBuilder("clang", "--version")
                    .redirectErrorStream(true)
                    .start();

            return process.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            return false;
        }
    }

    public static String clangNotInstalledErrorMessage() {
        return "Clang is not installed or could not be executed.";
    }
}