package dev.gamecrash.verteiler.util;

import dev.gamecrash.verteiler.config.Configuration;

public class Minifier {
    private static final String cssSeparators = "{}:;,>+~()[]";
    private static final String[] preserveTags = {"script", "style", "pre", "textarea"};
    private static final String newlineChars = ")]}\"'`+-";
    private static final String newlineBeforeChars = "([{+-!~";
    private static final String regexStartChars = "=(,[!&|?:;{}\n";
    private static final String[] regexKeywords = {"return", "typeof", "void", "delete", "new", "case", "throw", "in", "instanceof"};

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
                    if (!isCSSSeparator(last) && i + 1 < len && !isCSSSeparator(next) && !Character.isWhitespace(next))
                        result.append(' ');
                }
                continue;
            }

            if (!result.isEmpty() && result.charAt(result.length() - 1) == ' ' && isCSSSeparator(c))
                result.setLength(result.length() - 1);
            result.append(c);
        }
        return result.toString().trim();
    }

    public static String minifyJS(String js) {
        if (!config.minifyFiles) return js;
        if (js == null || js.isEmpty()) return "";

        int len = js.length();
        StringBuilder result = new StringBuilder(len);
        boolean inSingleComment = false, inMultiComment = false, inString = false, inTemplate = false, inRegex = false;
        char stringChar = 0;

        for (int i = 0; i < len; i++) {
            char c = js.charAt(i);
            char next = i + 1 < len ? js.charAt(i + 1) : 0;
            char prev = i > 0 ? js.charAt(i - 1) : 0;

            if (inSingleComment) {
                if (c == '\n') {
                    inSingleComment = false;
                    if (!result.isEmpty() && needsNewline(result.charAt(result.length() - 1))) result.append('\n');
                }
                continue;
            }

            if (inMultiComment) {
                if (c == '*' && next == '/') {
                    inMultiComment = false;
                    i++;
                }
                continue;
            }
            if (inTemplate) {
                result.append(c);
                if (c == '`' && prev != '\\') inTemplate = false;
                continue;
            }
            if (inString) {
                result.append(c);
                if (c == stringChar && prev != '\\') inString = false;
                continue;
            }
            if (inRegex) {
                result.append(c);
                if (c == '/' && prev != '\\') inRegex = false;
                continue;
            }

            if (c == '`') {
                inTemplate = true;
                result.append(c);
                continue;
            }
            if (c == '"' || c == '\'') {
                inString = true;
                stringChar = c;
                result.append(c);
                continue;
            }

            if (c == '/') {
                if (next == '/') {
                    inSingleComment = true;
                    i++;
                    continue;
                }
                if (next == '*') {
                    inMultiComment = true;
                    i++;
                    continue;
                }
                if (isRegexStart(result)) {
                    inRegex = true;
                    result.append(c);
                    continue;
                }
            }

            if (Character.isWhitespace(c)) {
                if (!result.isEmpty()) {
                    char last = result.charAt(result.length() - 1);
                    if (c == '\n' && needsNewline(last) && !Character.isWhitespace(last)) result.append('\n');
                    else if (c != '\n' && needsSpace(last, next)) result.append(' ');
                }
                continue;
            }

            if (!result.isEmpty()) {
                char last = result.charAt(result.length() - 1);
                if (last == ' ' && !isIdentChar(c)) result.setLength(result.length() - 1);
                else if (last == '\n' && !needsNewlineBefore(c)) {
                    result.setLength(result.length() - 1);
                    if (needsSpace(getLastNonWhitespace(result), c)) result.append(' ');
                }
            }
            result.append(c);
        }
        return result.toString().trim();
    }

    public static String minifyHTML(String html) {
        if (!config.minifyFiles) return html;
        if (html == null || html.isEmpty()) return "";

        int len = html.length();
        StringBuilder result = new StringBuilder(len);
        int i = 0;

        while (i < len) {
            char c = html.charAt(i);

            if (c == '<' && i + 3 < len && html.charAt(i + 1) == '!' && html.charAt(i + 2) == '-' && html.charAt(i + 3) == '-') {
                int endComment = html.indexOf("-->", i + 4);
                i = (endComment != -1) ? endComment + 3 : len;
                continue;
            }

            if (c == '<') {
                String matchedTag = matchPreserveTag(html, i, len);
                if (matchedTag != null) {
                    int tagEnd = html.indexOf('>', i);
                    if (tagEnd != -1) {
                        result.append(minifyTag(html, i, tagEnd + 1));
                        i = tagEnd + 1;

                        int closeStart = indexOfIgnoreCase(html, "</" + matchedTag, i);
                        closeStart = (closeStart != -1) ? closeStart : len;

                        String content = html.substring(i, closeStart);
                        switch (matchedTag) {
                            case "script" -> result.append(minifyJS(content));
                            case "style" -> result.append(minifyCSS(content));
                            default -> result.append(content);
                        }
                        i = closeStart;
                        continue;
                    }
                }

                int tagEnd = html.indexOf('>', i);
                if (tagEnd != -1) {
                    result.append(minifyTag(html, i, tagEnd + 1));
                    i = tagEnd + 1;
                    continue;
                }
            }

            if (Character.isWhitespace(c)) {
                while (i < len && Character.isWhitespace(html.charAt(i))) i++;
                if (!result.isEmpty() && i < len) {
                    char last = result.charAt(result.length() - 1);
                    char nextChar = html.charAt(i);
                    boolean textBefore = last != '>';
                    boolean textAfter = nextChar != '<';
                    if (textBefore || textAfter) {
                        result.append(' ');
                    }
                }
                continue;
            }

            result.append(c);
            i++;
        }

        return result.toString().trim();
    }

    private static String matchPreserveTag(String html, int pos, int len) {
        for (String tag : preserveTags) {
            int tagLen = tag.length();
            if (pos + tagLen + 1 < len && html.charAt(pos) == '<') {
                boolean matches = true;
                for (int j = 0; j < tagLen && matches; j++) {
                    if (Character.toLowerCase(html.charAt(pos + 1 + j)) != tag.charAt(j)) matches = false;
                }
                if (matches && !Character.isLetterOrDigit(html.charAt(pos + 1 + tagLen))) {
                    return tag;
                }
            }
        }
        return null;
    }

    private static int indexOfIgnoreCase(String str, String target, int fromIndex) {
        int targetLen = target.length();
        int max = str.length() - targetLen;
        for (int i = fromIndex; i <= max; i++) {
            boolean found = true;
            for (int j = 0; j < targetLen && found; j++) {
                if (Character.toLowerCase(str.charAt(i + j)) != Character.toLowerCase(target.charAt(j))) {
                    found = false;
                }
            }
            if (found) return i;
        }
        return -1;
    }

    private static String minifyTag(String html, int start, int end) {
        StringBuilder result = new StringBuilder(end - start);
        boolean inQuote = false;
        char quoteChar = 0;
        boolean lastWasSpace = false;

        for (int i = start; i < end; i++) {
            char c = html.charAt(i);

            if (!inQuote && (c == '"' || c == '\'')) {
                inQuote = true;
                quoteChar = c;
                result.append(c);
                lastWasSpace = false;
            } else if (inQuote && c == quoteChar) {
                inQuote = false;
                result.append(c);
            } else if (inQuote) {
                result.append(c);
            } else if (Character.isWhitespace(c)) {
                if (!lastWasSpace && !result.isEmpty() && result.charAt(result.length() - 1) != '<') {
                    result.append(' ');
                    lastWasSpace = true;
                }
            } else {
                if (lastWasSpace && (c == '>' || c == '/' || c == '=')) {
                    result.setLength(result.length() - 1);
                }
                result.append(c);
                lastWasSpace = false;
            }
        }

        return result.toString();
    }

    private static boolean isRegexStart(StringBuilder sb) {
        if (sb.isEmpty()) return true;
        int i = sb.length() - 1;
        while (i >= 0 && Character.isWhitespace(sb.charAt(i))) i--;
        if (i < 0) return true;

        char last = sb.charAt(i);
        if (regexStartChars.indexOf(last) >= 0) return true;

        String content = sb.toString().trim();
        for (String kw : regexKeywords) if (content.endsWith(kw)) return true;
        return false;
    }

    private static char getLastNonWhitespace(StringBuilder sb) {
        for (int i = sb.length() - 1; i >= 0; i--) {
            if (!Character.isWhitespace(sb.charAt(i))) return sb.charAt(i);
        }
        return 0;
    }

    private static boolean needsNewline(char c) {
        return Character.isLetterOrDigit(c) || newlineChars.indexOf(c) >= 0;
    }

    private static boolean needsSpace(char prev, char next) {
        return isIdentChar(prev) && isIdentChar(next);
    }

    private static boolean isIdentChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    private static boolean needsNewlineBefore(char c) {
        return isIdentChar(c) || newlineBeforeChars.indexOf(c) >= 0;
    }

    private static boolean isCSSSeparator(char c) {
        return cssSeparators.indexOf(c) >= 0;
    }
}
