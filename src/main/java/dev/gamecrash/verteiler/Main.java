package dev.gamecrash.verteiler;

import dev.gamecrash.verteiler.config.Configuration;
import dev.gamecrash.verteiler.logging.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    private static Logger logger;
    private static Configuration config;

    public static void main(String[] args) {
        logger = Logger.getInstance();
        logger.info("Starting Verteiler...");

        Path configPath = Paths.get("config.toml");
        for (int i = 0; i < args.length; i++) {
            if ((args[i].equals("-c") || args[i].equals("--config")) && i + 1 < args.length) {
                configPath = Paths.get(args[i + 1]);
                break;
            }
        }

        logger.info("Config: {}", configPath.toAbsolutePath());

        config = Configuration.load(configPath);
        logger.loadConfig();
        logger.info("Loaded config. Data directory: {}", config.getDataPath().toAbsolutePath());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down...");
            logger.flushQueue();
        }));
    }
}
