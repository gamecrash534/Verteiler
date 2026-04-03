package dev.gamecrash.verteiler.http.routes;

import dev.gamecrash.verteiler.config.Configuration;
import dev.gamecrash.verteiler.http.WebServer;
import dev.gamecrash.verteiler.http.WebUI;
import dev.gamecrash.verteiler.logging.Logger;
import dev.gamecrash.verteiler.model.ChunkedUploadSession;
import dev.gamecrash.verteiler.model.FileEntry;
import dev.gamecrash.verteiler.storage.FileStorage;
import dev.gamecrash.verteiler.util.Json;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AdminRoutes {
    private static final Logger logger = Logger.getInstance();
    private final FileStorage fileStorage;
    private final Configuration config;

    private final AtomicInteger chunkedUploadId = new AtomicInteger();
    private final Map<Integer, ChunkedUploadSession> chunkedUploadSessions = new ConcurrentHashMap<>();

    public AdminRoutes(FileStorage fileStorage, Configuration config) {
        this.fileStorage = fileStorage;
        this.config = config;
    }

    public void authenticate(Context ctx) {
        String token = ctx.cookie("admin_token");
        if (Set.of("/admin", "/admin/").contains(ctx.path().split("\\?")[0]) && token == null) token = ctx.queryParam("token");

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

    public void startChunkedUpload(Context ctx) throws IOException {
        if (!config.chunkedUploadsEnabled) {
            WebServer.jsonRes(ctx, 400, false, "chunked uploads not enabled");
            return;
        }

        String targetPath = ctx.formParam("path");
        if (targetPath == null) targetPath = "";

        String filename = ctx.formParam("filename");
        if (filename == null) {
            WebServer.jsonRes(ctx, 400, false, "filename required");
            return;
        }

        if (config.allowedExtensions.length > 0) {
            String ext = getExtension(filename);
            boolean allowed = false;
            for (String allowedExt : config.allowedExtensions) {
                if (allowedExt.trim().equalsIgnoreCase(ext)) {
                    allowed = true;
                    break;
                }
            }

            if (!allowed) {
                logger.warn("rejected upload of {}; extension not allowed", filename);
                return;
            }
        }

        long totalSize = Long.parseLong(ctx.formParam("totalSize"));
        int totalChunks = Integer.parseInt(ctx.formParam("totalChunks"));

        int id = chunkedUploadId.getAndIncrement();
        Path uploadDir = config.tempUploadDirectory.resolve(String.valueOf(id));

        Files.createDirectories(uploadDir);
        chunkedUploadSessions.put(id, new ChunkedUploadSession(targetPath, filename, totalSize, totalChunks, uploadDir));

        ctx.status(200).contentType("application/json").result(Json.object("success", true, "message", "chunked upload ready",
            "id", id, "chunkSize", config.chunkSize, "totalChunks", totalChunks
        ));
    }

    public void chunkedUpload(Context ctx) throws IOException {
        int id = Integer.valueOf(ctx.formParam("id"));
        int chunkIdx = Integer.valueOf(ctx.formParam("chunkIndex"));

        ChunkedUploadSession session = chunkedUploadSessions.get(id);
        if (session == null) {
            WebServer.jsonRes(ctx, 404, false, "upload session under given id not found");
            return;
        }

        UploadedFile chunk = ctx.uploadedFile("chunk");
        if (chunk == null) {
            WebServer.jsonRes(ctx, 400, false, "chunk may not be null");
            return;
        }

        if (chunkIdx != session.nextChunkIdx) {
            WebServer.jsonRes(ctx, 400, false, "unexpected chunk index, should've been " + session.nextChunkIdx);
            return;
        }

        long expectedRemaining = session.totalSize - session.receivedBytes;
        long maxAllowed = Math.clamp(expectedRemaining, 0, config.chunkSize);

        Path chunkPath = session.uploadDir.resolve(String.valueOf(chunkIdx));

        try (InputStream input = chunk.content()) {
            Files.copy(input, chunkPath, StandardCopyOption.REPLACE_EXISTING);
        }

        long actualSize = Files.size(chunkPath);
        boolean isLastChunk = chunkIdx == session.totalChunks - 1;

        if (actualSize > maxAllowed) {
            Files.deleteIfExists(chunkPath);
            WebServer.jsonRes(ctx, 413, false, "chunk too large; max: " + maxAllowed);
            return;
        }

        if (!isLastChunk && actualSize != config.chunkSize) {
            Files.deleteIfExists(chunkPath);
            WebServer.jsonRes(ctx, 422, false, "chunk too large");
        }

        if (!isLastChunk && actualSize != expectedRemaining) {
            Files.deleteIfExists(chunkPath);
            WebServer.jsonRes(ctx, 422, false, "expected final chunk size mismatch");
        }

        session.receivedBytes += actualSize;
        session.nextChunkIdx++;

        WebServer.jsonRes(ctx, 200, true, "chunk received");
    }

    public void endChunkedUpload(Context ctx) throws IOException {
        int id = Integer.parseInt(ctx.formParam("id"));

        ChunkedUploadSession session = chunkedUploadSessions.get(id);
        if (session == null) {
            WebServer.jsonRes(ctx, 404, false, "upload session under given id not found");
            return;
        }

        if (session.nextChunkIdx != session.totalChunks) {
            WebServer.jsonRes(ctx, 409, false, "upload incomplete, " +
                "received only " + session.nextChunkIdx + " of " + session.totalChunks + " expected chunks"
            );
            return;
        }

        String filePath = session.targetPath.isEmpty() ? session.filename : session.targetPath + "/" + session.filename;
        Path merged = session.uploadDir.resolve("merged");
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(merged))) {
            for (int i = 0; i < session.totalChunks; i++) {
                Path chunk = session.uploadDir.resolve(String.valueOf(i));
                Files.copy(chunk, out);
            }
        }

        fileStorage.copy(filePath, merged);

        WebServer.jsonRes(ctx, 200, true, "uploaded 1 file");
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
