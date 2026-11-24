package org.openl.rules.webstudio.dependencies;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.jspecify.annotations.Nullable;

import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.util.FileUtils;

/**
 * The {@code pom.xml} the OpenL Maven Plugin builds a project without one with: a pom-less project.
 *
 * <p>The anchor of the project is the nearest {@code pom.xml} in a folder above it, when it declares the
 * {@code openl-maven-plugin} among its own build plugins and is no OpenL project itself. The project takes the anchor
 * as its parent: its dependency management, its dependencies and its properties, as Maven inherits them.
 *
 * <p>The dependency management of the anchor sets the version of an OpenL project the {@code rules.xml} names by Maven
 * coordinates too, as the plugin sets it. A project with no anchor above it has no {@code pom.xml}, nor has one the
 * scan of the anchor does not reach: below a folder named {@code target} or starting with a dot, or inside another
 * OpenL project.
 *
 * @author Yury Molchan
 */
final class PomlessPom {

    private static final String PLUGIN_GROUP = "org.openl.rules";
    private static final String PLUGIN = "openl-maven-plugin";
    private static final Set<String> OPENL_PACKAGINGS = Set.of("openl", "openl-jar");
    private static final String TARGET = "target";
    private static final Pattern INVALID_ID = Pattern.compile("[^A-Za-z0-9_.-]");

    private PomlessPom() {
    }

    /**
     * The {@code pom.xml} of a project without one, whose parent is its anchor.
     *
     * @param folder       the path of the folder of the project, with {@code /} separators
     * @param origin       where it comes from, as {@link ProjectPom} tells it
     * @param reader       reads the POMs above the folder
     * @param dependencies the dependencies the {@code rules.xml} of the project declares, or {@code null}
     * @return the {@code pom.xml}, or {@code null} when no anchor is above the folder
     */
    static @Nullable ProjectPom of(String folder,
                                   String origin,
                                   ProjectPom.Reader reader,
                                   @Nullable Collection<ProjectDependencyDescriptor> dependencies) throws IOException {
        var path = folder + "/" + ProjectPom.FILE_NAME;
        var relativePath = "../" + ProjectPom.FILE_NAME;
        // Up to the root of the repository, above which no path leads
        for (var above = ProjectPom.resolve(path, relativePath); above != null;
                above = ProjectPom.resolve(path, relativePath)) {
            var text = reader.read(above);
            if (text != null) {
                // The plugin looks no higher than the nearest pom.xml: the scan of a higher anchor stops at its folder
                var anchor = PomReactor.parse(text);
                return anchor == null || !isAnchor(anchor) || !isScanned(folderOf(above), folder, reader)
                        ? null
                        : new ProjectPom(textOf(anchor, relativePath, folder),
                                path,
                                origin,
                                reader,
                                projectsOf(dependencies));
            }
            relativePath = "../" + relativePath;
        }
        return null;
    }

    /** Whether the plugin builds the projects without a {@code pom.xml} below the {@code pom.xml}. */
    private static boolean isAnchor(Model model) {
        return !OPENL_PACKAGINGS.contains(model.getPackaging()) && model.getBuild() != null
                && model.getBuild()
                        .getPlugins()
                        .stream()
                        .anyMatch(plugin -> PLUGIN_GROUP.equals(plugin.getGroupId())
                                && PLUGIN.equals(plugin.getArtifactId()));
    }

    /**
     * Whether the scan of the anchor reaches the project, as the plugin scans: it skips the folders named
     * {@code target} or starting with a dot, and goes no deeper than a folder holding a {@code rules.xml}, another
     * OpenL project.
     */
    private static boolean isScanned(String anchorFolder, String folder, ProjectPom.Reader reader)
            throws IOException {
        var below = anchorFolder.isEmpty() ? folder : folder.substring(anchorFolder.length() + 1);
        var names = below.split("/");
        var path = new StringBuilder(anchorFolder);
        for (var i = 0; i < names.length; i++) {
            if (names[i].startsWith(".") || TARGET.equals(names[i])) {
                return false;
            }
            path.append(path.isEmpty() ? "" : "/").append(names[i]);
            if (i < names.length - 1 && reader.read(path + "/" + ProjectDescriptor.FILE_NAME) != null) {
                return false;
            }
        }
        return true;
    }

    /** The folder of the file, empty for a file at the root. */
    private static String folderOf(String file) {
        var slash = file.lastIndexOf('/');
        return slash < 0 ? "" : file.substring(0, slash);
    }

    /** The text of a {@code pom.xml} with the anchor as its parent, named after the folder as the plugin names it. */
    private static String textOf(Model anchor, String relativePath, String folder) throws IOException {
        var parent = new Parent();
        parent.setGroupId(PomReactor.groupIdOf(anchor));
        parent.setArtifactId(anchor.getArtifactId());
        parent.setVersion(PomReactor.versionOf(anchor));
        parent.setRelativePath(relativePath);
        var model = new Model();
        model.setModelVersion("4.0.0");
        model.setParent(parent);
        // Maven takes no other characters in an id, which a folder of OpenL Studio may hold, such as a space
        model.setArtifactId(INVALID_ID.matcher(FileUtils.getName(folder)).replaceAll("_"));
        var text = new StringWriter();
        new MavenXpp3Writer().write(text, model);
        return text.toString();
    }

    /** The OpenL projects the {@code rules.xml} names by Maven coordinates, by their names. */
    private static Map<String, String> projectsOf(@Nullable Collection<ProjectDependencyDescriptor> dependencies) {
        var projects = new LinkedHashMap<String, String>();
        MavenDependencyResolver.namedMavenDependenciesOf(dependencies, Map.of())
                .forEach(dependency -> projects.putIfAbsent(dependency.getName(),
                        dependency.getMavenArtifact().trim()));
        return projects;
    }
}
