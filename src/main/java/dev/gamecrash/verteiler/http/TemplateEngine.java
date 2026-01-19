package dev.gamecrash.verteiler.http;

import dev.gamecrash.verteiler.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateEngine {
    private static TemplateEngine instance;
    private static final Logger logger = Logger.getInstance();

    private static final Pattern variablePattern = Pattern.compile("\\{\\{([^#/}]+?)}}");
    private static final Pattern sectionPattern = Pattern.compile("\\{\\{#([^}]+)}}(.*?)\\{\\{/\\1}}", Pattern.DOTALL);

    private final Map<String, String> templateCache = new ConcurrentHashMap<>();
    private final Map<String, String> staticFileCache = new ConcurrentHashMap<>();

    public static TemplateEngine getInstance() {
        if (instance == null) instance = new TemplateEngine();
        return instance;
    }

    public String getTemplate(String name) {
        return templateCache.computeIfAbsent(name, this::loadTemplate);
    }

    public String getCSS() {
        return staticFileCache.computeIfAbsent("web/static/css/style.css", key -> loadResource(key));
    }

    public String render(String template, Map<String, Object> context) {
        return  renderString(getTemplate(template), context);
    }

    public String renderString(String template, Map<String, Object> context) {
        template = processSections(template, context);
        template = replaceVariables(template, context);

        return template;
    }

    private String processSections(String template, Map<String, Object> context) {
        Matcher matcher = sectionPattern.matcher(template);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String sectionName = matcher.group(1);
            String sectionContent = matcher.group(2);
            Object value = context.get(sectionName);

            String replacement;
            if (value == null || value.equals(false) || value.equals("")) replacement = "";
            else if (value instanceof Iterable<?>) {
                StringBuilder itemsBuilder = new StringBuilder();
                for (Object item : (Iterable<?>) value) {
                    if (item instanceof Map) {
                        Map<String, Object> itemCtx = new HashMap<>(context);
                        //noinspection unchecked
                        itemCtx.putAll((Map<String, Object>) item);
                        itemsBuilder.append(renderString(sectionContent, itemCtx));
                    } else itemsBuilder.append(sectionContent);
                }
                replacement = itemsBuilder.toString();
            } else replacement = renderString(sectionContent, context);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);

        return buffer.toString();
    }

    private String replaceVariables(String template, Map<String, Object> context) {
        Matcher matcher = variablePattern.matcher(template);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = context.get(varName);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(value == null ? "" : value.toString()));
        }
        matcher.appendTail(buffer);

        return buffer.toString();
    }

    private String loadResource(String path) {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
            if (stream == null) return "<!-- Resource '" + path + "' not found -->";
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Could not load resource {}", e, path);
            return "<!-- Error loading '" + path + "' -->";
        }
    }

    private String loadTemplate(String name) {
        return loadResource("web/templates/" + name + ".html");
    }

    public static ContextBuilder context() {
        return new ContextBuilder();
    }

    public static class ContextBuilder {
        private final Map<String, Object> context = new HashMap<>();

        public ContextBuilder put(String key, Object value) {
            context.put(key, value);
            return this;
        }

        public Map<String, Object> build() {
            return context;
        }
    }
}
