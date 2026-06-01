package org.openl.rules.maven.extension;

import java.util.Map;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.project.MavenProject;

import org.openl.rules.maven.OpenLPackagings;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;

/**
 * Builds the Maven {@link Model} for a pom-less OpenL project: GAV derived from the path,
 * {@code packaging=openl}, the anchor as {@code <parent>} (so the project inherits the anchor's
 * {@code <build>}, {@code <pluginManagement>}, {@code <properties>}, {@code <dependencyManagement>},
 * {@code <distributionManagement>}, and so on through natural Maven inheritance), optional
 * {@code <name>}, and {@code rules.xml} dependencies translated to Maven dependencies (using
 * {@code <mavenArtifact>} when given, otherwise resolving {@code <name>} against the supplied
 * name → coordinates index). A {@code <mavenArtifact>} that points at an OpenL artefact already in
 * the reactor takes that artefact's reactor version, overriding the (possibly placeholder) version
 * written in {@code rules.xml} — a reactor module resolves only at its reactor version. When the
 * anchor declares {@code <dependencyManagement>}, the synthesiser explicitly overrides matching
 * dependency versions at synth time (after the reactor-version step, so management can still pin a
 * sibling GA to a published version on purpose) — needed because the translated dependencies carry
 * an explicit {@code <version>}, which Maven's own management resolution would otherwise skip.
 * <p>
 * The synthesised model carries {@code <parent>} with an empty {@code <relativePath/>} (forces
 * reactor/repo lookup since the anchor is always in the session at synth time). The participant
 * additionally decorates the model with a transient
 * {@code <build><plugin openl-maven-plugin extensions=true></build>} so ModelBuilder loads the
 * extension realm during the in-memory build. {@code PrepareRepositoryPomMojo} strips
 * {@code <parent>} and {@code <build>} when materialising the installed pom, so consumers see a
 * flat artefact pom without any anchor lineage.
 *
 * @author Yury Molchan
 */
final class OpenLModelSynthesizer {

    private OpenLModelSynthesizer() {
    }

    static Model synthesize(OpenLCoordinates coordinates,
                            String version,
                            ProjectDescriptor descriptor,
                            Map<String, OpenLCoordinates> names,
                            Map<String, Dependency> dependencyManagement,
                            MavenProject anchor,
                            Map<String, String> reactorVersions) {
        var model = new Model();
        model.setModelVersion("4.0.0");
        model.setGroupId(coordinates.groupId());
        model.setArtifactId(coordinates.artifactId());
        model.setVersion(version);
        model.setPackaging(OpenLPackagings.OPENL_PACKAGING);
        if (anchor != null) {
            // Wire the anchor as parent so ModelBuilder applies natural Maven inheritance —
            // anchor's <build><plugins>, <pluginManagement>, <properties>, etc. land in the
            // pom-less project's effective model. PrepareRepositoryPomMojo strips <parent> from
            // the installed pom so consumers still see a flat artefact. Empty <relativePath/>
            // forces repo/reactor lookup (the anchor is always in-session at synth time).
            var parent = new Parent();
            parent.setGroupId(anchor.getGroupId());
            parent.setArtifactId(anchor.getArtifactId());
            parent.setVersion(anchor.getVersion());
            parent.setRelativePath("");
            model.setParent(parent);
        }
        if (descriptor == null) {
            return model;
        }
        if (descriptor.getName() != null && !descriptor.getName().isBlank()) {
            model.setName(descriptor.getName().trim());
        }
        if (descriptor.getDependencies() != null) {
            for (var dep : descriptor.getDependencies()) {
                var maven = toMavenDependency(dep, version, names, reactorVersions);
                if (maven != null) {
                    applyManagement(maven, dependencyManagement);
                    model.addDependency(maven);
                }
            }
        }
        return model;
    }

    private static Dependency toMavenDependency(ProjectDependencyDescriptor dep,
                                                String projectVersion,
                                                Map<String, OpenLCoordinates> names,
                                                Map<String, String> reactorVersions) {
        var mavenArtifact = dep.getMavenArtifact();
        var explicit = OpenLPackagings.parseMavenArtifact(mavenArtifact);
        if (explicit != null) {
            // Java libs declared via <mavenArtifact>g:a:jar:v</mavenArtifact> are self-contained inside
            // the OpenL project's lib/ — the package filter still includes them in this project's zip
            // (optional is not checked there), but <optional>true</> stops downstream OpenL consumers
            // from inheriting the jar (and its transitives) through this project's installed pom. The
            // OpenL zip type stays non-optional: sibling OpenL projects must remain visible to consumers.
            // A jar is never an OpenL reactor sibling, so it keeps the literal version from rules.xml.
            if (OpenLPackagings.JAR_DEPENDENCY_TYPE.equals(explicit.getType())) {
                explicit.setOptional(true);
                return explicit;
            }
            // When the coordinate points at an OpenL artefact that is itself in the reactor, the reactor
            // version wins over whatever rules.xml declares — a placeholder (e.g. 000000) or a stale
            // literal. A reactor module resolves only at its reactor version; any other version misses
            // the reactor's exact-GAV index and falls through to the remote repositories.
            if (reactorVersions != null) {
                var reactorVersion = reactorVersions.get(explicit.getGroupId() + ':' + explicit.getArtifactId());
                if (reactorVersion != null) {
                    explicit.setVersion(reactorVersion);
                }
            }
            return explicit;
        }
        if (mavenArtifact != null && !mavenArtifact.isBlank()) {
            // A <mavenArtifact> was declared but doesn't parse. Fail the build deterministically rather
            // than silently falling back to <name> resolution (or dropping the dependency), which would
            // hide the typo and ship a project missing a dependency the author clearly intended.
            throw new IllegalArgumentException("Invalid <mavenArtifact> coordinate '" + mavenArtifact.trim()
                    + "' on OpenL dependency '" + dep.getName()
                    + "'. Expected Aether format groupId:artifactId[:type[:classifier]]:version.");
        }
        var name = dep.getName();
        if (name == null || name.isBlank() || names == null) {
            return null;
        }
        var coords = names.get(name.trim());
        if (coords == null) {
            return null;
        }
        var maven = new Dependency();
        maven.setGroupId(coords.groupId());
        maven.setArtifactId(coords.artifactId());
        // Use the literal project version (siblings share it) so the installed pom holds a
        // concrete version rather than a ${project.version} placeholder that downstream
        // consumers' Maven won't reinterpolate.
        maven.setVersion(projectVersion);
        maven.setType(OpenLPackagings.ZIP_DEPENDENCY_TYPE);
        return maven;
    }

    /**
     * If the anchor's {@code <dependencyManagement>} has an entry for {@code dep}'s GA, override
     * the dependency's version with the managed one. Type, classifier and other attributes stay as
     * the {@code <mavenArtifact>} (or name-resolved sibling) defined them.
     */
    private static void applyManagement(Dependency dep, Map<String, Dependency> management) {
        if (management == null) {
            return;
        }
        var managed = management.get(dep.getGroupId() + ':' + dep.getArtifactId());
        if (managed != null && managed.getVersion() != null && !managed.getVersion().isBlank()) {
            dep.setVersion(managed.getVersion());
        }
    }
}
