package org.openl.rules.webstudio.dependencies;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.artifact.Artifact;
import org.jspecify.annotations.Nullable;

/**
 * The build of several modules a {@code pom.xml} is a module of: the topmost of its parents found beside it, each by
 * its {@code relativePath}, and every module it lists, the modules of those modules included.
 *
 * <p>A dependency on another module of the same build at the version of that module is built with it, as a Maven
 * build takes it from the reactor. It is not downloaded: the Maven repository holds no such version, such as a
 * {@code -SNAPSHOT} no pipeline publishes, and asking for it only waits for the answer that it is missing.
 *
 * <p>A module is told by its {@code groupId}, {@code artifactId} and version, each inherited from its parent when it
 * does not say its own.
 *
 * @author Yury Molchan
 */
@Slf4j
final class PomReactor {

    private static final Pattern PROPERTY = Pattern.compile("\\$\\{([^}]+)}");

    private PomReactor() {
    }

    /**
     * The topmost parent of the {@code pom.xml} found beside it, through the parent beside each parent, when it lists
     * modules.
     *
     * @return the parent, or {@code null} when no parent beside it lists modules
     */
    static @Nullable ProjectPom rootOf(ProjectPom pom) {
        ProjectPom root = null;
        var visited = new HashSet<String>();
        for (var current = parentOf(pom); current != null && visited.add(current.path()); current = parentOf(current)) {
            root = current;
        }
        var model = root == null ? null : parse(root.text());
        return model == null || model.getModules().isEmpty() ? null : root;
    }

    /**
     * The coordinates of every module of the build: those the root lists, and those its modules list in turn.
     *
     * @return {@code groupId:artifactId:version} of each module that can be read
     */
    static Set<String> modulesOf(ProjectPom root) {
        var modules = new HashSet<String>();
        var visited = new HashSet<String>();
        var queue = new ArrayDeque<Map.Entry<String, Properties>>();
        queue.add(Map.entry(root.path(), new Properties()));
        while (!queue.isEmpty()) {
            var next = queue.poll();
            var path = next.getKey();
            var model = visited.add(path) ? parse(root.read(path)) : null;
            if (model != null) {
                // A module takes the properties of the one listing it, such as the revision of a CI-friendly version
                var properties = new Properties();
                properties.putAll(next.getValue());
                properties.putAll(model.getProperties());
                if (!path.equals(root.path())) {
                    modules.add(coordinatesOf(interpolate(groupIdOf(model), properties),
                            model.getArtifactId(),
                            interpolate(versionOf(model), properties)));
                }
                model.getModules()
                        .stream()
                        .map(module -> ProjectPom.resolve(path, module))
                        .filter(Objects::nonNull)
                        .forEach(module -> queue.add(Map.entry(module, properties)));
            }
        }
        return modules;
    }

    /** The value with the properties it names put in; a property none sets is left as it is written. */
    private static @Nullable String interpolate(@Nullable String value, Properties properties) {
        if (value == null || !value.contains("${")) {
            return value;
        }
        return PROPERTY.matcher(value)
                .replaceAll(found -> Matcher.quoteReplacement(properties.getProperty(found.group(1), found.group())));
    }

    /** Whether the artifact is one of the modules, at the version of the module. */
    static boolean holds(Set<String> modules, Artifact artifact) {
        var coordinates = coordinatesOf(artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion());
        return modules.contains(coordinates);
    }

    /**
     * The parent of the {@code pom.xml} beside it, by its {@code relativePath}, {@code ../pom.xml} by default, when it
     * is the parent the {@code pom.xml} names. An empty {@code relativePath} tells Maven to look for the parent in the
     * repository only.
     */
    private static @Nullable ProjectPom parentOf(ProjectPom pom) {
        var model = parse(pom.text());
        var parent = model == null ? null : model.getParent();
        if (parent == null) {
            return null;
        }
        var relativePath = parent.getRelativePath() == null ? "../" + ProjectPom.FILE_NAME : parent.getRelativePath();
        var path = relativePath.isBlank() ? null : ProjectPom.resolve(pom.path(), relativePath);
        var text = path == null ? null : pom.read(path);
        var parentModel = parse(text);
        if (parentModel == null
                || !Objects.equals(parent.getGroupId(), groupIdOf(parentModel))
                || !Objects.equals(parent.getArtifactId(), parentModel.getArtifactId())
                || !Objects.equals(parent.getVersion(), versionOf(parentModel))) {
            return null;
        }
        return pom.related(text, path);
    }

    private static String coordinatesOf(@Nullable String groupId, String artifactId, @Nullable String version) {
        return groupId + ":" + artifactId + ":" + version;
    }

    static @Nullable String groupIdOf(Model model) {
        return model.getGroupId() != null || model.getParent() == null ? model.getGroupId()
                : model.getParent().getGroupId();
    }

    static @Nullable String versionOf(Model model) {
        return model.getVersion() != null || model.getParent() == null ? model.getVersion()
                : model.getParent().getVersion();
    }

    static @Nullable Model parse(@Nullable String text) {
        if (text == null) {
            return null;
        }
        try {
            return new MavenXpp3Reader().read(new StringReader(text), false);
        } catch (IOException | XmlPullParserException e) {
            log.debug("Cannot read a POM of the build.", e);
            return null;
        }
    }
}
