package dev.gamecrash.verteiler.model;

import static dev.gamecrash.verteiler.logging.Colors.*;

public enum LogLevel {
    INFO(BLUE, "INFO"),
    WARN(ORANGE, "WARN"),
    ERROR(RED, "ERROR");

    private final String color;
    private final String label;

    LogLevel(String color, String label) {
        this.color = color;
        this.label = label;
    }

    public String getColor() {
        return color;
    }

    public String getLabel() {
        return label;
    }

    public static LogLevel getValue(String string) {
        try {
            return LogLevel.valueOf(string);
        } catch (IllegalArgumentException e) {
            return LogLevel.INFO;
        }
    }
}
