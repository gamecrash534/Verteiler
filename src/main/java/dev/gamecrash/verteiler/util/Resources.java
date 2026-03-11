package dev.gamecrash.verteiler.util;

import dev.gamecrash.verteiler.Main;
import dev.gamecrash.verteiler.config.Configuration;
import dev.gamecrash.verteiler.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Resources {
    private static final Logger logger = Logger.getInstance();
    private static final Configuration config = Configuration.getInstance();
    private static final Path resourcesPath = Path.of(config.customResourcesDirectory);

    public static String loadResource(String path) {
        try {
            if (Configuration.getInstance().useCustomResources) {
                path = path.substring("web/".length());
                if (!Files.exists(resourcesPath.resolve(path))) return "<!-- Resource '" + path + "' not found -->";

                return new String(Files.readAllBytes(resourcesPath.resolve(path)));
            } else {
                try (InputStream stream = Main.class.getClassLoader().getResourceAsStream(path)) {
                    if (stream == null) return "<!-- Resource '" + path + "' not found -->";
                    return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        } catch (IOException e) {
            logger.error("Could not load resource {}", e, path);
            return "<!-- Error loading '" + path + "' -->";
        }
    }

    public static void saveResourcesToPath(Path path) {
        try {
            URL jar = Main.class.getClassLoader().getResource("web/");
            JarURLConnection con = (JarURLConnection) jar.openConnection();
            JarFile file = con.getJarFile();
            Enumeration<JarEntry> entries = file.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().startsWith("web")) continue;

                Path targetPath = path.resolve(entry.getName().replace("web/", ""));
                if (entry.isDirectory()) Files.createDirectories(targetPath);
                else {
                    Files.createDirectories(targetPath.getParent());

                    try (var in = file.getInputStream(entry)) {
                        Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }

            logger.info("Saved all resources to respective directory");
        } catch (IOException e) {
            logger.error("Could not save resources to directory: " + path.getFileName());
            logger.info("Exiting");
            System.exit(1);
        }
    }
}
