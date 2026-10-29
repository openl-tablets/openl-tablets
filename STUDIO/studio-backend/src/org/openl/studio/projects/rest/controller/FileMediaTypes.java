package org.openl.studio.projects.rest.controller;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;

import org.openl.util.FileUtils;

/**
 * The media type a project file is answered with, by the extension of its name.
 *
 * <p>It is the type Spring knows for the extension, or {@code application/octet-stream} for an unknown one. The text
 * files a rules project keeps that Spring does not list — Markdown, YAML, properties and Groovy — are answered as
 * text, so a client reads them as text rather than download them as bytes.
 *
 * @author Yury Molchan
 */
final class FileMediaTypes {

    private static final MediaType TEXT_MARKDOWN = MediaType.parseMediaType("text/markdown");
    private static final MediaType APPLICATION_YAML = MediaType.parseMediaType("application/yaml");

    private static final Map<String, MediaType> TEXT_TYPES = Map.of(
            "md", TEXT_MARKDOWN,
            "markdown", TEXT_MARKDOWN,
            "yaml", APPLICATION_YAML,
            "yml", APPLICATION_YAML,
            "properties", MediaType.TEXT_PLAIN,
            "groovy", MediaType.TEXT_PLAIN);

    private FileMediaTypes() {
    }

    /**
     * The media type of a file by its name.
     */
    static MediaType of(String fileName) {
        var extension = Objects.requireNonNullElse(FileUtils.getExtension(fileName), "");
        var type = TEXT_TYPES.get(extension.toLowerCase(Locale.ROOT));
        return type != null ? type : MediaTypeFactory.getMediaType(fileName).orElse(MediaType.APPLICATION_OCTET_STREAM);
    }
}
