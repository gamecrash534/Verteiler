package dev.gamecrash.verteiler.storage;

import dev.gamecrash.verteiler.logging.Logger;
import dev.gamecrash.verteiler.model.FileEntry;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class FileStorage {
    private final Logger logger = Logger.getInstance();
    private final Path rootPath;

    public FileStorage(@NotNull Path rootPath) throws IOException {
        this.rootPath = rootPath.toAbsolutePath().normalize();
        if (!Files.exists(rootPath)) {
            Files.createDirectories(rootPath);
            logger.info("Created data directory: {}", rootPath);
        }
    }

    public List<FileEntry> list(String relativePath) throws IOException {
        Path path = resolvePath(relativePath);
        if (!Files.isDirectory(path)) throw new IllegalArgumentException("not a directory");

        List<FileEntry> fileEntries = new ArrayList<>();
        try (Stream<Path> paths = Files.list(path)) {
            paths.forEach(entry -> {
                try {
                    fileEntries.add(toFileEntry(entry));
                } catch (IOException e) {
                    logger.error("Could not read file entry at {}", e, entry);
                }
            });
        }

        fileEntries.sort(Comparator.comparing((FileEntry entry) -> !entry.isDirectory()).thenComparing(FileEntry::name, String.CASE_INSENSITIVE_ORDER));

        return fileEntries;
    }

    public FileEntry get(String relativePath) throws IOException {
        Path path = resolvePath(relativePath);
        if (!Files.exists(path)) return null;

        return toFileEntry(path);
    }

    public FileEntry save(String relativePath, InputStream data) throws IOException {
        Path path = resolvePath(relativePath);
        Path parent = path.getParent();

        if (!Files.exists(parent)) Files.createDirectories(parent);

        Files.copy(data, path, StandardCopyOption.REPLACE_EXISTING);
        logger.info("Saved file {}", path);

        return toFileEntry(path);
    }

    public FileEntry copy(String relativePath, Path existing) throws IOException {
        Path path = resolvePath(relativePath);
        Path parent = path.getParent();

        if (!Files.exists(parent)) Files.createDirectories(parent);

        Files.copy(existing, path, StandardCopyOption.REPLACE_EXISTING);
        logger.info("Saved file {}", path);

        return toFileEntry(path);
    }

    public boolean delete(String relativePath) throws IOException {
        Path path = resolvePath(relativePath);
        if (!Files.exists(path)) return false;

        if (Files.isDirectory(path)) {
            try (Stream<Path> paths = Files.walk(path)) {
                paths.sorted(Comparator.reverseOrder()).forEach(file -> {
                    try {
                        Files.delete(file);
                    } catch (IOException e) {
                        logger.error("Could not delete {}", e, file.toString());
                    }
                });
            }
        } else Files.delete(path);

        logger.info("Deleted {}", path);
        return true;
    }

    public FileEntry mkdir(String relativePath) throws IOException {
        Path path = resolvePath(relativePath);
        Files.createDirectories(path);
        logger.info("Created directory {}", path);

        return toFileEntry(path);
    }

    public FileEntry move(String fromPath, String toPath) throws IOException {
        Path from = resolvePath(fromPath);
        Path to = resolvePath(toPath);

        Path parent = to.getParent();
        if (!Files.exists(parent)) Files.createDirectories(parent);

        Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        logger.info("Moved {} to {}", from, to);
        return toFileEntry(to);
    }

    public boolean exists(String relativePath) {
        return Files.exists(resolvePath(relativePath));
    }

    public boolean isDirectory(String relativePath) {
        return Files.isDirectory(resolvePath(relativePath));
    }

    public Path getAbsolutePath(String relativePath) {
        return resolvePath(relativePath);
    }

    public long getDirectorySize(String relativePath) throws IOException {
        Path path = resolvePath(relativePath);
        if (!Files.isDirectory(path)) return Files.size(path);

        try (Stream<Path> walk = Files.walk(path)) {
            return walk.filter(Files::isRegularFile)
                .mapToLong(p -> {
                    try {
                        return Files.size(p);
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .sum();
        }
    }

    public List<FileEntry> search(String pattern, String basePath, int maxResults) throws IOException {
        Path base = resolvePath(basePath);
        List<FileEntry> results = new ArrayList<>();
        String lowerPattern = pattern.toLowerCase();

        try (Stream<Path> walk = Files.walk(base)) {
            walk.filter(p -> p.getFileName().toString().toLowerCase().contains(lowerPattern))
                .limit(maxResults)
                .forEach(p -> {
                    try {
                        results.add(toFileEntry(p));
                    } catch (IOException e) {
                        logger.error("Could not read file entry at {}", e, p);
                    }
                });
        }

        return results;
    }

    public long fileCount(String relativePath) throws IOException {
        try (Stream<Path> stream = Files.find(resolvePath(relativePath), Integer.MAX_VALUE, (path, attr) -> attr.isRegularFile())) {
            return stream.count();
        }
    }

    public long directoryCount(String relativePath) throws IOException {
        try (Stream<Path> stream = Files.find(resolvePath(relativePath), Integer.MAX_VALUE, (path, attr) -> attr.isDirectory())) {
            return stream.count();
        }
    }

    private Path resolvePath(@NotNull String relativePath) {
        if (relativePath.isEmpty() || relativePath.equals("/")) return rootPath;

        String normalizedPath = relativePath.replace("\\", "/");
        if (normalizedPath.startsWith("/")) normalizedPath = normalizedPath.substring(1);

        Path resolved = rootPath.resolve(normalizedPath).normalize();
        if (!resolved.startsWith(rootPath)) throw new SecurityException("path traversal :'( " + relativePath);

        return resolved;
    }

    private FileEntry toFileEntry(Path path) throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
        String relativePath = rootPath.relativize(path).toString();

        return  new FileEntry(path.getFileName().toString(), relativePath, attributes.isDirectory(), attributes.isDirectory() ? 0 : attributes.size(),
            attributes.lastModifiedTime().toInstant(), attributes.isDirectory() ? "" : MimeTypes.getMimeType(path.getFileName()));
    }
}
