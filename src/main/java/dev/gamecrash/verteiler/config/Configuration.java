package dev.gamecrash.verteiler.config;

import dev.gamecrash.verteiler.logging.Logger;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class Configuration {
    private final TomlParseResult toml;
    private static Configuration instance;

    // Server
    public final String host;
    public final int port;
    public final String dataDirectory;

    // Logging
    public final String logLevel;
    public final boolean logAccess;
    public final boolean logToFile;
    public final String logDirectory;

    public Configuration(Path configPath) throws IOException {
        if (!Files.exists(configPath)) createDefaultConfig(configPath);
        toml = Toml.parse(configPath);

        host = getString("server.host", "0.0.0.0");
        port = getInt("server.port", 2987);
        dataDirectory = getString("server.data_directory", "./data");

        logLevel = getString("logging.level", "INFO");
        logAccess = getBoolean("logging.log_access", true);
        logToFile = getBoolean("logging.log_to_file", false);
        logDirectory = getString("logging.log_directory", "./logs");
    }

    public static Configuration load(Path path) {
        try {
            instance = new Configuration(path);
            return instance;
        } catch (IOException e) {
            Logger.getInstance().error("Failed to load configuration: ", e);
            Logger.getInstance().info("Exiting");
            System.exit(1);
        }
        return null;
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private void createDefaultConfig(Path path) throws IOException {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("config.toml");
        String conf = new String(stream.readAllBytes(), StandardCharsets.UTF_8).formatted(generateToken());
        if (path.getParent() != null) Files.createDirectories(path.getParent());
        Files.writeString(path, conf);
    }

    private String getString(String key, String defaultValue) {
        String value = toml.getString(key);
        return value == null ? defaultValue : value;
    }

    private int getInt(String key, int defaultValue) {
        Long value = toml.getLong(key);
        return value == null ? defaultValue : value.intValue();
    }

    private long getLong(String key, long defaultValue) {
        Long value = toml.getLong(key);
        return value == null ? defaultValue : value;
    }

    private boolean getBoolean(String key, boolean defaultValue) {
        Boolean value = toml.getBoolean(key);
        return value == null ? defaultValue : value;
    }

    public static Configuration getInstance() {
        return instance;
    }

    public Path getDataPath() {
        return Paths.get(dataDirectory);
    }
}
