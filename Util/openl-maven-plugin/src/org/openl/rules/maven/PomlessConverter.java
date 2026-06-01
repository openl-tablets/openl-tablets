package org.openl.rules.maven;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Pure analysis of a classic (pom-ful) OpenL project: decides whether deleting its {@code pom.xml} is safe.
 * <p>
 * A project is <b>deletable</b> when everything its {@code pom.xml} declares is reproduced pom-less:
 * coordinates from the folder path, {@code <name>} from {@code rules.xml}, OpenL ({@code zip}) dependencies
 * from {@code rules.xml}, and non-OpenL dependencies hoisted to the anchor.
 * <p>
 * Anything else that affects the build is a <b>blocker</b>: a per-project {@code openl-maven-plugin}
 * {@code <configuration>}/{@code <executions>}, any other plugin, {@code <profiles>}, custom
 * {@code <resources>}/{@code <finalName>}/source dirs, or project-level {@code <properties>}.
 *
 * @author Yury Molchan
 */
final class PomlessConverter {

    private PomlessConverter() {
    }

    /**
     * @param artifactId           project artifactId (for reporting)
     * @param groupId              effective project groupId (input to the {@code flattenGroupId} heuristic)
     * @param pomFile              the {@code pom.xml} that would be deleted
     * @param deletable            {@code true} when {@link #blockers} is empty
     * @param rulesXmlDeps         dependencies that move into this project's own {@code rules.xml} as
     *                             {@code <mavenArtifact>} entries: packaged jars and OpenL siblings,
     *                             distinguished by {@link Dependency#getType()}
     * @param hoistDeps            shared non-OpenL dependencies (provided/tile/pom, …) that move to the anchor
     * @param blockers             reasons the project can't be auto-converted (empty when deletable)
     * @param dependenciesThreshold the {@code <dependenciesThreshold>}, or {@code null}; the anchor takes the
     *                             max across its projects
     */
    record Plan(String artifactId, String groupId, Path pomFile, boolean deletable,
                List<Dependency> rulesXmlDeps, List<Dependency> hoistDeps, List<String> blockers,
                Integer dependenciesThreshold) {
    }

    static Plan analyze(MavenProject project) {
        var model = project.getOriginalModel();
        var blockers = new ArrayList<String>();
        var rulesXmlDeps = new ArrayList<Dependency>();
        var hoist = new ArrayList<Dependency>();

        if (model.getProfiles() != null && !model.getProfiles().isEmpty()) {
            blockers.add("declares <profiles>");
        }
        if (model.getProperties() != null && !model.getProperties().isEmpty()) {
            blockers.add("declares project <properties>");
        }
        if (model.getDistributionManagement() != null) {
            blockers.add("overrides <distributionManagement>");
        }
        if (model.getRepositories() != null && !model.getRepositories().isEmpty()) {
            blockers.add("declares custom <repositories>");
        }

        var threshold = analyzeBuild(model.getBuild(), blockers);

        if (model.getDependencies() != null) {
            var resolvedVersions = effectiveVersions(project);
            for (var raw : model.getDependencies()) {
                if (raw.getExclusions() != null && !raw.getExclusions().isEmpty()) {
                    // <exclusions> can't be expressed in rules.xml or in a hoisted anchor dep (they'd
                    // over-apply across siblings), so a dep with exclusions blocks conversion.
                    blockers.add("dependency '" + raw.getGroupId() + ":" + raw.getArtifactId()
                            + "' declares <exclusions> (non-migratable)");
                    continue;
                }
                var dep = withResolvedVersion(raw, resolvedVersions);
                if (OpenLPackagings.ZIP_DEPENDENCY_TYPE.equals(dep.getType())) {
                    rulesXmlDeps.add(dep); // OpenL sibling → rules.xml as <mavenArtifact>g:a:v</>
                } else if (isPackagedJar(dep)) {
                    rulesXmlDeps.add(dep); // jar bundled into this project's lib/ → its own rules.xml
                } else {
                    hoist.add(dep); // shared, non-packaged (provided/tile/pom, …) → anchor
                }
            }
        }

        return new Plan(project.getArtifactId(), project.getGroupId(), pomPath(project), blockers.isEmpty(),
                rulesXmlDeps, hoist, blockers, threshold);
    }

    /**
     * A jar that the OpenL package step would bundle into the project's {@code lib/}: type {@code jar} (or the
     * default), scope {@code compile}/{@code runtime} (or the default). {@code provided}/{@code test} jars and
     * non-jar types ({@code tile}, {@code pom}, …) are NOT packaged and stay on the anchor.
     */
    private static boolean isPackagedJar(Dependency dep) {
        var type = dep.getType();
        var isJar = type == null || OpenLPackagings.JAR_DEPENDENCY_TYPE.equals(type);
        var scope = dep.getScope();
        var isPackagedScope = scope == null || "compile".equals(scope) || "runtime".equals(scope);
        return isJar && isPackagedScope;
    }

    /**
     * Indexes the project's effective dependencies (after dependency-management injection) by
     * {@code groupId:artifactId:type:classifier}. The raw model may omit {@code <version>} when a parent or
     * BOM supplies it; this backfills the concrete version so the migrator never writes a null coordinate.
     */
    private static Map<String, String> effectiveVersions(MavenProject project) {
        var index = new HashMap<String, String>();
        var effective = project.getDependencies();
        if (effective != null) {
            for (var dep : effective) {
                if (dep.getVersion() != null && !dep.getVersion().isBlank()) {
                    index.putIfAbsent(versionKey(dep), dep.getVersion());
                }
            }
        }
        return index;
    }

    /**
     * Returns {@code dep} with its effective (post-interpolation, post-management) version when that differs
     * from the raw one, else the original. The raw model may carry an uninterpolated {@code ${revision}} that
     * must not reach {@code <mavenArtifact>}. Falls back to the raw declaration when nothing better exists.
     */
    private static Dependency withResolvedVersion(Dependency dep, Map<String, String> effectiveVersions) {
        var effective = effectiveVersions.get(versionKey(dep));
        if (effective != null && !effective.isBlank() && !effective.equals(dep.getVersion())) {
            var copy = dep.clone();
            copy.setVersion(effective);
            return copy;
        }
        return dep;
    }

    private static String versionKey(Dependency dep) {
        var type = dep.getType() == null ? OpenLPackagings.JAR_DEPENDENCY_TYPE : dep.getType();
        var classifier = dep.getClassifier() == null ? "" : dep.getClassifier();
        return dep.getGroupId() + ':' + dep.getArtifactId() + ':' + type + ':' + classifier;
    }

    /** Returns the declared {@code <dependenciesThreshold>} (or {@code null}); appends any blockers found. */
    private static Integer analyzeBuild(Build build, List<String> blockers) {
        if (build == null) {
            return null;
        }
        if (build.getFinalName() != null) {
            blockers.add("custom <finalName>");
        }
        if (build.getSourceDirectory() != null || build.getScriptSourceDirectory() != null
                || build.getTestSourceDirectory() != null) {
            blockers.add("custom source directory");
        }
        if ((build.getResources() != null && !build.getResources().isEmpty())
                || (build.getTestResources() != null && !build.getTestResources().isEmpty())) {
            blockers.add("custom <resources>");
        }
        Integer threshold = null;
        for (var plugin : build.getPlugins()) {
            var t = analyzePlugin(plugin, blockers);
            if (t != null) {
                threshold = t;
            }
        }
        return threshold;
    }

    /**
     * Inspects a plugin entry: returns the {@code openl-maven-plugin} {@code <dependenciesThreshold>} (or
     * {@code null}) and appends blockers for anything that can't go pom-less.
     * <p>
     * {@code <dependenciesThreshold>} is the only config key that doesn't block (it's reconciled to the
     * anchor max). Any other child (skipITs, deploymentName, …) blocks — those have no safe anchor-wide value.
     */
    private static Integer analyzePlugin(Plugin plugin, List<String> blockers) {
        if (!OpenLPackagings.isOpenLPlugin(plugin.getGroupId(), plugin.getArtifactId())) {
            blockers.add("declares extra plugin '" + plugin.getArtifactId() + "'");
            return null;
        }
        if (plugin.getExecutions() != null && !plugin.getExecutions().isEmpty()) {
            blockers.add("openl-maven-plugin has <executions>");
        }
        if (!(plugin.getConfiguration() instanceof Xpp3Dom config)) {
            return null;
        }
        Integer threshold = null;
        for (var child : config.getChildren()) {
            var name = child.getName();
            if (PackageMojo.DEPENDENCIES_THRESHOLD_PARAM.equals(name)) {
                threshold = parseThreshold(child.getValue(), blockers);
            } else if (PackageMojo.DEPLOYMENT_PACKAGE_PARAM.equals(name)) {
                // Deprecated no-op — deployment artefacts are auto-discovered now. Safe to drop; doesn't block.
                continue;
            } else {
                // Per-project config with no portable anchor-wide value.
                blockers.add("openl-maven-plugin <configuration> has non-portable <" + name + ">");
            }
        }
        return threshold;
    }

    private static Integer parseThreshold(String value, List<String> blockers) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            blockers.add("openl-maven-plugin <dependenciesThreshold> is not an integer: '" + value.trim() + "'");
            return null;
        }
    }

    private static Path pomPath(MavenProject project) {
        var file = project.getFile();
        return file == null ? null : file.toPath();
    }
}
