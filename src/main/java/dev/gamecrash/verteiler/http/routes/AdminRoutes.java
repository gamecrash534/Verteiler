package dev.gamecrash.verteiler.http.routes;

import dev.gamecrash.verteiler.config.Configuration;
import dev.gamecrash.verteiler.http.WebServer;
import dev.gamecrash.verteiler.http.WebUI;
import dev.gamecrash.verteiler.logging.Logger;
import dev.gamecrash.verteiler.model.FileEntry;
import dev.gamecrash.verteiler.storage.FileStorage;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class AdminRoutes {
    private static final Logger logger = Logger.getInstance();
    private final FileStorage fileStorage;
    private final Configuration config;

    public AdminRoutes(FileStorage fileStorage, Configuration config) {
        this.fileStorage = fileStorage;
        this.config = config;
    }

    public void authenticate(Context ctx) {
        String token = ctx.cookie("admin_token");
        if (Set.of("/admin", "/admin/").contains(ctx.path().split("\\?")[0]) && token == null) token = ctx.queryParam("token");
        System.out.println(token);

        if (token == null || !token.equals(config.adminToken)) {
            ctx.status(401)
                .html(WebUI.loginPage(config))
                .skipRemainingHandlers()
                .removeCookie("admin_token");
            return;
        }

        ctx.cookie("admin_token", token, 604800 * 7);
    }

    public void dashboard(Context ctx) throws IOException {
        long totalSize = fileStorage.getDirectorySize("");

        ctx.html(WebUI.dashboard(config, totalSize, fileStorage.fileCount(""), fileStorage.directoryCount("") - 1));
    }

    public void browse(Context ctx) throws IOException {
        String path = getPathFromContext(ctx, "/admin/browse");

        if (!fileStorage.exists(path)) {
            ctx.status(404).html(WebUI.error404(config, path, true));
            return;
        }

        if (!fileStorage.isDirectory(path)) {
            ctx.redirect("/download/" + path);
            return;
        }

        List<FileEntry> entries = fileStorage.list(path);

        ctx.html(WebUI.adminBrowse(config, path, entries));
    }

    public void upload(Context ctx) throws IOException {
        String targetPath = ctx.formParam("path");
        if (targetPath == null) targetPath = "";

        List<UploadedFile> files = ctx.uploadedFiles("files");
        if (files.isEmpty()) {
            WebServer.jsonRes(ctx, 400, false, "no files uploaded");
            return;
        }

        int uploaded = 0;
        for (UploadedFile file : files) {
            String filePath = targetPath.isEmpty() ? file.filename() : targetPath + "/" + file.filename();

            if (config.allowedExtensions.length > 0) {
                String ext = getExtension(file.filename());
                boolean allowed = false;
                for (String allowedExt : config.allowedExtensions) {
                    if (allowedExt.trim().equalsIgnoreCase(ext)) {
                        allowed = true;
                        break;
                    }
                }
                if (!allowed) {
                    logger.warn("rejected upload of {}; extension not allowed", file.filename());
                    continue;
                }
            }

            fileStorage.save(filePath, file.content());
            uploaded++;
        }

        WebServer.jsonRes(ctx, 200, true, "uploaded " + uploaded + " file(s)");
    }

    public void mkdir(Context ctx) throws IOException {
        String path = ctx.formParam("path");
        if (path == null || path.isEmpty()) {
            WebServer.jsonRes(ctx, 400, false, "path required");
            return;
        }

        fileStorage.mkdir(path);
        WebServer.jsonRes(ctx, 200,true, "created directory");
    }

    public void delete(Context ctx) throws IOException {
        String path = ctx.formParam("path");
        if (path == null || path.isEmpty()) {
            WebServer.jsonRes(ctx, 400, false, "path required");
            return;
        }

        boolean deleted = fileStorage.delete(path);
        if (deleted) WebServer.jsonRes(ctx, 200,true, "deleted successfully");
        else WebServer.jsonRes(ctx, 404, false, "file not found");
    }

    public void move(Context ctx) throws IOException {
        String from = ctx.formParam("from");
        String to = ctx.formParam("to");

        if (from == null || from.isEmpty() || to == null || to.isEmpty()) {
            WebServer.jsonRes(ctx, 400, false, "from and to paths required");
            return;
        }

        fileStorage.move(from, to);
        WebServer.jsonRes(ctx, 200, true, "moved successfully");
    }

    private String getPathFromContext(Context ctx, String prefix) {
        String path = ctx.path();
        if (path.startsWith(prefix)) path = path.substring(prefix.length());
        if (path.startsWith("/")) path = path.substring(1);
        path = path.replaceAll("%20", " ");

        return path.isEmpty() ? "" : path;
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot + 1) : "";
    }
}
