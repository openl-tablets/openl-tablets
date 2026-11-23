package org.openl.rules.webstudio.dependencies;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.apache.maven.model.building.ModelSource2;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import org.openl.util.formatters.FileNameFormatter;

/**
 * The {@code pom.xml} of a project, and where the POMs it names by a path relative to it are read.
 *
 * <p>A parent is found as Maven finds it in a build of several modules: by its {@code relativePath}, {@code ../pom.xml}
 * by default, beside the {@code pom.xml}, before the Maven repository. A project of a design repository reads it from
 * the repository at the version of the project; a project on disk, from the disk.
 *
 * <p>Two of them are one when their text and their path are, the place they come from included: what is read from them
 * is kept in memory by them.
 *
 * @author Yury Molchan
 */
public final class ProjectPom {

    /** The name of the file, in the folder of the project. */
    public static final String FILE_NAME = "pom.xml";

    private final String text;
    private final String path;
    private final String origin;
    private final Reader reader;

    /**
     * Reads a file by its path, as the {@code pom.xml} names it relative to its own.
     */
    @FunctionalInterface
    public interface Reader {
        /**
         * @param path the path, with {@code /} separators, as the path of the {@code pom.xml} is written
         * @return the text of the file, or {@code null} when there is none
         */
        @Nullable String read(String path) throws IOException;
    }

    /**
     * @param text   the text of the {@code pom.xml}
     * @param path   its path, with {@code /} separators, which the paths it names are relative to
     * @param origin where it comes from, such as a repository and a version, which tells two paths apart
     * @param reader reads the files the {@code pom.xml} names by a path relative to its own
     */
    ProjectPom(String text, String path, String origin, Reader reader) {
        this.text = text;
        this.path = path;
        this.origin = origin;
        this.reader = reader;
    }

    /** A {@code pom.xml} read alone, which finds every POM it names in the Maven repository. */
    public static ProjectPom of(String text) {
        return new ProjectPom(text, FILE_NAME, "", path -> null);
    }

    public String text() {
        return text;
    }

    /** Its path, with {@code /} separators, which the paths it names are relative to. */
    String path() {
        return path;
    }

    /** A POM read beside it, which reads the files it names as this one does. */
    ProjectPom related(String relatedText, String relatedPath) {
        return new ProjectPom(relatedText, relatedPath, origin, reader);
    }

    /**
     * Reads a file the {@code pom.xml} names by a path relative to its own, already resolved against it.
     *
     * @return the text, or {@code null} when there is none or it cannot be read
     */
    @Nullable String read(String related) {
        try {
            return reader.read(related);
        } catch (IOException e) {
            return null;
        }
    }

    /** The {@code pom.xml} as Maven reads a model, finding the POMs it names by a relative path beside it. */
    ModelSource2 modelSource() {
        return new Source(this);
    }

    /**
     * The path the relative path leads to from the folder of the file, or {@code null} when it leads out of the root.
     * A path naming no {@code .xml} file names a folder, whose {@code pom.xml} is meant, as Maven reads it.
     */
    static @Nullable String resolve(String file, String relativePath) {
        var target = FileNameFormatter.normalizePath(relativePath);
        target = target.endsWith(".xml") ? target : StringUtils.trimTrailingCharacter(target, '/') + "/" + FILE_NAME;
        var resolved = StringUtils.cleanPath(file.substring(0, file.lastIndexOf('/') + 1) + target);
        return resolved.startsWith("../") || resolved.startsWith("/../") ? null : resolved;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ProjectPom other && text.equals(other.text) && path.equals(other.path)
                && origin.equals(other.origin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, path, origin);
    }

    @Override
    public String toString() {
        return origin + ":" + path;
    }

    /** A POM read as a model, with the POMs it names by a relative path. */
    private record Source(ProjectPom pom) implements ModelSource2 {

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(pom.text.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public String getLocation() {
            return pom.path;
        }

        @Override
        public @Nullable ModelSource2 getRelatedSource(String relativePath) {
            var related = resolve(pom.path, relativePath);
            // With none, Maven looks for the POM in the repository
            var relatedText = related == null ? null : pom.read(related);
            return relatedText == null ? null : new Source(pom.related(relatedText, related));
        }

        @Override
        public URI getLocationURI() {
            try {
                return new URI("openl", pom.origin + "!/" + pom.path, null);
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
