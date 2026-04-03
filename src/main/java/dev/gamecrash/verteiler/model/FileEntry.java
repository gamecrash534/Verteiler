package dev.gamecrash.verteiler.model;

import dev.gamecrash.verteiler.http.WebUI;

import java.time.Instant;

public record FileEntry(String name, String path, boolean isDirectory, long size, Instant lastModified, String mimeType) {
    public String getReadableSize() {
        if (isDirectory) return "-";
        return WebUI.getReadableSize(size);
    }
}
