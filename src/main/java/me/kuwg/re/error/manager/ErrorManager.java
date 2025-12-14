package me.kuwg.re.error.manager;

import me.kuwg.re.error.codes.ErrorCodes;

import java.lang.reflect.Field;

final class ErrorManager {
    static void raise(RError error) {
        System.err.println("An exception occurred: ");
        System.err.println("  Message: " + error.getMessage());
        System.err.println("  At line " + error.getLine());
        System.err.println("  Internal error name: \"" + getIEN(error.getCode()) + "\"");

        //System.exit(error.getCode());
        throw new RuntimeException();
    }

    private static String getIEN(int code) {
        try {
            for (final Field f : ErrorCodes.class.getFields()) {
                if (f.getInt(null) != code) continue;
                String name = f.getName().toLowerCase().replace("_", " ");

                return name.substring(0, name.length() - 6);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        return "Unknown";
    }
}
