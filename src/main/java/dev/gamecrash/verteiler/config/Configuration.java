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

    // Admin, chunked uploads
    public final boolean chunkedUploadsEnabled;
    public final long chunkSize;
    public final Path tempUploadDirectory;
    public final long uploadSessionTtl;

    // UI
    public final String title;
    public final boolean showFileSizes;
    public final boolean showDates;
    public final boolean dragDropUpload;
    public final boolean enablePreview;

    // UI, Footer
    public final boolean footerEnabled;
    public final boolean showCredits;

    // UI, Resources
    public final boolean useCustomResources;
    public final String customResourcesDirectory;

     // Security
    public final boolean allowDirectoryListing;
    public final String[] allowedExtensions;

    // Performance
    public final boolean enableCaching;
    public final int cacheMaxAge;
    public final boolean minifyFiles;

    public Configuration(Path configPath) throws IOException {
        String env_conf = System.getenv("VERTEILER_CONFIG");
        if (env_conf != null) configPath = Path.of(env_conf);

        if (!Files.exists(configPath)) createDefaultConfig(configPath);
        toml = Toml.parse(configPath);

        tempUploadDirectory = Files.createTempDirectory("verteiler");

        host = getString("server.host", "VERTEILER_HOST", "0.0.0.0");
        port = getInt("server.port", "VERTEILER_PORT", 2987);
        dataDirectory = getString("server.data_directory", "VERTEILER_DATA_DIRECTORY", "./data");

        logLevel = getString("logging.level", "VERTEILER_LOG_LEVEL", "INFO");
        logAccess = getBoolean("logging.log_access", "VERTEILER_LOG_ACCESS", true);
        logToFile = getBoolean("logging.log_to_file", "VERTEILER_LOG_TO_FILE", false);
        logDirectory = getString("logging.log_directory", "VERTEILER_LOG_DIRECTORY", "./logs");

        adminEnabled = getBoolean("admin.enabled", "VERTEILER_ADMIN_ENABLED", true);
        adminToken = getString("admin.token", "VERTEILER_ADMIN_TOKEN", generateToken());

        chunkedUploadsEnabled = getBoolean("admin.chunked_uploads.enabled", "VERTEILER_CHUNKED_UPLOADS_ENABLED", true);
        chunkSize = getLong("admin.chunked_uploads.chunk_size", "VERTEILER_CHUNK_SIZE", 5 * 1024 * 1024);
        uploadSessionTtl = getLong("admin.chunked_uploads.session_ttl", "VERTEILER_SESSION_TTL", 15 * 60 * 1000);

        title = getString("ui.title", "VERTEILER_TITLE", "Verteiler");
        showFileSizes = getBoolean("ui.show_file_sizes", "VERTEILER_SHOW_FILE_SIZES", true);
        showDates = getBoolean("ui.show_dates", "VERTEILER_SHOW_DATES", true);
        dragDropUpload = getBoolean("ui.drag_drop_upload", "VERTEILER_DRAG_DROP_UPLOAD", true);
        enablePreview = getBoolean("ui.enable_preview", "VERTEILER_ENABLE_PREVIEW", true);

        footerEnabled = getBoolean("ui.footer.enabled", "VERTEILER_FOOTER_ENABLED", true);
        showCredits = getBoolean("ui.footer.show_credits", "VERTEILER_SHOW_CREDITS", true);

        useCustomResources = getBoolean("ui.resources.use_custom_resources", "VERTEILER_USE_CUSTOM_RESOURCES", false);
        customResourcesDirectory = getString("ui.resources.custom_resources_directory", "VERTEILER_CUSTOM_RESOURCES_DIRECTORY", "./web");

        allowDirectoryListing = getBoolean("security.allow_directory_listing", "VERTEILER_ALLOW_DIRECTORY_LISTING", true);
        String extensions = getString("security.allowed_extensions", "VERTEILER_ALLOWED_EXTENSIONS", "");
        this.allowedExtensions = extensions.isEmpty() ? new String[0] : extensions.split(",");

        enableCaching = getBoolean("performance.enable_caching", "VERTEILER_ENABLE_CACHING", true);
        cacheMaxAge = getInt("performance.cache_max_age", "VERTEILER_CACHE_MAX_AGE", 3600);
        minifyFiles = getBoolean("performance.minify_files", "VERTEILER_MINIFY_FILES", true);
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

    private String getString(String key, String envVar, String defaultValue) {
        String envValue = System.getenv(envVar);
        if (envValue != null) return envValue;
        String tomlValue = toml.getString(key);
        return tomlValue != null ? tomlValue : defaultValue;
    }

    private long getLong(String key, String envVar, long defaultValue) {
        String envValue = System.getenv(envVar);
        if (envValue != null) try {
            return Long.parseLong(envValue);
        } catch (NumberFormatException ignored) { }
        Long tomlValue = toml.getLong(key);
        return tomlValue == null ? defaultValue : tomlValue;
    }

    private int getInt(String key, String envVar, int defaultValue) {
        return (int) getLong(key, envVar, defaultValue);
    }

    private boolean getBoolean(String key, String envVar, boolean defaultValue) {
        String envValue = System.getenv(envVar);
        if (envValue != null) return Boolean.parseBoolean(envValue);
        Boolean tomlValue = toml.getBoolean(key);
        if (tomlValue != null) return tomlValue;
        return defaultValue;
    }

    public static Configuration getInstance() {
        return instance;
    }

    public Path getDataPath() {
        return Paths.get(dataDirectory);
    }
}
