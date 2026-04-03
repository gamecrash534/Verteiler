package dev.gamecrash.verteiler.http;

import dev.gamecrash.verteiler.config.Configuration;
import dev.gamecrash.verteiler.http.routes.AdminRoutes;
import dev.gamecrash.verteiler.http.routes.ApiRoutes;
import dev.gamecrash.verteiler.http.routes.FileRoutes;
import dev.gamecrash.verteiler.logging.Logger;
import dev.gamecrash.verteiler.storage.FileStorage;
import dev.gamecrash.verteiler.util.Json;
import io.javalin.Javalin;
import io.javalin.apibuilder.ApiBuilder;
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

            javalinConfig.routes.exception(Exception.class, (e, ctx) -> {
                logger.error("Exception got caught", e);
                jsonRes(ctx, 500, false, "Internal server error");
            });
            
            javalinConfig.routes.apiBuilder(this::registerRoutes);
        });

        server.start(config.host, config.port);
        logger.info("Web Server started on http://{}:{}", config.host, config.port);

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

        ApiBuilder.get("/assets/css/*", WebServer::webResource);
        ApiBuilder.get("/assets/js/*",  WebServer::webResource);
        ApiBuilder.get("/favicon.ico",  WebServer::webResource);

        ApiBuilder.get("/", fileRoutes::index);
        ApiBuilder.get("/browse", fileRoutes::browse);
        ApiBuilder.get("/browse/*", fileRoutes::browse);
        ApiBuilder.get("/download/*", fileRoutes::download);
        ApiBuilder.get("/raw/*", fileRoutes::raw);
        ApiBuilder.get("/preview/*", fileRoutes::preview);

        if (config.adminEnabled) {
            ApiBuilder.before("/admin", adminRoutes::authenticate);
            ApiBuilder.before("/admin/*", adminRoutes::authenticate);
            ApiBuilder.before("/api/admin/*", adminRoutes::authenticate);

            ApiBuilder.get("/admin", adminRoutes::dashboard);
            ApiBuilder.get("/admin/browse", adminRoutes::browse);
            ApiBuilder.get("/admin/browse/*", adminRoutes::browse);

            ApiBuilder.post("/api/admin/upload", adminRoutes::upload);
            ApiBuilder.post("/api/admin/upload/chunked/start", adminRoutes::startChunkedUpload);
            ApiBuilder.post("/api/admin/upload/chunked/chunk", adminRoutes::chunkedUpload);
            ApiBuilder.post("/api/admin/upload/chunked/end", adminRoutes::endChunkedUpload);
            ApiBuilder.post("/api/admin/mkdir", adminRoutes::mkdir);
            ApiBuilder.post("/api/admin/delete", adminRoutes::delete);
            ApiBuilder.post("/api/admin/move", adminRoutes::move);
        }

        ApiBuilder.get("/api/list", apiRoutes::list);
        ApiBuilder.get("/api/list/*", apiRoutes::list);
        ApiBuilder.get("/api/info/*", apiRoutes::info);
    }

    public static void jsonRes(Context ctx, int status, boolean success, String message) {
        ctx.status(status).contentType("application/json").result(Json.object("success", success, "message", message));
    }

    private static void webResource(Context ctx) {
        if (ctx.path().startsWith("/assets/css/")) {
            String sheet = ctx.path().replace("/assets/css/", "").replace(".css", "");
            ctx.contentType("text/css").result(WebUI.getCSS(sheet));
        } else if (ctx.path().startsWith("/assets/js")) {
            String script = ctx.path().replace("/assets/js/", "").replace(".js", "");
            ctx.contentType("text/javascript").result(WebUI.getJS(script));
        } else if (ctx.path().startsWith("/favicon")) {
            ctx.contentType("image/svg+xml").result(WebUI.getFavicon());
        }
    }
}
