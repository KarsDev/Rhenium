package me.kuwg.re.env;

import static me.kuwg.re.constants.Constants.Lang.*;

public final class EnvironmentValidator {

    public static boolean isSupported() {
        return WIN || SUPPORT_NW;
    }

    public static String errorMessage() {
        return "This OS is not supported: " + OS;
    }
}
