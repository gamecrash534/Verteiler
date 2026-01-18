package dev.gamecrash.verteiler.config;

import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

        instance = this;
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private void createDefaultConfig(Path path) throws IOException {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("config.toml")
        String conf = new String(stream.readAllBytes(), StandardCharsets.UTF_8).formatted(generateToken());
        if (path.getParent() != null) Files.createDirectories(path.getParent());
        Files.writeString(path, conf);
    }

    public Object getObject(String key, Object defaultValue) {
        Object val = toml.get(key);
        return val == null ? defaultValue : val;
    }

    public String getString(String key, String defaultValue) {
        return (String) getObject(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        return (int) getObject(key, defaultValue);
    }

    public long getLong(String key, long defaultValue) {
        return (long) getObject(key, defaultValue);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return (boolean) getObject(key, defaultValue);
    }

    public static Configuration getInstance() {
        return instance;
    }
}
