package org.openl.rules.maven;

import java.nio.file.Path;

import org.apache.maven.model.Dependency;

import org.openl.rules.project.model.RulesDeploy;

/**
 * Single source of truth for OpenL packaging constants and the small pure functions shared between
 * openl-maven-plugin mojos and {@code OpenLPomlessParticipant}.
 * <p>
 * Centralises:
 * <ul>
 *     <li>Packaging type strings — {@link #OPENL_PACKAGING}, {@link #OPENL_JAR_PACKAGING} —
 *         and their corresponding Maven dependency types ({@link #ZIP_DEPENDENCY_TYPE},
 *         {@link #JAR_DEPENDENCY_TYPE}). The same strings appear in
 *         {@code META-INF/plexus/components.xml} — keep them in sync.</li>
 *     <li>{@link #isOpenL(String)} — predicate used to skip OpenL projects when looking for
 *         pom-less anchors and to include them when assembling the deployment BOM.</li>
 *     <li>{@link #dependencyType(String)} — packaging → {@code <type>} mapping for synthesised
 *         {@code <dependency>} entries.</li>
 *     <li>{@link #hasEmptyPublishers(Path)} — empty-{@code <publishers/>} check that
 *         {@code PackageMojo} uses to suppress the {@code *-deployment.zip} attachment and that
 *         {@code PrepareDeploymentBomMojo} uses to predict the same.</li>
 * </ul>
 *
 * @author Yury Molchan
 */
public final class OpenLPackagings {

    /** Maven {@code <packaging>} value for an OpenL rules project (zip-extension artefact). */
    public static final String OPENL_PACKAGING = "openl";

    /** Maven {@code <packaging>} value for an OpenL project that bundles compiled Java alongside rules. */
    public static final String OPENL_JAR_PACKAGING = "openl-jar";

    /** Maven {@code <dependency><type>} value for {@link #OPENL_PACKAGING} artefacts. */
    public static final String ZIP_DEPENDENCY_TYPE = "zip";

    /** Maven {@code <dependency><type>} value for {@link #OPENL_JAR_PACKAGING} artefacts. */
    public static final String JAR_DEPENDENCY_TYPE = "jar";

    /** {@code groupId} of this plugin — used to recognise its declaration in a pom's {@code <build>}. */
    public static final String PLUGIN_GROUP_ID = "org.openl.rules";

    /** {@code artifactId} of this plugin. */
    public static final String PLUGIN_ARTIFACT_ID = "openl-maven-plugin";

    /** {@code groupId} of flatten-maven-plugin — disabled on pom-less projects (no on-disk pom to read). */
    public static final String FLATTEN_GROUP_ID = "org.codehaus.mojo";

    /** {@code artifactId} of flatten-maven-plugin. */
    public static final String FLATTEN_ARTIFACT_ID = "flatten-maven-plugin";

    /** Project property that skips the flatten goal. The switch only exists since {@link #FLATTEN_MIN_VERSION}. */
    public static final String FLATTEN_SKIP_PROPERTY = "flatten.skip";

    /** First flatten-maven-plugin release with a {@code flatten.skip} switch; older versions are bumped to it. */
    public static final String FLATTEN_MIN_VERSION = "1.6.0";

    /** True when the plugin coordinates identify {@code org.openl.rules:openl-maven-plugin}. */
    public static boolean isOpenLPlugin(String groupId, String artifactId) {
        return PLUGIN_GROUP_ID.equals(groupId) && PLUGIN_ARTIFACT_ID.equals(artifactId);
    }

    private OpenLPackagings() {
    }

    /** True when {@code packaging} is one of the OpenL types ({@code openl} or {@code openl-jar}). */
    public static boolean isOpenL(String packaging) {
        return OPENL_PACKAGING.equals(packaging) || OPENL_JAR_PACKAGING.equals(packaging);
    }

    /**
     * Maps OpenL packaging to the Maven dependency type used in synthesised {@code <dependency>}
     * entries: {@code openl} → {@code zip}, {@code openl-jar} → {@code jar}. Returns
     * {@link #ZIP_DEPENDENCY_TYPE} for any non-OpenL packaging so callers can use this as a safe
     * default.
     */
    public static String dependencyType(String packaging) {
        return OPENL_JAR_PACKAGING.equals(packaging) ? JAR_DEPENDENCY_TYPE : ZIP_DEPENDENCY_TYPE;
    }

    /**
     * Parses a {@code rules.xml} {@code <mavenArtifact>} coordinate in Aether's {@code DefaultArtifact}
     * format — {@code <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>} (the version is
     * always the last segment). The {@code <extension>} maps to the dependency {@code <type>} and
     * defaults to {@link #ZIP_DEPENDENCY_TYPE} (the OpenL artefact). Returns {@code null} when the
     * segment count is out of the 3–5 range.
     * <p>
     * The returned {@link Dependency} carries no scope/optional flag — callers decide those (e.g. the
     * synthesiser marks {@code jar} entries {@code <optional>true</>} so downstream OpenL consumers
     * don't inherit a project's bundled Java libs).
     */
    public static Dependency parseMavenArtifact(String coordinates) {
        if (coordinates == null || coordinates.isBlank()) {
            return null;
        }
        var parts = coordinates.trim().split(":");
        if (parts.length < 3 || parts.length > 5) {
            return null;
        }
        var dep = new Dependency();
        dep.setGroupId(parts[0]);
        dep.setArtifactId(parts[1]);
        dep.setVersion(parts[parts.length - 1]); // version is the last segment
        dep.setType(parts.length >= 4 ? parts[2] : ZIP_DEPENDENCY_TYPE);
        if (parts.length == 5) {
            dep.setClassifier(parts[3]);
        }
        return dep;
    }

    /**
     * Returns {@code true} when {@code basedir/rules-deploy.xml} declares an empty
     * {@code <publishers/>} element — the signal {@code PackageMojo} uses to suppress the
     * auto-attached {@code *-deployment.zip} artefact. Returns {@code false} when the file is
     * missing or declares any publisher.
     */
    public static boolean hasEmptyPublishers(Path basedir) {
        var rulesDeploy = RulesDeploy.read(basedir);
        return rulesDeploy != null
                && rulesDeploy.getPublishers() != null
                && rulesDeploy.getPublishers().length == 0;
    }
}
