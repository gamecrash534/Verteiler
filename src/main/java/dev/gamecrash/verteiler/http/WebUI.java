package dev.gamecrash.verteiler.http;

import dev.gamecrash.verteiler.config.Configuration;
import dev.gamecrash.verteiler.storage.FileEntry;
import dev.gamecrash.verteiler.storage.MimeTypes;
import org.jetbrains.annotations.Nullable;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebUI {
    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm").withZone(ZoneId.systemDefault());
    private static final TemplateEngine engine = TemplateEngine.getInstance();

    public static String browseDirectory(Configuration config, String path, List<FileEntry> entries) {
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
                .build()
        );

        return renderLayout(config, path.isEmpty() ? "files" : path, content, null);
    }

    private static String renderLayout(Configuration config, String title, String content, @Nullable String scripts) {
        return engine.render("layout", TemplateEngine.context()
            .put("title", title)
            .put("appName", config.title)
            .put("content", content)
            .put("scripts", scripts)
            .put("showFooter", config.footerEnabled)
            .put("showCredits", config.showCredits)
            .build());
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

    public static String getCSS() {
        return engine.getCSS();
    }

    public static String getJS() {
        return engine.getJS();
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
}
