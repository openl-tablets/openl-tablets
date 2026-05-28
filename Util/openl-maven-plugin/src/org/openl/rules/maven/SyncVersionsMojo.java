package org.openl.rules.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import org.openl.rules.project.model.ProjectDescriptor;

/**
 * Reconciles the {@code <mavenArtifact>} versions declared in a project's {@code rules.xml} with the
 * versions managed by the inherited {@code <dependencyManagement>}.
 * <p>
 * Bound to the {@code validate} phase of the {@code openl}/{@code openl-jar} lifecycles
 * ({@code META-INF/plexus/components.xml}), so it runs at the very start of every OpenL build. For a
 * pom-less project the anchor is wired as {@code <parent>}, so the project's effective
 * {@code <dependencyManagement>} carries the anchor's entries (plus any imported BOMs). This mojo
 * walks every {@code <mavenArtifact>} in {@code rules.xml}; when an entry's {@code groupId:artifactId}
 * is managed, it rewrites the coordinate's version (the last Aether segment) to the managed value.
 * <p>
 * The anchor's {@code <dependencyManagement>} is therefore the single source of truth: bump a version
 * there and the next build aligns every {@code rules.xml} placeholder to it. Entries whose
 * {@code groupId:artifactId} is <i>not</i> managed are left untouched (e.g. a project-local Java lib
 * pinned in {@code rules.xml}). The edit is surgical text replacement — only the version segment of a
 * managed coordinate changes, so the rest of the coordinate, the surrounding XML, comments and
 * formatting are preserved. Idempotent: a {@code rules.xml} already in sync is not rewritten.
 *
 * @author Yury Molchan
 */
@Mojo(name = "sync-versions", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public final class SyncVersionsMojo extends AbstractMojo {

    private static final Pattern MAVEN_ARTIFACT = Pattern.compile("<mavenArtifact>([^<]*)</mavenArtifact>");

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        var basedir = project.getBasedir();
        if (basedir == null) {
            return; // no physical project directory (nothing to sync)
        }
        var rulesXml = basedir.toPath().resolve(ProjectDescriptor.FILE_NAME);
        if (!Files.isRegularFile(rulesXml)) {
            return; // not a rules.xml-bearing folder
        }
        var managed = managedVersions(project);
        if (managed.isEmpty()) {
            return; // nothing to reconcile against
        }
        String original;
        try {
            original = Files.readString(rulesXml);
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot read '" + rulesXml + "'.", e);
        }
        var result = sync(original, managed);
        if (result.changed() == 0) {
            return; // already in sync — leave the file (and the working tree) untouched
        }
        try {
            Files.writeString(rulesXml, result.content());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write '" + rulesXml + "'.", e);
        }
        getLog().info("Synced " + result.changed() + " <mavenArtifact> version(s) in " + rulesXml
                + " from the inherited <dependencyManagement>.");
    }

    /**
     * Effective (inherited-from-anchor) managed versions, keyed by {@code groupId:artifactId}. The
     * synthesised pom-less project has the anchor as {@code <parent>}, so its effective
     * {@code <dependencyManagement>} carries the anchor's entries (and any imported BOMs).
     */
    private static Map<String, String> managedVersions(MavenProject project) {
        var dm = project.getDependencyManagement();
        if (dm == null || dm.getDependencies() == null) {
            return Map.of();
        }
        var map = new HashMap<String, String>();
        for (var dep : dm.getDependencies()) {
            if (dep.getVersion() != null && !dep.getVersion().isBlank()) {
                map.putIfAbsent(dep.getGroupId() + ':' + dep.getArtifactId(), dep.getVersion());
            }
        }
        return map;
    }

    /**
     * Rewrites the version (last Aether segment) of every {@code <mavenArtifact>} whose
     * {@code groupId:artifactId} is managed, leaving the rest of the coordinate (extension,
     * classifier) and the surrounding XML untouched. Pure and side-effect free for unit testing.
     */
    static Result sync(String rulesXml, Map<String, String> managed) {
        var matcher = MAVEN_ARTIFACT.matcher(rulesXml);
        var out = new StringBuilder();
        var changed = 0;
        while (matcher.find()) {
            var synced = syncCoordinate(matcher.group(1).trim(), managed);
            if (synced != null) {
                changed++;
                matcher.appendReplacement(out,
                        Matcher.quoteReplacement("<mavenArtifact>" + synced + "</mavenArtifact>"));
            }
            // Unmanaged / already-synced entries are skipped; appendTail (or the next replacement)
            // copies their original text verbatim.
        }
        matcher.appendTail(out);
        return new Result(out.toString(), changed);
    }

    /**
     * Returns the coordinate with its version segment replaced by the managed version, or {@code null}
     * when the coordinate is malformed, unmanaged, or already in sync.
     */
    private static String syncCoordinate(String coordinates, Map<String, String> managed) {
        var dep = OpenLPackagings.parseMavenArtifact(coordinates);
        if (dep == null) {
            return null;
        }
        var version = managed.get(dep.getGroupId() + ':' + dep.getArtifactId());
        if (version == null || version.equals(dep.getVersion())) {
            return null;
        }
        var parts = coordinates.split(":");
        parts[parts.length - 1] = version; // version is always the last Aether segment
        return String.join(":", parts);
    }

    /** Outcome of a {@link #sync} pass: the (possibly rewritten) content and how many entries changed. */
    record Result(String content, int changed) {
    }
}
