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
 * Builds the Maven {@link Model} for a pom-less OpenL project: path-derived GAV, {@code packaging=openl},
 * the anchor as {@code <parent>} (for natural inheritance of its {@code <build>}, {@code <properties>},
 * {@code <dependencyManagement>}, etc.), the optional {@code <name>}, and the {@code rules.xml} dependencies
 * translated to Maven dependencies.
 * <p>
 * Each dependency comes from its {@code <mavenArtifact>} when given, else from resolving {@code <name>}
 * against the name → coordinates index. Version precedence: anchor {@code <dependencyManagement>} wins (the
 * explicit opt-out to pin a published version), else a coordinate pointing at a reactor OpenL sibling takes
 * that sibling's reactor version (a reactor module resolves only at its reactor version), else the literal
 * version. Management is applied here at synth time because the translated deps carry an explicit
 * {@code <version>} that Maven's own management resolution would skip.
 * <p>
 * The {@code <parent>} uses an empty {@code <relativePath/>} to force reactor/repo lookup. The participant
 * adds a transient {@code <build>} extension stub; {@code PrepareRepositoryPomMojo} strips {@code <parent>}
 * and {@code <build>} from the installed pom, so consumers see a flat artefact.
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
            // A jar is a self-contained lib in the project's lib/: <optional>true</> stops downstream OpenL
            // consumers from inheriting it, and it keeps its literal version (never a reactor sibling).
            if (OpenLPackagings.JAR_DEPENDENCY_TYPE.equals(explicit.getType())) {
                explicit.setOptional(true);
                return explicit;
            }
            // A coordinate pointing at a reactor OpenL sibling takes the reactor version over whatever
            // rules.xml declares (a placeholder or stale literal) — a module resolves only at its reactor version.
            if (reactorVersions != null) {
                var reactorVersion = reactorVersions.get(explicit.getGroupId() + ':' + explicit.getArtifactId());
                if (reactorVersion != null) {
                    explicit.setVersion(reactorVersion);
                }
            }
            return explicit;
        }
        if (mavenArtifact != null && !mavenArtifact.isBlank()) {
            // A declared-but-unparseable <mavenArtifact> fails the build rather than silently falling back
            // to <name> resolution, which would hide the typo and ship a project missing the dependency.
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
        // Siblings share the project version; use it literally so the installed pom holds a concrete version,
        // not a ${project.version} placeholder consumers won't reinterpolate.
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
