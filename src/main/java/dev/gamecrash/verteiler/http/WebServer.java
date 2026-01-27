package dev.gamecrash.verteiler.http;

import dev.gamecrash.verteiler.config.Configuration;
import dev.gamecrash.verteiler.http.routes.AdminRoutes;
import dev.gamecrash.verteiler.http.routes.ApiRoutes;
import dev.gamecrash.verteiler.http.routes.FileRoutes;
import dev.gamecrash.verteiler.logging.Logger;
import dev.gamecrash.verteiler.storage.FileStorage;
import dev.gamecrash.verteiler.util.Json;
import io.javalin.Javalin;
import io.javalin.http.Context;

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
        AdminRoutes adminRoutes = new AdminRoutes(fileStorage, config);
        ApiRoutes apiRoutes = new ApiRoutes(fileStorage, config);

        server.get("/assets/css/style.css", ctx -> ctx.contentType("text/css").result(WebUI.getCSS()));
        server.get("/assets/js/app.js", ctx -> ctx.contentType("text/javascript").result(WebUI.getJS()));

        server.get("/", fileRoutes::index);
        server.get("/browse", fileRoutes::browse);
        server.get("/browse/*", fileRoutes::browse);
        server.get("/download/*", fileRoutes::download);
        server.get("/raw/*", fileRoutes::raw);
        server.get("/preview/*", fileRoutes::preview);

        if (config.adminEnabled) {
            // TODO: authentication stuff
            server.before("/admin", adminRoutes::authenticate);
            server.before("/admin/*", adminRoutes::authenticate);
            server.before("/api/admin/*", adminRoutes::authenticate);

            server.get("/admin", adminRoutes::dashboard);
            server.get("/admin/browse", adminRoutes::browse);
            server.get("/admin/browse/*", adminRoutes::browse);

            server.post("/api/admin/upload", adminRoutes::upload);
            server.post("/api/admin/mkdir", adminRoutes::mkdir);
            server.post("/api/admin/delete", adminRoutes::delete);
            server.post("/api/admin/move", adminRoutes::move);
        }

        server.get("/api/list", apiRoutes::list);
        server.get("/api/list/*", apiRoutes::list);
        server.get("/api/info/*", apiRoutes::info);
    }

    public static void jsonRes(Context ctx, int status, boolean success, String message) {
        ctx.status(status).contentType("application/json").result(Json.object("success", success, "message", message));
    }
}
