package dev.gamecrash.verteiler.util;

import dev.gamecrash.verteiler.config.Configuration;

public class Minifier {
    private static final String cssSeparators = "{}:;,>+~()[]";

    private static final Configuration config = Configuration.getInstance();

    public static String minifyCSS(String css) {
        if (!config.minifyFiles) return css;
        if (css == null || css.isEmpty()) return "";

        int len = css.length();
        StringBuilder result = new StringBuilder(len);
        boolean inComment = false, inString = false;
        char stringChar = 0;

        for (int i = 0; i < len; i++) {
            char c = css.charAt(i);
            char next = i + 1 < len ? css.charAt(i + 1) : 0;

            if (!inComment && (c == '"' || c == '\'')) {
                if (!inString) {
                    inString = true;
                    stringChar = c;
                } else if (c == stringChar && (i == 0 || css.charAt(i - 1) != '\\')) inString = false;
                result.append(c);

                continue;
            }

            if (inString) {
                result.append(c);
                continue;
            }

            if (!inComment && c == '/' && next == '*') {
                inComment = true;
                i++;
                continue;
            }
            if (inComment) {
                if (c == '*' && next == '/') {
                    inComment = false;
                    i++;
                }
                continue;
            }

            if (Character.isWhitespace(c)) {
                if (!result.isEmpty()) {
                    char last = result.charAt(result.length() - 1);
                    if (!isCSSSeparator(last) && i + 1 < len && !isCSSSeparator(next) && !Character.isWhitespace(next)) result.append(' ');
                }
                continue;
            }

            if (!result.isEmpty() && result.charAt(result.length() - 1) == ' ' && isCSSSeparator(c)) result.setLength(result.length() - 1);
            result.append(c);
        }
        return result.toString().trim();
    }

    private static boolean isCSSSeparator(char c) {
        return cssSeparators.indexOf(c) >= 0;
    }

}
