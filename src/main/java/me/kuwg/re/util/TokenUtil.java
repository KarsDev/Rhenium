package me.kuwg.re.util;

public final class TokenUtil {
    @SafeVarargs
    private static <T> T[] arr(T... values) {
        return values;
    }

    public static final class Keywords {
        public static String[] loadKeywords() {
            final String[] dataTypes = dataTypes();

            final String[] keywords = new String[dataTypes.length];

            int index = 0;

            for (final String val : dataTypes) {
                keywords[index++] = val;
            }

            return keywords;
        }

        private static String[] dataTypes() {
            return arr(
                    "int", "int32", "int64", "dec", "str"
            );
        }


    }

    public static final class Operators {
        public static String[] loadOperators() {
            return arr(
                    "+", "-", "*", "/", "%",
                    "=",
                    "&", "^", "<", "<<", ">", ">>", "|",
                    "&&", "||", "==", "!=", "!"
            );
        }
    }

    public static final class Dividers {
        public static Character[] loadDividers() {
            return arr(
                    '{', '[', '(', ')', ']', '}', ','
            );
        }
    }
}
