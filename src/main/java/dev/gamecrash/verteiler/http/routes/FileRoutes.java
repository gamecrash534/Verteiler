package dev.gamecrash.verteiler.http.routes;

import dev.gamecrash.verteiler.config.Configuration;
import dev.gamecrash.verteiler.http.WebUI;
import dev.gamecrash.verteiler.storage.FileEntry;
import dev.gamecrash.verteiler.storage.FileStorage;
import io.javalin.http.Context;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FileRoutes {
    private final FileStorage fileStorage;
    private final Configuration config;

    public FileRoutes(FileStorage fileStorage, Configuration config) {
        this.fileStorage = fileStorage;
        this.config = config;
    }

    public void index(Context ctx) {
        ctx.redirect("/browse");
    }

    public void browse(Context ctx) throws IOException {
        if (!config.allowDirectoryListing) {
            ctx.status(403).result("Directory listing is disabled");
            return;
        }

        String path = getPathFromContext(ctx, "/browse");
        if (!fileStorage.exists(path)) {
            ctx.status(404).result("path not found");
            return;
        }

        if (!fileStorage.isDirectory(path)) {
            ctx.redirect("/download/" + path);
            return;
        }

        List<FileEntry> entries = fileStorage.list(path);
        ctx.html(WebUI.browseDirectory(config, path, entries));
    }

    private String getPathFromContext(Context ctx, String prefix) {
        String path = ctx.path();
        if (path.startsWith(prefix)) path = path.substring(prefix.length());
        if (path.startsWith("/")) path = path.substring(1);

        return path;
    }
}
