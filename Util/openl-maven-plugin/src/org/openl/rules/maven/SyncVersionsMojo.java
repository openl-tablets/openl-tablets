package org.openl.rules.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import org.openl.rules.project.model.ProjectDescriptor;

/**
 * Reconciles the {@code <mavenArtifact>} versions in a project's {@code rules.xml} with the versions resolved
 * from the build — the inherited {@code <dependencyManagement>} and every OpenL artefact in the reactor.
 * <p>
 * Bound to the {@code validate} phase, so it runs after the participant has added the pom-less projects (each
 * with the anchor as {@code <parent>}, so its effective {@code <dependencyManagement>} carries the anchor's
 * entries). Version precedence per coordinate: management wins (the explicit opt-out to pin a published
 * version), else an OpenL coordinate whose {@code groupId:artifactId} is a reactor artefact takes the reactor
 * version. {@code jar} coordinates follow management only — they're bundled libs, never reactor siblings. This
 * mirrors {@code OpenLModelSynthesizer}, keeping the on-disk {@code rules.xml} aligned with the build.
 * <p>
 * Coordinates that are neither managed nor reactor artefacts are left untouched. Only the version segment is
 * rewritten, so the rest of the file is preserved. Idempotent: a file already in sync is not rewritten.
 *
 * @author Yury Molchan
 */
@Mojo(name = "sync-versions", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public final class SyncVersionsMojo extends AbstractMojo {

    private static final Pattern MAVEN_ARTIFACT = Pattern.compile("<mavenArtifact>([^<]*)</mavenArtifact>");

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

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
        var reactor = OpenLPackagings.reactorOpenLVersions(session.getAllProjects());
        if (managed.isEmpty() && reactor.isEmpty()) {
            return; // nothing to reconcile against
        }
        String original;
        try {
            original = Files.readString(rulesXml);
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot read '" + rulesXml + "'.", e);
        }
        var result = sync(original, managed, reactor);
        if (result.changed() == 0) {
            return; // already in sync — leave the file (and the working tree) untouched
        }
        try {
            Files.writeString(rulesXml, result.content());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write '" + rulesXml + "'.", e);
        }
        getLog().info("Synced " + result.changed() + " <mavenArtifact> version(s) in " + rulesXml + ".");
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
                map.putIfAbsent(OpenLPackagings.ga(dep.getGroupId(), dep.getArtifactId()), dep.getVersion());
            }
        }
        return map;
    }

    /**
     * Rewrites the version (last Aether segment) of every {@code <mavenArtifact>} whose
     * {@code groupId:artifactId} is managed, leaving the rest of the coordinate (extension,
     * classifier) and the surrounding XML untouched. Pure and side-effect free for unit testing.
     */
    static Result sync(String rulesXml, Map<String, String> managed, Map<String, String> reactor) {
        var matcher = MAVEN_ARTIFACT.matcher(rulesXml);
        var out = new StringBuilder();
        var changed = 0;
        while (matcher.find()) {
            var synced = syncCoordinate(matcher.group(1).trim(), managed, reactor);
            if (synced != null) {
                changed++;
                matcher.appendReplacement(out,
                        Matcher.quoteReplacement("<mavenArtifact>" + synced + "</mavenArtifact>"));
            }
            // Unresolved / already-synced entries are skipped; appendTail (or the next replacement)
            // copies their original text verbatim.
        }
        matcher.appendTail(out);
        return new Result(out.toString(), changed);
    }

    /**
     * Returns the coordinate with its version segment replaced by the resolved version, or {@code null}
     * when the coordinate is malformed, unresolved, or already in sync. The inherited management wins;
     * otherwise a non-{@code jar} coordinate whose {@code groupId:artifactId} is a reactor OpenL
     * artefact takes the reactor version.
     */
    private static String syncCoordinate(String coordinates, Map<String, String> managed,
                                         Map<String, String> reactor) {
        var dep = OpenLPackagings.parseMavenArtifact(coordinates);
        if (dep == null) {
            return null;
        }
        var ga = OpenLPackagings.ga(dep.getGroupId(), dep.getArtifactId());
        var version = managed.get(ga);
        if (version == null && !OpenLPackagings.JAR_DEPENDENCY_TYPE.equals(dep.getType())) {
            version = reactor.get(ga);
        }
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
