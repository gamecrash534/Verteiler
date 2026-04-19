package dev.gamecrash.verteiler.http.routes;

import dev.gamecrash.verteiler.config.Configuration;
import dev.gamecrash.verteiler.http.WebServer;
import dev.gamecrash.verteiler.http.WebUI;
import dev.gamecrash.verteiler.logging.Logger;
import dev.gamecrash.verteiler.model.FileEntry;
import dev.gamecrash.verteiler.storage.FileStorage;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FileRoutes {
    private final FileStorage fileStorage;
    private final Configuration config;
    private final Logger logger = Logger.getInstance();

    public FileRoutes(FileStorage fileStorage, Configuration config) {
        this.fileStorage = fileStorage;
        this.config = config;
    }

    public void index(Context ctx) {
        ctx.redirect("/browse", HttpStatus.MOVED_PERMANENTLY);
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

    public void raw(Context ctx) throws IOException {
        String path = ctx.path().startsWith("/raw") ? getPathFromContext(ctx, "/raw") : getPathFromContext(ctx, "/");

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
            ctx.status(404).html(WebUI.error404(config, "Not Found: " + path, isAdmin));
            return;
        }

        ctx.html(WebUI.previewFile(config, entry, path, isAdmin));
    }

    private void serveFile(Context ctx, String path, boolean asDownload) throws IOException {
        FileEntry entry = fileStorage.get(path);
        if (entry == null) {
            ctx.status(404).result("File not found");
            return;
        }

        logger.info("Serving {}", path);

        Path filePath = fileStorage.getAbsolutePath(path);
        var response = ctx.res();

        response.setContentType(entry.mimeType());
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Cache-Control", config.enableCaching ? "public, s-maxage=" + config.cacheMaxAge + ", max-age=0, must-revalidate" : "no-store");
        response.setHeader("Content-Disposition", (asDownload ? "attachment" : "inline") + "; filename=\"" + entry.name() + "\"");

        String rangeHeader = ctx.header("Range");
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) handleRangeRequest(response, filePath, entry.size(), rangeHeader);
        else {
            response.setContentLengthLong(entry.size());
            Files.copy(filePath, response.getOutputStream());
        }
    }

    private void handleRangeRequest(HttpServletResponse response, Path filePath, long fileSize, String rangeHeader) throws IOException {
        String range = rangeHeader.substring(6);
        String[] parts = range.split("-");

        long start = parts[0].isEmpty() ? fileSize - Long.parseLong(parts[1]) : Long.parseLong(parts[0]);
        long end = parts.length > 1 && !parts[1].isEmpty() ? Long.parseLong(parts[1]) : fileSize - 1;

        if (start > end || start < 0 || end >= fileSize) {
            response.setStatus(416);
            response.setHeader("Content-Range", "bytes */" + fileSize);
            return;
        }

        long length = end - start + 1;
        response.setStatus(206);
        response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileSize);
        response.setContentLengthLong(length);

        try (var channel = Files.newByteChannel(filePath)) {
            channel.position(start);
            var out = response.getOutputStream();
            var buffer = ByteBuffer.allocate(16384);
            long remaining = length;

            while (remaining > 0) {
                buffer.clear();
                if (remaining < buffer.capacity()) buffer.limit((int) remaining);

                int read = channel.read(buffer);
                if (read <= 0) break;

                out.write(buffer.array(), 0, read);
                remaining -= read;
            }
        }
    }

    private boolean isAdmin(Context ctx) {
        if (!config.adminEnabled) return false;
        String cookie = ctx.cookie("admin_token");
        return cookie != null && cookie.equals(config.adminToken);
    }

    private String getPathFromContext(Context ctx, String prefix) {
        String path = ctx.path();
        if (path.startsWith(prefix)) path = path.substring(prefix.length());
        if (path.startsWith("/")) path = path.substring(1);
        path = path.replaceAll("%20", " ");

        return path;
    }
}
