package dev.gamecrash.verteiler.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MimeTypes {
    public static String getMimeType(Path file) {
        try {
            String type = Files.probeContentType(file);
            return type == null ? "application/octet-stream" : type;
        } catch (IOException e) {
            return "application/octet-stream";
        }
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
