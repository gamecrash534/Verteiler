package dev.gamecrash.verteiler;

import dev.gamecrash.verteiler.config.Configuration;
import dev.gamecrash.verteiler.http.WebServer;
import dev.gamecrash.verteiler.logging.Logger;
import dev.gamecrash.verteiler.storage.FileStorage;
import dev.gamecrash.verteiler.util.Resources;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    private static Logger logger;
    private static Configuration config;
    private static FileStorage fileStorage;

    public static void main(String[] args) {
        logger = Logger.getInstance();
        try {
            logger.info("Starting Verteiler...");

            Path configPath = Paths.get("config/config.toml");
            for (int i = 0; i < args.length; i++) {
                if ((args[i].equals("-c") || args[i].equals("--config")) && i + 1 < args.length) {
                    configPath = Path.of(args[i + 1]);
                    break;
                }
            }

            logger.info("Config: {}", configPath.toAbsolutePath());

            config = Configuration.load(configPath);
            logger.loadConfig();
            logger.info("Loaded config. Data directory: {}", config.getDataPath().toAbsolutePath());

            Path resourcesPath = Path.of(config.customResourcesDirectory);
            if (config.useCustomResources && !Files.exists(resourcesPath)) Resources.saveResourcesToPath(resourcesPath);

            fileStorage = new FileStorage(config.getDataPath());

            WebServer server = new WebServer(config, fileStorage);
            server.startServer();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down...");
                server.stop();
                logger.flushQueue();
            }));
        } catch (Exception e) {
            logger.error("Failed to start Verteiler: ", e);
            logger.info("Shutting down");
            logger.flushQueue();
            System.exit(1);
        }
    }
}
