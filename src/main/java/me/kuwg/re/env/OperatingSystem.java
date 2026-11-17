package me.kuwg.re.env;

import java.util.Locale;

public enum OperatingSystem {
    WINDOWS,
    LINUX,
    MACOS,
    UNKNOWN;

    private static final OperatingSystem CURRENT = detect();

    private static OperatingSystem detect() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);

        if (os.contains("win")) {
            return WINDOWS;
        }

        if (os.contains("mac") || os.contains("darwin")) {
            return MACOS;
        }

        if (os.contains("linux")) {
            return LINUX;
        }

        return UNKNOWN;
    }

    public static OperatingSystem current() {
        return CURRENT;
    }

    public static boolean isWindows() {
        return CURRENT == WINDOWS;
    }

    public static boolean isLinux() {
        return CURRENT == LINUX;
    }

    public static boolean isMacOS() {
        return CURRENT == MACOS;
    }
}