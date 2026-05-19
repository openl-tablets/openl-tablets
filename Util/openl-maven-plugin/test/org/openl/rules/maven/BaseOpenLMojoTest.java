package org.openl.rules.maven;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Covers the OpenL-project detection performed by {@link BaseOpenLMojo#execute()} before the goal body
 * runs. Two signals: openl-maven-plugin in build, or {@code rules.xml} in the configured source
 * directory. The plugin's own GAV is read at runtime from the injected {@link PluginDescriptor}, so
 * these tests instantiate a stub mojo with a {@link PluginDescriptor} configured exactly the way Maven
 * would configure it.
 */
class BaseOpenLMojoTest {

    private static final String GID = "org.openl.rules";
    private static final String AID = "openl-maven-plugin";

    @Test
    void noPluginAndNoRulesXmlMeansNotOpenLProject(@TempDir Path source) {
        var mojo = newMojo(GID, AID);
        // Empty source directory, no plugins declared → not an OpenL project.
        assertFalse(mojo.isOpenLProject(List.of(), source.toFile()));
        assertFalse(mojo.isOpenLProject(null, source.toFile()));
        // Null sourceDirectory is treated the same way (defensive).
        assertFalse(mojo.isOpenLProject(null, null));
        assertFalse(mojo.isOpenLProject(List.of(), null));
    }

    @Test
    void openlMavenPluginAloneMakesItAnOpenLProject(@TempDir Path source) {
        var mojo = newMojo(GID, AID);
        assertTrue(mojo.isOpenLProject(List.of(plugin(GID, AID)), source.toFile()));
    }

    @Test
    void openlMavenPluginAmongOthersStillMatches(@TempDir Path source) {
        var mojo = newMojo(GID, AID);
        // Order doesn't matter — anyMatch.
        assertTrue(mojo.isOpenLProject(List.of(
                plugin("org.foo", "bar-plugin"),
                plugin(GID, AID),
                plugin("org.baz", "qux-plugin")), source.toFile()));
    }

    @Test
    void unrelatedPluginsAloneMeanNotOpenLProject(@TempDir Path source) {
        var mojo = newMojo(GID, AID);
        assertFalse(mojo.isOpenLProject(List.of(
                plugin("org.foo", "bar-plugin"),
                plugin("org.baz", "qux-plugin")), source.toFile()));
    }

    @Test
    void pluginMatchRequiresBothGroupAndArtifactId(@TempDir Path source) {
        var mojo = newMojo(GID, AID);
        // Wrong groupId — must not match (an artifactId clash like this is unlikely in practice but
        // the guard documents the intent).
        assertFalse(mojo.isOpenLProject(List.of(plugin("io.bogus", AID)), source.toFile()));
        // Wrong artifactId — must not match.
        assertFalse(mojo.isOpenLProject(
                List.of(plugin(GID, "openl-something-else")), source.toFile()));
    }

    @Test
    void rulesXmlInSourceDirectoryMakesItAnOpenLProjectEvenWithoutPlugin(@TempDir Path source) {
        var mojo = newMojo(GID, AID);
        // A pom-less / plugin-less project that nevertheless carries OpenL rules.xml is still treated
        // as an OpenL project.
        touch(source.resolve("rules.xml"));
        assertTrue(mojo.isOpenLProject(List.of(), source.toFile()));
        assertTrue(mojo.isOpenLProject(null, source.toFile()));
        assertTrue(mojo.isOpenLProject(List.of(plugin("org.foo", "bar-plugin")), source.toFile()));
    }

    @Test
    void missingSourceDirectoryFallsBackToPluginOnlyCheck(@TempDir Path tmp) {
        var mojo = newMojo(GID, AID);
        // sourceDirectory pointing at a non-existent path — rules.xml fallback simply returns false,
        // the plugin check decides.
        var missing = tmp.resolve("absent").toFile();
        assertFalse(mojo.isOpenLProject(List.of(), missing));
        assertTrue(mojo.isOpenLProject(List.of(plugin(GID, AID)), missing));
    }

    @Test
    void rulesXmlAsDirectoryDoesNotCountAsOpenLProject(@TempDir Path source) throws IOException {
        var mojo = newMojo(GID, AID);
        // Defensive: a "rules.xml" *directory* must not be mistaken for the descriptor file.
        Files.createDirectory(source.resolve("rules.xml"));
        assertFalse(mojo.isOpenLProject(List.of(), source.toFile()));
    }

    @Test
    void pluginCoordinatesComeFromInjectedDescriptor(@TempDir Path source) {
        // The detector reads the GAV from the injected PluginDescriptor — if the plugin is rebranded
        // with a different gid/aid, that rebrand drives the match too. The check is plugin-coordinate
        // agnostic.
        var rebrandedMojo = newMojo("com.example", "openl-rebranded-plugin");
        assertTrue(rebrandedMojo.isOpenLProject(
                List.of(plugin("com.example", "openl-rebranded-plugin")), source.toFile()));
        // The original gid/aid no longer matches the rebranded descriptor.
        assertFalse(rebrandedMojo.isOpenLProject(List.of(plugin(GID, AID)), source.toFile()));
    }

    @Test
    void allowedDependenciesIncludesDependencyWithoutExplicitScope() {
        // Maven's effective model leaves <scope> at its raw POM value — for a POM that omits the
        // element, Dependency.getScope() returns null. The documented default ("compile") is applied
        // by the artifact resolver, not the model. The mojo must therefore treat null as compile so
        // default-scope deps aren't silently dropped from package/verify output.
        var mojo = newMojo(GID, AID);
        mojo.project = newProjectWithDependencies(
                dep("g1", "a-no-scope", null),
                dep("g2", "a-compile", Artifact.SCOPE_COMPILE),
                dep("g3", "a-test", Artifact.SCOPE_TEST));
        var allowed = mojo.getAllowedDependencies(BaseOpenLMojo::isRuntimeScope);
        assertTrue(allowed.contains("g1:a-no-scope"));
        assertTrue(allowed.contains("g2:a-compile"));
        assertFalse(allowed.contains("g3:a-test"));
    }

    private static MavenProject newProjectWithDependencies(Dependency... deps) {
        var model = new Model();
        for (var d : deps) {
            model.addDependency(d);
        }
        return new MavenProject(model);
    }

    private static Dependency dep(String groupId, String artifactId, String scope) {
        var d = new Dependency();
        d.setGroupId(groupId);
        d.setArtifactId(artifactId);
        d.setVersion("1");
        d.setScope(scope);
        return d;
    }

    /** A stub subclass that satisfies the abstract contract — we never actually invoke {@code execute}. */
    private static BaseOpenLMojo newMojo(String groupId, String artifactId) {
        var descriptor = new PluginDescriptor();
        descriptor.setGroupId(groupId);
        descriptor.setArtifactId(artifactId);
        var mojo = new BaseOpenLMojo() {
            @Override
            void execute(String sourcePath, boolean hasDependencies) {
                throw new UnsupportedOperationException("not used in BaseOpenLMojoTest");
            }
        };
        mojo.plugin = descriptor;
        return mojo;
    }

    private static Plugin plugin(String groupId, String artifactId) {
        Plugin p = new Plugin();
        p.setGroupId(groupId);
        p.setArtifactId(artifactId);
        return p;
    }

    private static void touch(Path path) {
        try {
            Files.writeString(path, "<project/>");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
