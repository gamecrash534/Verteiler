package dev.gamecrash.verteiler.storage;

import java.util.HashMap;
import java.util.Map;

public class MimeTypes {
    private static final Map<String, String> mimeTypes = new HashMap<>();

    static {
        // Text
        mimeTypes.put("txt", "text/plain");
        mimeTypes.put("html", "text/html");
        mimeTypes.put("htm", "text/html");
        mimeTypes.put("css", "text/css");
        mimeTypes.put("js", "text/javascript");
        mimeTypes.put("json", "application/json");
        mimeTypes.put("xml", "application/xml");
        mimeTypes.put("csv", "text/csv");
        mimeTypes.put("md", "text/markdown");

        // Images
        mimeTypes.put("png", "image/png");
        mimeTypes.put("jpg", "image/jpeg");
        mimeTypes.put("jpeg", "image/jpeg");
        mimeTypes.put("gif", "image/gif");
        mimeTypes.put("webp", "image/webp");
        mimeTypes.put("svg", "image/svg+xml");
        mimeTypes.put("ico", "image/x-icon");
        mimeTypes.put("bmp", "image/bmp");

        // Audio
        mimeTypes.put("mp3", "audio/mpeg");
        mimeTypes.put("wav", "audio/wav");
        mimeTypes.put("ogg", "audio/ogg");
        mimeTypes.put("flac", "audio/flac");
        mimeTypes.put("m4a", "audio/mp4");

        // Video
        mimeTypes.put("mp4", "video/mp4");
        mimeTypes.put("webm", "video/webm");
        mimeTypes.put("mkv", "video/x-matroska");
        mimeTypes.put("avi", "video/x-msvideo");
        mimeTypes.put("mov", "video/quicktime");

        // Archives
        mimeTypes.put("zip", "application/zip");
        mimeTypes.put("tar", "application/x-tar");
        mimeTypes.put("gz", "application/gzip");
        mimeTypes.put("7z", "application/x-7z-compressed");
        mimeTypes.put("rar", "application/vnd.rar");
        mimeTypes.put("xz", "application/x-xz");

        // Documents
        mimeTypes.put("pdf", "application/pdf");
        mimeTypes.put("doc", "application/msword");
        mimeTypes.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        mimeTypes.put("xls", "application/vnd.ms-excel");
        mimeTypes.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        mimeTypes.put("ppt", "application/vnd.ms-powerpoint");
        mimeTypes.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        mimeTypes.put("odt", "application/vnd.oasis.opendocument.text");
        mimeTypes.put("ods", "application/vnd.oasis.opendocument.spreadsheet");

        // Executables and binaries
        mimeTypes.put("exe", "application/x-msdownload");
        mimeTypes.put("msi", "application/x-msdownload");
        mimeTypes.put("deb", "application/x-debian-package");
        mimeTypes.put("rpm", "application/x-rpm");
        mimeTypes.put("dmg", "application/x-apple-diskimage");
        mimeTypes.put("iso", "application/x-iso9660-image");
        mimeTypes.put("bin", "application/octet-stream");

        // Code
        mimeTypes.put("java", "text/x-java-source");
        mimeTypes.put("py", "text/x-python");
        mimeTypes.put("c", "text/x-c");
        mimeTypes.put("cpp", "text/x-c++");
        mimeTypes.put("h", "text/x-c");
        mimeTypes.put("rs", "text/x-rust");
        mimeTypes.put("go", "text/x-go");
        mimeTypes.put("sh", "text/x-shellscript");
        mimeTypes.put("bash", "text/x-shellscript");

        // Fonts
        mimeTypes.put("woff", "font/woff");
        mimeTypes.put("woff2", "font/woff2");
        mimeTypes.put("ttf", "font/ttf");
        mimeTypes.put("otf", "font/otf");

        // thanks to AI for generating this, I would be way too lazy to do this by hand
    }

    public static String getMimeType(String filename) {
        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex < 0) return "application/octet-stream";
        String extension = filename.substring(dotIndex + 1).toLowerCase();

        return mimeTypes.getOrDefault(extension, "application/octet-stream");
    }

    public static boolean isPreviewable(String mimeType) {
        return isText(mimeType) || isImage(mimeType) || isVideo(mimeType) || isAudio(mimeType) || mimeType.equals("application/pdf");
    }

    public static String getPreviewType(String mimeType) {
        if (isText(mimeType)) return "text";
        if (isImage(mimeType)) return "image";
        if (isVideo(mimeType)) return "video";
        if (isAudio(mimeType)) return "audio";
        if (mimeType.equals("application/pdf")) return "pdf";
        return "";
    }

    public static boolean isText(String mimeType) {
        return mimeType.startsWith("text/") || mimeType.equals("application/json") || mimeType.equals("application/xml");
    }

    public static boolean isImage(String mimeType) {
        return mimeType.startsWith("image/");
    }

    public static boolean isVideo(String mimeType) {
        return mimeType.startsWith("video/");
    }

    public static boolean isAudio(String mimeType) {
        return mimeType.startsWith("audio/");
    }
}
