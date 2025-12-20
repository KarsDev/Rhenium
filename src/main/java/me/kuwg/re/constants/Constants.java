package me.kuwg.re.constants;

import me.kuwg.re.error.errors.RInternalError;

import java.util.regex.Pattern;

public abstract class Constants {
    private Constants() {
        throw new RInternalError();
    }

    public static final class Tokens extends Constants {
        public static final String[] KEYWORDS = {
                "using",
                "mut", "const", "global",
                "for", "if", "else", "while", "do", "break", "continue",
                "_Builtin", "_IR", "_NativeCPP",
                "range", "len", "sizeof", "cast", "_Typeof", "_TypeofLLVM",
                "generic", "func", "return",
                "none", "null", "anyptr",
                "byte", "short", "int", "long",
                "float", "double",
                "bool", "char", "str", "ptr",
                "struct", "init", "impl", "self", "inherits",
                "try", "catch", "raise",
                "is",
                "async",
        };

        public static final String[] BOOLEANS = {
                "true", "false"
        };

        public static final String[] OPERATORS = {
                "and", "or", "not", "in",

                "+", "-", "*", "/", "%",
                "+=", "-=", "*=", "/=", "%=",

                ">", "<", ">=", "<=", "==", "!=",
                "^", "<<", ">>", ">>>",
                ":", "=",
                "->", "@", ".",

                "<<", ">>", ">>>", "&", "|", "~", "^",
                "<<=", ">>=", ">>>=", "&=", "|=", "^=",
        };

        public static final String[] DIVIDERS = {
                "{", "}", "[", "]", "(", ")", ","
        };

        public static final Pattern IDENTIFIER_PATTERN =
                Pattern.compile("^\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*$");

        private static final String HEX_DIGITS = "[0-9a-fA-F_]+";
        private static final String DEC_DIGITS = "[0-9][0-9_]*";
        private static final String BIN_DIGITS = "[01_]+";
        private static final String OCT_DIGITS = "[0-7_]+";

        private static final String HEX_FLOAT = "0[xX]" + HEX_DIGITS + "(?:\\." + "[0-9a-fA-F_]*" + ")?[pP][+-]?[0-9_]+[fFdD]?";
        private static final String DEC_FLOAT =
                "(" + DEC_DIGITS + "\\.[0-9_]*" +
                        "|" +
                        "\\.[0-9_]+" +
                        "|" +
                        DEC_DIGITS + ")" +
                        "([eE][+-]?[0-9_]+)?[fFdD]?";

        private static final String HEX_INT = "0[xX]" + HEX_DIGITS + "[lL]?";
        private static final String BIN_INT = "0[bB]" + BIN_DIGITS + "[lL]?";
        private static final String OCT_INT = "0" + OCT_DIGITS + "[lL]?";
        private static final String DEC_INT = DEC_DIGITS + "[lL]?";

        public static final Pattern NUMBER_PATTERN = Pattern.compile(
                "^(?:" + HEX_FLOAT + "|" + HEX_INT + "|" + BIN_INT + "|" + OCT_INT + "|" + DEC_FLOAT + "|" + DEC_INT + ")"
        );
    }

    public static final class Lang extends Constants {
        public static final String OS = System.getProperty("os.name").toLowerCase();
        public static final boolean WIN = OS.contains("win");

        public static final boolean SUPPORT_NW = false;
    }
}
