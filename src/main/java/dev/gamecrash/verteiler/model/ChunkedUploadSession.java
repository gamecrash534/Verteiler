package dev.gamecrash.verteiler.model;

import dev.gamecrash.verteiler.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public class ChunkedUploadSession {
    public final String targetPath;
    public final String filename;
    public final long totalSize;
    public final int totalChunks;
    public final Path uploadDir;

    public int nextChunkIdx = 0;
    public long receivedBytes = 0;
    public long lastChange;

    public ChunkedUploadSession(String targetPath, String filename, long totalSize, int totalChunks, Path uploadDir) {
        this.targetPath = targetPath;
        this.filename = filename;
        this.totalSize = totalSize;
        this.totalChunks = totalChunks;
        this.uploadDir = uploadDir;
        lastChange = System.currentTimeMillis();
    }

    public boolean isStale(long ttlMs) {
        return System.currentTimeMillis() - lastChange > ttlMs;
    }

    public void removeTempData() throws IOException {
        try (Stream<Path> paths = Files.walk(uploadDir)) {
            paths.sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        Logger.getInstance().warn("Could not delete temporary upload file {}", path.toString());
                    }
                });
        }
    }
}
