package dev.gamecrash.verteiler.http;

import dev.gamecrash.verteiler.config.Configuration;
import dev.gamecrash.verteiler.http.routes.FileRoutes;
import dev.gamecrash.verteiler.logging.Logger;
import dev.gamecrash.verteiler.storage.FileStorage;
import dev.gamecrash.verteiler.util.Json;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.io.IOException;

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

        registerRoutes();

        server.exception(Exception.class, (e, ctx) -> {
            logger.error("Exception got caught", e);
            jsonRes(ctx, 500, false, "Internal server error");
        });

        server.start(config.host, config.port);
        logger.info("Web Server startet on http://{}:{}", config.host, config.port);

        if (config.adminEnabled) {
            logger.info("Admin interface can be found under http://{}:{}/{}", config.host, config.port, "admin");
            logger.info("Currently set token is {}", config.adminToken);
        }
    }

    public void stop() {
        server.stop();
        logger.info("Web Server stopped");
    }

    private void registerRoutes() {
        FileRoutes fileRoutes = new FileRoutes(fileStorage, config);

        server.get("/assets/css/style.css", ctx -> ctx.contentType("text/css").result(WebUI.getCSS()));
        server.get("/assets/js/app.js", ctx -> ctx.contentType("text/javascript").result(WebUI.getJS()));

        server.get("/", fileRoutes::index);
        server.get("/browse", fileRoutes::browse);
        server.get("/browse/*", fileRoutes::browse);
        server.get("/download/*", fileRoutes::download);
        server.get("/raw/*", fileRoutes::ram);
        server.get("/preview/*", fileRoutes::preview);
        /*

        if (config.adminEnabled) {
            // TODO: authentication stuff
            server.before("/admin", null);
            server.before("/admin/*", null);
            server.before("/api/admin/*", null);

            server.get("/admin", null);
            server.get("/admin/browse", null);
            server.get("/admin/browse/*", null);

            server.post("/api/admin/upload", null);
            server.post("/api/admin/mkdir", null);
            server.post("/api/admin/delete", null);
            server.post("/api/admin/move", null);
        }

        server.get("/api/list", null);
        server.get("/api/list/*", null);
        server.get("/api/info/*", null);
         */
    }

    public static void jsonRes(Context ctx, int status, boolean success, String message) {
        ctx.status(status).contentType("application/json").result(Json.object("success", success, "message", message));
    }
}
