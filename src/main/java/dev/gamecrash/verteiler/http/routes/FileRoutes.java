package dev.gamecrash.verteiler.http.routes;

import dev.gamecrash.verteiler.config.Configuration;
import dev.gamecrash.verteiler.http.WebServer;
import dev.gamecrash.verteiler.http.WebUI;
import dev.gamecrash.verteiler.storage.FileEntry;
import dev.gamecrash.verteiler.storage.FileStorage;
import io.javalin.http.Context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
            ctx.status(403).html(WebUI.error403(config, "Directory listing is disabled", isAdmin(ctx)));
            return;
        }

        String path = getPathFromContext(ctx, "/browse");
        if (!fileStorage.exists(path)) {
            ctx.status(404).html(WebUI.error404(config, "Not Found: " + path, isAdmin(ctx)));
            return;
        }

        if (!fileStorage.isDirectory(path)) {
            ctx.redirect("/download/" + path);
            return;
        }

        List<FileEntry> entries = fileStorage.list(path);

        ctx.html(WebUI.browseDirectory(config, path, entries, isAdmin(ctx)));
    }

    public void download(Context ctx) throws IOException {
        String path = getPathFromContext(ctx, "/download");
        if (!fileStorage.exists(path)) {
            WebServer.jsonRes(ctx, 404, false, "path not found");
            return;
        }

        if (fileStorage.isDirectory(path)) {
            WebServer.jsonRes(ctx, 400, false, "downloading a directory would be fun, right?");
            return;
        }

        serveFile(ctx, path, true);
    }

    public void ram(Context ctx) throws IOException {
        String path = getPathFromContext(ctx, "/raw");

        if (!fileStorage.exists(path)) {
            WebServer.jsonRes(ctx, 404, false, "path not found");
            return;
        }

        if (fileStorage.isDirectory(path)) {
            WebServer.jsonRes(ctx, 400, false, "downloading a directory would be fun, right?");
            return;
        }

        serveFile(ctx, path, false);
    }

    public void preview(Context ctx) throws IOException {
        String path = getPathFromContext(ctx, "/preview");
        boolean isAdmin = isAdmin(ctx);
        if (!config.enablePreview) {
            ctx.redirect("/download/" + path);
            return;
        }

        if (!fileStorage.exists(path)) {
            ctx.status(404).html(WebUI.error404(config, "Not Found: " + path, isAdmin));
            return;
        }

        if (fileStorage.isDirectory(path)) {
            ctx.redirect("/browse/" + path);
            return;
        }

        FileEntry entry = fileStorage.get(path);
        if (entry == null) {
            ctx.status(404).html(WebUI.error404(config, "Not Found: " +  path, isAdmin));
            return;
        }

        ctx.html(WebUI.previewFile(config, entry, path, isAdmin));
    }

    private void serveFile(Context ctx, String path, boolean asDownload) throws IOException {
        FileEntry entry = fileStorage.get(path);
        if (entry == null) {
            ctx.status(404).result("File not found");
        }

        Path filePath = fileStorage.getAbsolutePath(path);

        ctx.contentType(entry.mimeType());
        ctx.header("Content-Length", String.valueOf(entry.size()));

        if (config.enableCaching) ctx.header("Cache-Control", "public, max-age=" + config.cacheMaxAge);
        if (asDownload) ctx.header("Content-Disposition", "attachment; filename=\"" + entry.name() + "\"");
        else ctx.header("Content-Disposition", "inline; filename=\"" + entry.name() + "\"");

        String rangeHeader = ctx.header("Range");
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) handleRangeRequest(ctx, filePath, entry.size(), rangeHeader);
        else ctx.result(Files.newInputStream(filePath));
    }

    private void handleRangeRequest(Context ctx, Path filePath, long fileSize, String rangeHeader) throws IOException {
        String range = rangeHeader.substring(6);
        String[] parts = range.split("-");

        long start = parts[0].isEmpty() ? fileSize - Long.parseLong(parts[1]) : Long.parseLong(parts[0]);
        long end = parts.length > 1 && !parts[1].isEmpty() ? Long.parseLong(parts[1]) : fileSize - 1;

        if (start > end || start < 0 || end >= fileSize) {
            ctx.status(416).header("Content-Range", "bytes */" + fileSize);
            return;
        }

        long length = end - start + 1;
        ctx.status(206);
        ctx.header("Content-Range", "bytes " + start + "-" + end + "/" + fileSize);
        ctx.header("Content-Length", String.valueOf(length));
        ctx.header("Accept-Ranges", "bytes");

        byte[] allBytes = Files.readAllBytes(filePath);
        byte[] rangeBytes = new byte[(int) length];
        System.arraycopy(allBytes, (int) start, rangeBytes, 0, (int) length);
        ctx.result(rangeBytes);
    }

    private boolean isAdmin(Context ctx) {
        String cookie = ctx.cookie("admin_token");
        return cookie != null && cookie.equals(config.adminToken);
    }

    private String getPathFromContext(Context ctx, String prefix) {
        String path = ctx.path();
        if (path.startsWith(prefix)) path = path.substring(prefix.length());
        if (path.startsWith("/")) path = path.substring(1);

        return path;
    }
}
