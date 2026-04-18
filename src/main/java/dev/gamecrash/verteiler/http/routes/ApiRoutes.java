package dev.gamecrash.verteiler.http.routes;

import dev.gamecrash.verteiler.Main;
import dev.gamecrash.verteiler.config.Configuration;
import dev.gamecrash.verteiler.http.WebServer;
import dev.gamecrash.verteiler.model.FileEntry;
import dev.gamecrash.verteiler.storage.FileStorage;
import dev.gamecrash.verteiler.storage.MimeTypes;
import dev.gamecrash.verteiler.util.Json;
import io.javalin.http.Context;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ApiRoutes {
    private final FileStorage fileStorage;
    private final Configuration config;

    public ApiRoutes(FileStorage fileStorage, Configuration config) {
        this.fileStorage = fileStorage;
        this.config = config;
    }

    public void health(Context ctx) {
        ctx.status(200).contentType("application/json")
            .result("{\"success\":true,\"timestamp\":" + System.currentTimeMillis() + "}");
    }

    public void list(Context ctx) throws IOException {
        if (!config.allowDirectoryListing) {
            WebServer.jsonRes(ctx, 403, false, "directory listing is disabled");
            return;
        }

        String path = getPathFromContext(ctx, "/api/list");

        if (!fileStorage.exists(path)) {
            WebServer.jsonRes(ctx, 404, false, "path not found");
            return;
        }

        if (!fileStorage.isDirectory(path)) {
            WebServer.jsonRes(ctx, 400, false, "not a directory");
            return;
        }

        List<FileEntry> entries = fileStorage.list(path);
        if (entries.isEmpty()) {
            ctx.status(404).contentType("application/json")
                .result(Json.object("success", true, "data", "empty"));
            return;
        }
        ctx.contentType("application/json")
            .result(Json.object("success", true, "data", Json.object("path", path, "entries", entries)));
    }

    public void info(Context ctx) throws IOException {
        String path = getPathFromContext(ctx, "/api/info");

        FileEntry entry = fileStorage.get(path);
        if (entry == null) {
            WebServer.jsonRes(ctx, 404, false, "path not found");
            return;
        }

        ctx.contentType("application/json")
            .result(Json.object("success", true, "data", entry));
    }

    private String getPathFromContext(Context ctx, String prefix) {
        String path = ctx.path();
        if (path.startsWith(prefix)) path = path.substring(prefix.length());
        if (path.startsWith("/")) path = path.substring(1);
        path = path.replaceAll("%20", " ");

        return path.isEmpty() ? "/" : path;
    }
}
