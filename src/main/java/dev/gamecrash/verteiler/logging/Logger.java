package dev.gamecrash.verteiler.logging;

import dev.gamecrash.verteiler.config.Configuration;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

import static dev.gamecrash.verteiler.logging.Colors.*;

public class Logger {
    private static Logger instance;
    private Path logFile;
    private boolean logToFile = true;
    private LogLevel logLevel = LogLevel.INFO;

    private final PrintStream out;
    private final PrintStream err;
    private final DateTimeFormatter dateFormatter;
    private final Set<String> logFileQueue = new HashSet<>();

    public Logger() {
        this.out = System.out;
        this.err = System.err;
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        instance = this;
    }

    public void loadConfig() {
        Configuration config = Configuration.getInstance();

        logToFile = config.logToFile;
        if (logToFile) {
            try {
                Path loggingDirectory = Paths.get(config.logDirectory);
                Files.createDirectories(loggingDirectory);
                logFile = Files.createFile(loggingDirectory.resolve(LocalDateTime.now().format(dateFormatter) + ".log"));
            } catch (IOException e) {
                error("Could not create logging directories! ", e);
                System.exit(1);
            }
        } else {
            logFileQueue.clear();
        }

        logLevel = LogLevel.getValue(config.logLevel);
    }

    private String getCallerInfo() {
        try {
            return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(frames -> frames
                    .map(StackWalker.StackFrame::getDeclaringClass)
                    .filter(c -> !c.equals(Logger.class))
                    .findFirst()
                    .map(Class::getSimpleName)
                    .orElse("Unknown"));
        } catch (Throwable t) {
            return "Unknown";
        }
    }

    private String formatMessage(LogLevel level, String message, String caller) {
        String messageColor = switch (level) {
            case ERROR -> RED;
            case WARN -> ORANGE;
            case INFO -> WHITE;
        };

        return DIM + BLUE + "[" + LocalDateTime.now().format(dateFormatter) + "]" + RESET +
            " " + BOLD + level.getColor() + "[" + level.getLabel() + "]" + RESET +
            " " + DIM + PURPLE + "[" + caller + "]" + RESET +
            " " + messageColor + message + RESET;
    }

    private String formatWithArgs(String message, Object... args) {
        if (args == null || args.length == 0) return message;

        StringBuilder result = new StringBuilder();
        int argIndex = 0;
        int i = 0;
        while (i < message.length()) {
            if (i < message.length() - 1 && message.charAt(i) == '{' && message.charAt(i + 1) == '}') {
                if (argIndex < args.length) result.append(args[argIndex++]);
                else result.append("{}");
                i += 2;
            } else {
                result.append(message.charAt(i));
                i++;
            }
        }
        return result.toString();
    }

    public void info(String message, Object... args) {
        log(LogLevel.INFO, message, args);
    }

    public void warn(String message, Object... args) {
        log(LogLevel.WARN, message, args);
    }

    public void error(String message, Object... args) {
        log(LogLevel.ERROR, message, args);
    }

    public void error(String message, Throwable throwable, Object... args) {
        log(LogLevel.ERROR, message, args);
        if (throwable != null) {
            err.print(RED);
            throwable.printStackTrace(err);
            err.print(RESET);
        }
    }

    private void log(LogLevel level, String message, Object... args) {
        if (level.ordinal() < logLevel.ordinal()) return;

        String caller = getCallerInfo();
        String formattedMessage = formatWithArgs(message, args);
        String output = formatMessage(level, formattedMessage, caller);

        if (level == LogLevel.ERROR) err.println(output);
        else out.println(output);

        if (!logToFile) return;

        String plainOutput = "[" + LocalDateTime.now().format(dateFormatter) + "]" +
            " [" + level.getLabel() + "]" +
            " [" + caller + "] " +
            formattedMessage;

        logFileQueue.add(plainOutput);
        if (logFileQueue.size() >= 64) flushQueue();
    }

    public void flushQueue() {
        if (!logToFile) return;
        try {
            Files.writeString(logFile, String.join("\n", logFileQueue), StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Error on writing logs to file:");
            e.printStackTrace();
        } finally {
            logFileQueue.clear();
        }
    }

    public static Logger getInstance() {
        if (instance == null) instance = new Logger();
        return instance;
    }
}