package dev.gamecrash.verteiler.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class Configuration {
    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);
    private final TomlParseResult toml;
    private static Configuration instance;

    public Configuration(Path configPath) throws IOException {
        if (!Files.exists(configPath)) createDefaultConfig(configPath);
        toml = Toml.parse(configPath);

        instance = this;
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private void createDefaultConfig(Path path) {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("config.toml")) {
            String conf = new String(stream.readAllBytes(), StandardCharsets.UTF_8).formatted(generateToken());
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            Files.writeString(path, conf);
        } catch (Exception e) {
            logger.error("Could not load config, stopping");
            System.exit(1);
        }
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
