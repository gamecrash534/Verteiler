package dev.gamecrash.verteiler.http;

import dev.gamecrash.verteiler.config.Configuration;
import dev.gamecrash.verteiler.logging.Logger;
import dev.gamecrash.verteiler.storage.FileStorage;
import io.javalin.Javalin;

public class WebServer {
    private static final Logger logger = Logger.getInstance();

    private final Configuration config;
    private final FileStorage fileStorage;
    private Javalin server;

    public WebServer(Configuration config, FileStorage fileStorage) {
        this.config = config;
        this.fileStorage = fileStorage;
    }

    public void startServer() {
        server = Javalin.create(javalinConfig -> {
            if (config.logAccess)
                javalinConfig.requestLogger.http((ctx, ms) -> logger.info("{} {} {} - {}ms", ctx.method(), ctx.path(),
                    ctx.status(), String.format("%.2f", ms)));
        });

        server.start(config.host, config.port);
        logger.info("Web Server startet on http://{}:{}", config.host, config.port);
    }

    public void stop() {
        server.stop();
        logger.info("Web Server stopped");
    }
}
