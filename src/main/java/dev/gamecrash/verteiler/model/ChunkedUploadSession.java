package dev.gamecrash.verteiler.model;

import java.nio.file.Path;

public class ChunkedUploadSession {
    public final String targetPath;
    public final String filename;
    public final long totalSize;
    public final int totalChunks;
    public final Path uploadDir;

    public int nextChunkIdx = 0;
    public long receivedBytes = 0;

    public ChunkedUploadSession(String targetPath, String filename, long totalSize, int totalChunks, Path uploadDir) {
        this.targetPath = targetPath;
        this.filename = filename;
        this.totalSize = totalSize;
        this.totalChunks = totalChunks;
        this.uploadDir = uploadDir;
    }
}
