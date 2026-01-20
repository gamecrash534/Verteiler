package dev.gamecrash.verteiler.http.routes;

import dev.gamecrash.verteiler.config.Configuration;
import dev.gamecrash.verteiler.http.WebServer;
import dev.gamecrash.verteiler.http.WebUI;
import dev.gamecrash.verteiler.logging.Logger;
import dev.gamecrash.verteiler.storage.FileStorage;
import io.javalin.http.Context;

import java.io.IOException;

public class AdminRoutes {
    private static final Logger logger = Logger.getInstance();
    private final FileStorage fileStorage;
    private final Configuration config;

    public AdminRoutes(FileStorage fileStorage, Configuration config) {
        this.fileStorage = fileStorage;
        this.config = config;
    }

    public void authenticate(Context ctx) {
        String token = ctx.queryParam("token");
        token = token == null ? ctx.cookie("admin_token") : token;

        if (token == null || !token.equals(config.adminToken)) {
            ctx.status(401);
            if (ctx.path().startsWith("/api")) WebServer.jsonRes(ctx, 401, false, "Unauthorized");
            else ctx.html(WebUI.loginPage(config));
            ctx.skipRemainingHandlers();
            return;
        }

        ctx.cookie("admin_token", token, 604800 * 7);
    }

    public void dashboard(Context ctx) throws IOException {
        long totalSize = fileStorage.getDirectorySize("");

        ctx.html(WebUI.dashboard(config, totalSize, fileStorage.fileCount(""), fileStorage.directoryCount("") - 1));
    }
}
