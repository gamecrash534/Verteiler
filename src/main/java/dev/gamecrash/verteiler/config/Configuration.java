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

    // Admin
    public final boolean adminEnabled;
    public final String adminToken;

    // UI
    public final String title;
    public final boolean showFileSizes;
    public final boolean showDates;
    public final boolean dragDropUpload;
    public final boolean enablePreview;

    // UI, Footer
    public final boolean footerEnabled;
    public final boolean showCredits;

     // Security
    public final boolean allowDirectoryListing;
    public final String[] allowedExtensions;

    // Performance
    public final boolean enableCaching;
    public final int cacheMaxAge;
    public final boolean minifyFiles;

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

        adminEnabled = getBoolean("admin.enabled", true);
        adminToken = getString("admin.token", generateToken());

        title = getString("ui.title", "Verteiler");
        showFileSizes = getBoolean("ui.show_file_sizes", true);
        showDates = getBoolean("ui.show_dates", true);
        dragDropUpload = getBoolean("ui.drag_drop_upload", true);
        enablePreview = getBoolean("ui.enable_preview", true);

        footerEnabled = getBoolean("ui.footer.enabled", true);
        showCredits = getBoolean("ui.footer.show_credits", true);

        allowDirectoryListing = getBoolean("security.allow_directory_listing", true);
        String extensions = getString("security.allowed_extensions", "");
        this.allowedExtensions = extensions.isEmpty() ? new String[0] : extensions.split(",");

        enableCaching = getBoolean("performance.enable_caching", true);
        cacheMaxAge = getInt("performance.cache_max_age", 3600);
        minifyFiles = getBoolean("performance.minify_files", true);
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
