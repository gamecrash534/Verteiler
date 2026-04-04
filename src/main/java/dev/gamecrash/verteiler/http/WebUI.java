package dev.gamecrash.verteiler.http;

import dev.gamecrash.verteiler.Main;
import dev.gamecrash.verteiler.config.Configuration;
import dev.gamecrash.verteiler.logging.Logger;
import dev.gamecrash.verteiler.model.FileEntry;
import dev.gamecrash.verteiler.storage.MimeTypes;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebUI {
    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm").withZone(ZoneId.systemDefault());
    private static final TemplateEngine engine = TemplateEngine.getInstance();
    private static byte[] icon = new byte[0];

    public static String browseDirectory(Configuration config, String path, List<FileEntry> entries, boolean isAdmin) {
        List<Map<String, Object>> entryList = new ArrayList<>();
        for (FileEntry entry : entries) {
            String entryPath = path.isEmpty() ? entry.name() : path + "/" + entry.name();
            String href;
            if (entry.isDirectory()) href = "/browse/" + entryPath;
            else if (config.enablePreview && MimeTypes.isPreviewable(entry.mimeType())) href = "/preview/" + entryPath;
            else href = "/download/" + entryPath;

            Map<String, Object> item = new HashMap<>();
            item.put("href", href);
            item.put("class", entry.isDirectory() ? "directory" : "file");
            item.put("icon", getFileIcon(entry.isDirectory() ? null : entry.mimeType(), entry.isDirectory()));
            item.put("name", escapeHtml(entry.name() + (entry.isDirectory() ? "/" : "")));
            item.put("showSize", config.showFileSizes);
            item.put("size", entry.getReadableSize());
            item.put("showDate", config.showDates);
            item.put("date", dateFormat.format(entry.lastModified()));
            entryList.add(item);
        }

        String content = engine.render("browse", TemplateEngine.context()
            .put("breadcrumb", buildBreadcrumb(path, "/browse"))
            .put("hasParent", !path.isEmpty())
            .put("parentUrl", getParentUrl(path, "/browse"))
            .put("entries", entryList)
            .put("empty", entries.isEmpty())
            .put("currentPath", path)
            .put("upIcon", engine.getIcon("up"))
            .build()
        );

        return renderLayout(config, path.isEmpty() ? "files" : path, content, isAdmin);
    }

    public static String previewFile(Configuration config, FileEntry entry, String path, boolean isAdmin) {
        String mimeType = entry.mimeType();
        String previewType = MimeTypes.getPreviewType(mimeType);

        String content = engine.render("preview", TemplateEngine.context()
            .put("fileName", escapeHtml(entry.name()))
            .put("filePath", path)
            .put("fileSize", entry.getReadableSize())
            .put("mimeType", mimeType.equals("-") ? "folder" : mimeType)
            .put("notPreviewable", previewType.isEmpty())
            .put("isImage", previewType.equals("image"))
            .put("isVideo", previewType.equals("video"))
            .put("isAudio", previewType.equals("audio"))
            .put("isPdf", previewType.equals("pdf"))
            .put("isText", previewType.equals("text"))
            .put("downloadIcon", engine.getIcon("download"))
            .put("externalIcon", engine.getIcon("external"))
            .build());

        return renderLayout(config, entry.name(), content, isAdmin);
    }

    public static String loginPage(Configuration config) {
        String content = engine.render("login", TemplateEngine.context().build());

        return renderLayout(config, "Login", content, false);
    }

    public static String dashboard(Configuration config, long totalSize, long fileItems, long directoryItems) {
        String content = engine.render("admin-dashboard", TemplateEngine.context()
            .put("totalSize", getReadableSize(totalSize))
            .put("fileCount", fileItems)
            .put("directoryCount", directoryItems)
            .put("folderIcon", engine.getIcon("folder"))
            .build()
        );

        return renderLayout(config, "admin", content, true);
    }

    public static String adminBrowse(Configuration config, String path, List<FileEntry> entries) {
        String baseUrl = "/admin/browse";

        List<Map<String, Object>> entryList = new ArrayList<>();
        for (FileEntry entry : entries) {
            String entryPath = path.isEmpty() ? entry.name() : path + "/" + entry.name();
            String href = entry.isDirectory() ? baseUrl + "/" + entryPath : "/download/" + entryPath;

            Map<String, Object> item = new HashMap<>();
            item.put("href", href);
            item.put("class", entry.isDirectory() ? "directory" : "file");
            item.put("icon", getFileIcon(entry.isDirectory() ? null : entry.mimeType(), entry.isDirectory()));
            item.put("name", escapeHtml(entry.name()) + (entry.isDirectory() ? "/" : ""));
            item.put("path", escapeJs(entryPath));
            item.put("showSize", config.showFileSizes);
            item.put("size", entry.isDirectory() ? "" : entry.getReadableSize());
            item.put("showDate", config.showDates);
            item.put("date", dateFormat.format(entry.lastModified()));
            entryList.add(item);
        }

        String content = engine.render("admin-browse", TemplateEngine.context()
            .put("breadcrumb", buildBreadcrumb(path, baseUrl))
            .put("hasParent", !path.isEmpty())
            .put("parentUrl", getParentUrl(path, baseUrl))
            .put("entries", entryList)
            .put("empty", entries.isEmpty())
            .put("currentPath", path)
            .put("uploadIcon", engine.getIcon("upload"))
            .put("mkdirIcon", engine.getIcon("mkdir"))
            .put("editIcon", engine.getIcon("edit"))
            .put("trashIcon", engine.getIcon("trash"))
            .put("chunkedUploads", config.chunkedUploadsEnabled)
            .put("chunkSize", config.chunkSize)
            .build());

        return renderLayout(config, "admin - files", content, true);
    }

    public static String error404(Configuration config, String message, boolean isAdmin) {
        return renderError(config, "404", message, isAdmin);
    }

    public static String error403(Configuration config, String message, boolean isAdmin) {
        return renderError(config, "403", message, isAdmin);
    }

    private static String renderError(Configuration config, String code, String message, boolean isAdmin) {
        String content = engine.render("error", TemplateEngine.context()
                .put("code", code)
                .put("message", escapeHtml(message))
                .build()
        );

        return renderLayout(config, code, content, isAdmin);
    }


    private static String renderLayout(Configuration config, String title, String content, boolean isAdmin) {
        return engine.render("layout", TemplateEngine.context()
            .put("title", title)
            .put("appName", config.title)
            .put("content", content)
            .put("showFooter", config.footerEnabled)
            .put("showCredits", config.showCredits)
            .put("isAdmin", isAdmin)
            .put(title.contains("admin") ? "isOnAdmin" : "isBrowsing", true)
            .build());
    }

    private static String getFileIcon(String mimeType, boolean isDirectory) {
        if (isDirectory) return engine.getIcon("folder");
        if (mimeType == null) return engine.getIcon("file");
        if (mimeType.startsWith("image/")) return engine.getIcon("image");
        if (mimeType.startsWith("video/")) return engine.getIcon("video");
        if (mimeType.startsWith("audio/")) return engine.getIcon("audio");
        if (mimeType.startsWith("text/")) return engine.getIcon("text");
        if (mimeType.contains("zip") || mimeType.contains("tar") || mimeType.contains("compress")) return engine.getIcon("archive");

        return engine.getIcon("file");
    }

    private static String buildBreadcrumb(String path, String baseUrl) {
        if (path.isEmpty()) return "<a href=\"" + baseUrl + "\">/</a>";

        StringBuilder builder = new StringBuilder();
        builder.append("<a href=\"").append(baseUrl).append("\">/</a>");

        String[] parts = path.split("/");
        StringBuilder currentPath = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                currentPath.append(parts[i]);
                if (i < parts.length - 1) builder.append("<a href=\"").append(baseUrl).append("/").append(currentPath).append("\">").append(parts[i]).append("/</a>");
                else builder.append("<span>").append(parts[i]).append("</span>");

                currentPath.append("/");
            }
        }
        return builder.toString();
    }

    public static String getCSS(String name) {
        return engine.getCSS(name);
    }

    public static String getJS(String script) {
        return engine.getJS(script);
    }

    public static byte[] getFavicon() {
        if (icon.length > 0) return icon;

        try (InputStream stream = Main.class.getClassLoader().getResourceAsStream("web/static/favicon.png")) {
            if (stream == null) return null;
            return icon = stream.readAllBytes();
        } catch (IOException e) {
            Logger.getInstance().error("Could not load favicon", e);
            return null;
        }
    }

    private static String getParentUrl(String path, String baseUrl) {
        if (path.isEmpty()) return baseUrl;
        String parent = path.contains("/") ? path.substring(0, path.lastIndexOf('/')) : "";

        return baseUrl + "/" + parent;
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    private static String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\"", "\\\"")
            .replace("\n", "\\n");
    }

    public static String getReadableSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KiB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MiB", size / (1024.0 * 1024));
        else return String.format("%.1f GiB", size / (1024.0 * 1024 * 1024));
    }
}
