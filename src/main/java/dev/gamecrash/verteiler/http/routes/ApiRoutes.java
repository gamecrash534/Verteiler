package dev.gamecrash.verteiler.http.routes;

import dev.gamecrash.verteiler.config.Configuration;
import dev.gamecrash.verteiler.http.WebServer;
import dev.gamecrash.verteiler.logging.Logger;
import dev.gamecrash.verteiler.storage.FileEntry;
import dev.gamecrash.verteiler.storage.FileStorage;
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
            ctx.contentType("application/json")
                .result(Json.object("success", true, "data", "empty"));
            return;
        }
        Object[] entryObjects = entries.stream()
                .map(e -> Map.of(
                    "name", e.name(),
                    "path", e.path(),
                    "isDirectory", e.isDirectory(),
                    "size", e.size(),
                    "lastModified", e.lastModified().toString(),
                    "mimeType", e.mimeType() != null ? e.mimeType() : ""
                ))
                .toArray();

        ctx.contentType("application/json")
            .result(Json.object("success", true, "data", Map.of("path", path, "entries", entryObjects)));
    }

    public void info(Context ctx) throws IOException {
        String path = getPathFromContext(ctx, "/api/info");

        FileEntry entry = fileStorage.get(path);
        if (entry == null) {
            WebServer.jsonRes(ctx, 404, false, "path not found");
            return;
        }

        ctx.contentType("application/json")
            .result(Json.object(
                "success", true,
                "data", Map.of(
                    "name", entry.name(),
                    "path", entry.path(),
                    "isDirectory", entry.isDirectory(),
                    "size", entry.size(),
                    "lastModified", entry.lastModified().toString(),
                    "mimeType", entry.mimeType() != null ? entry.mimeType() : ""
                )
            ));
    }

    private String getPathFromContext(Context ctx, String prefix) {
        String path = ctx.path();
        if (path.startsWith(prefix)) path = path.substring(prefix.length());
        if (path.startsWith("/")) path = path.substring(1);
        path = path.replaceAll("%20", " ");

        return path.isEmpty() ? "" : path;
    }
}
