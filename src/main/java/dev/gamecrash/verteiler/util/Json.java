package dev.gamecrash.verteiler.util;

import java.time.Instant;

public class Json {
    public static String object(Object... keyValues) {
        if (keyValues.length % 2 != 0) throw new IllegalArgumentException("please provide kv pairs");
        StringBuilder builder = new StringBuilder(keyValues.length * 16);
        builder.append('{');
        for (int i = 0; i  < keyValues.length; i += 2) {
            if (i > 0) builder.append(',');
            escapeStringTo(builder, keyValues[i].toString());
            builder.append(':');
            stringifyTo(builder, keyValues[i + 1]);
        }

        return builder.append('}').toString();
    }

    private static void escapeStringTo(StringBuilder builder, String string) {
        builder.append('"');
        for (int i = 0, len = string.length(); i < len; i++) {
            char c = string.charAt(i);
            switch (c) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> { if (c < 32) builder.append(String.format("\\u%04x", (int) c)); else builder.append(c); }
            }
        }
        builder.append('"');
    }

    private static void stringifyTo(StringBuilder builder, Object object) {
        if (object == null) builder.append("null");
        else if (object instanceof String str) escapeStringTo(builder, str);
        else if (object instanceof Number || object instanceof Boolean) builder.append(object);
        else if (object instanceof Instant instant) escapeStringTo(builder, instant.toString());
    }
}
