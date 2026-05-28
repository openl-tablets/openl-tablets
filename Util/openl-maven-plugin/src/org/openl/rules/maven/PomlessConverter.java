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
 * Pure analysis of a classic (pom-ful) OpenL project, deciding whether it can be safely converted to
 * the pom-less form by simply deleting its {@code pom.xml}.
 * <p>
 * A project is <b>deletable</b> when everything its raw {@code pom.xml} declares is reproduced by the
 * pom-less participant + anchor inheritance:
 * <ul>
 *     <li>coordinates — re-derived from the folder path under the anchor;</li>
 *     <li>{@code <name>} — re-read from {@code rules.xml};</li>
 *     <li>OpenL ({@code zip}-type) dependencies — re-derived from {@code rules.xml} {@code <dependency>} entries;</li>
 *     <li>non-OpenL dependencies — <i>hoistable</i>: they must be moved to the anchor (reported, not auto-applied).</li>
 * </ul>
 * Anything that changes the build when removed and can't be reproduced per-project pom-less is a
 * <b>blocker</b>: a per-project {@code openl-maven-plugin} {@code <configuration>}/{@code <executions>},
 * any other plugin, {@code <profiles>}, custom {@code <resources>}/{@code <finalName>}/source dirs, or
 * project-level {@code <properties>}.
 *
 * @author Yury Molchan
 */
final class PomlessConverter {

    private PomlessConverter() {
    }

    /**
     * @param artifactId           project artifactId (for reporting)
     * @param groupId              project groupId (effective — used by the migrator's flattenGroupId heuristic
     *                             to decide whether the pom-less leaf's derived groupId would match the original)
     * @param pomFile              the {@code pom.xml} that would be deleted
     * @param deletable            {@code true} when {@link #blockers} is empty
     * @param rulesXmlDeps         dependencies that move into this project's own {@code rules.xml} as
     *                             {@code <mavenArtifact>} entries. Two flavours: packaged jars
     *                             ({@code <mavenArtifact>g:a:jar:v</>}, bundled into {@code lib/}; the
     *                             synthesiser marks them optional) and OpenL siblings
     *                             ({@code <mavenArtifact>g:a:v</>}, merged into the matching existing
     *                             {@code <dependency><name>artifactId</name></dependency>} so the GAV
     *                             becomes explicit alongside the OpenL {@code <name>}). Distinguished
     *                             by {@link Dependency#getType()}.
     * @param hoistDeps            shared non-OpenL dependencies (provided/tile/pom, …) that move to the anchor
     * @param blockers             human-readable reasons the project can't be auto-converted (empty when deletable)
     * @param dependenciesThreshold the project's openl-maven-plugin {@code <dependenciesThreshold>}, or {@code null}
     *                             when not declared. The mojo sets the anchor's threshold to the max across all
     *                             projects (the most permissive value never fails a stricter project's build).
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
                    // <exclusions> can't be expressed in rules.xml's <mavenArtifact> or in a hoisted anchor dep
                    // (where they'd over-apply across siblings), so a dep with exclusions blocks conversion —
                    // dropping them would silently change the project's transitive resolution.
                    blockers.add("dependency '" + raw.getGroupId() + ":" + raw.getArtifactId()
                            + "' declares <exclusions> (non-migratable)");
                    continue;
                }
                var dep = withResolvedVersion(raw, resolvedVersions);
                if (OpenLPackagings.ZIP_DEPENDENCY_TYPE.equals(dep.getType())) {
                    // OpenL sibling — merged into rules.xml as <mavenArtifact>g:a:v</> alongside the
                    // matching <name>artifactId</name> (when present), so the GAV is explicit and not
                    // dependent on reactor name-resolution.
                    rulesXmlDeps.add(dep);
                } else if (isPackagedJar(dep)) {
                    // Project-specific jar bundled into this project's lib/ → goes to its own rules.xml
                    // as <mavenArtifact>…:jar</mavenArtifact> (the synthesiser marks it optional).
                    rulesXmlDeps.add(dep);
                } else {
                    // Shared, non-packaged (provided/tile/pom, …) → anchor.
                    hoist.add(dep);
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
     * Indexes the project's <i>effective</i> dependencies (after Maven's dependency-management injection)
     * by {@code groupId:artifactId:type:classifier}. The raw {@code pom.xml} entries from
     * {@link MavenProject#getOriginalModel()} may omit {@code <version>} when a parent pom or an imported
     * BOM supplies it; this index backfills the concrete version so the migrator never writes a
     * {@code null} coordinate into {@code rules.xml} or the anchor.
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
     * Prefers the <i>effective</i> version (post-interpolation, post-management) over the raw declaration.
     * The raw model may carry {@code ${revision}} or another property placeholder that's still uninterpolated
     * — emitting it verbatim into {@code <mavenArtifact>} would (a) write garbage into the rules.xml and
     * (b) break {@code String.replaceFirst} which treats {@code $} as a backreference. Falls back to the raw
     * declaration when the effective model offers nothing better.
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
     * Inspects a plugin entry. Returns the openl-maven-plugin {@code <dependenciesThreshold>} value when present
     * (or {@code null}); appends blockers for anything that can't go pom-less.
     * <p>
     * {@code <dependenciesThreshold>} is the one configuration key that does NOT block: it's reconciled on the
     * anchor as the max across all projects. Any other configuration child (skipITs, deploymentName, …) still
     * blocks, because those vary per project with no safe anchor-wide value.
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
