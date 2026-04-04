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
import org.jetbrains.annotations.Nullable;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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

            if (!isExtensionAllowed(file.filename())) {
                WebServer.jsonRes(ctx, 400, false, "file extension forbidden");
                continue;
            }

            fileStorage.save(filePath, file.content());
            uploaded++;
        }

        WebServer.jsonRes(ctx, 200, true, "uploaded " + uploaded + " file(s)");
    }

    public void startChunkedUpload(Context ctx) throws IOException {
        clearStaleSessions();

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

        if (!isExtensionAllowed(filename)) {
            WebServer.jsonRes(ctx, 400, false, "file extension forbidden");
            return;
        }

        Long totalSize = parseLong(ctx.formParam("totalSize"));
        Integer totalChunks = parseInt(ctx.formParam("totalChunks"));
        if (totalSize == null || totalChunks == null) {
            WebServer.jsonRes(ctx, 400, false, "missing 'totalSize' or 'totalChunks' parameters");
            return;
        }

        int id = chunkedUploadId.getAndIncrement();
        Path uploadDir = config.tempUploadDirectory.resolve(String.valueOf(id));

        Files.createDirectories(uploadDir);
        chunkedUploadSessions.put(id, new ChunkedUploadSession(targetPath, filename, totalSize, totalChunks, uploadDir));

        ctx.status(200).contentType("application/json").result(Json.object("success", true, "message", "chunked upload ready",
            "id", id, "chunkSize", config.chunkSize, "totalChunks", totalChunks
        ));
    }

    public void chunkedUpload(Context ctx) throws IOException {
        Integer id = parseInt(ctx.formParam("id"));
        Integer chunkIdx = parseInt(ctx.formParam("chunkIndex"));
        if (id == null || chunkIdx == null) {
            WebServer.jsonRes(ctx, 400, false, "missing 'id' or 'chunkIndex' parameters");
            return;
        }

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

        synchronized (session) {
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
                return;
            }

            if (isLastChunk && actualSize != expectedRemaining) {
                Files.deleteIfExists(chunkPath);
                WebServer.jsonRes(ctx, 422, false, "expected final chunk size mismatch");
                return;
            }

            session.receivedBytes += actualSize;
            session.nextChunkIdx++;
            session.lastChange = System.currentTimeMillis();
        }

        WebServer.jsonRes(ctx, 200, true, "chunk received");
    }

    public void endChunkedUpload(Context ctx) throws IOException {
        Integer id = parseInt(ctx.formParam("id"));
        if (id == null) {
            WebServer.jsonRes(ctx, 400, false, "Missing 'id' parameter");
            return;
        }

        ChunkedUploadSession session = chunkedUploadSessions.get(id);
        if (session == null) {
            WebServer.jsonRes(ctx, 404, false, "upload session under given id not found");
            return;
        }

        String filePath = session.targetPath.isEmpty() ? session.filename : session.targetPath + "/" + session.filename;
        synchronized (session) {
            if (session.nextChunkIdx != session.totalChunks) {
                WebServer.jsonRes(ctx, 409, false, "upload incomplete, " +
                    "received only " + session.nextChunkIdx + " of " + session.totalChunks + " expected chunks"
                );
                return;
            }

            Path merged = session.uploadDir.resolve("merged");
            try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(merged))) {
                for (int i = 0; i < session.totalChunks; i++) {
                    Path chunk = session.uploadDir.resolve(String.valueOf(i));
                    Files.copy(chunk, out);
                }
            }

            fileStorage.copy(filePath, merged);

            if (session.isStale(config.uploadSessionTtl)) {
                try {
                    session.removeTempData();
                    chunkedUploadSessions.remove(id);
                } catch (IOException e) {
                    logger.warn("could not remove temp directory {}", session.uploadDir.toString());
                }
            }
        }

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

    private boolean isExtensionAllowed(String filename) {
        if (config.allowedExtensions.length == 0) return true;
        String ext = getExtension(filename);

        for (String allowedExt : config.allowedExtensions) {
            if (allowedExt.trim().equalsIgnoreCase(ext)) return true;
        }

        logger.warn("rejected upload of {}; extension not allowed", filename);
        return false;
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot + 1) : "";
    }

    @Nullable
    private Integer parseInt(String value) {
        if (value == null) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    private Long parseLong(String value) {
        if (value == null) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void clearStaleSessions() throws IOException {
        for (Map.Entry<Integer, ChunkedUploadSession> entry : chunkedUploadSessions.entrySet()) {
            ChunkedUploadSession session = entry.getValue();
            synchronized (session) {
                if (!session.isStale(config.uploadSessionTtl)) return;
                session.removeTempData();
                chunkedUploadSessions.remove(entry.getKey());
            }
        }
    }
}
