package dev.gamecrash.verteiler.storage;

import java.time.Instant;

public record FileEntry(String name, String path, boolean isDirectory, long size, Instant lastModified, String mimeType) {
    public String getReadableSize() {
        if (isDirectory) return "-";
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KiB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MiB", size / (1024.0 * 1024));
        else return String.format("%.1f GiB", size / (1024.0 * 1024 * 1024));
    }
}
