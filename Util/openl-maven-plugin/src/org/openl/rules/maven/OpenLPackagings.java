package org.openl.rules.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.MavenProject;
import org.jspecify.annotations.Nullable;

import org.openl.rules.project.model.RulesDeploy;

/**
 * Shared OpenL packaging constants and pure helpers used by the openl-maven-plugin mojos and
 * {@code OpenLPomlessParticipant}: the {@code openl}/{@code openl-jar} packaging strings and their Maven
 * dependency types, the {@link #isOpenL(String)} predicate, the {@link #dependencyType(String)} mapping, and
 * the {@link #hasEmptyPublishers(Path)} check.
 * <p>
 * The packaging strings also appear in {@code META-INF/plexus/components.xml} — keep them in sync.
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

    /** File name of the materialised install/deploy pom for a pom-less OpenL project (lives in {@code target/}). */
    public static final String INSTALL_POM_FILE_NAME = "openl-pom.xml";

    /**
     * Writes a pom-less project's install/deploy pom into {@code targetDir}: the raw model stripped of
     * {@code <parent>}, {@code <build>}, and the transient {@code flatten.skip} property. Returns the written
     * file. Idempotent — overwrites any previous content.
     */
    public static Path materialiseInstallPom(Model rawModel, Path targetDir) throws IOException {
        var minimal = rawModel.clone();
        minimal.setBuild(null);
        minimal.setParent(null);
        if (minimal.getProperties() != null) {
            minimal.getProperties().remove(FLATTEN_SKIP_PROPERTY);
        }
        Files.createDirectories(targetDir);
        var pomFile = targetDir.resolve(INSTALL_POM_FILE_NAME);
        try (var writer = Files.newBufferedWriter(pomFile)) {
            new MavenXpp3Writer().write(writer, minimal);
        }
        return pomFile;
    }

    /** True when the plugin coordinates identify {@code org.openl.rules:openl-maven-plugin}. */
    public static boolean isOpenLPlugin(String groupId, String artifactId) {
        return PLUGIN_GROUP_ID.equals(groupId) && PLUGIN_ARTIFACT_ID.equals(artifactId);
    }

    /** True when the plugin coordinates identify {@code org.codehaus.mojo:flatten-maven-plugin}. */
    public static boolean isFlattenPlugin(String groupId, String artifactId) {
        return FLATTEN_GROUP_ID.equals(groupId) && FLATTEN_ARTIFACT_ID.equals(artifactId);
    }

    private OpenLPackagings() {
    }

    /** True when {@code packaging} is one of the OpenL types ({@code openl} or {@code openl-jar}). */
    public static boolean isOpenL(String packaging) {
        return OPENL_PACKAGING.equals(packaging) || OPENL_JAR_PACKAGING.equals(packaging);
    }

    /** The {@code groupId:artifactId} key used to index OpenL artefacts across the reactor. */
    public static String ga(String groupId, String artifactId) {
        return groupId + ':' + artifactId;
    }

    /**
     * Maps every OpenL artefact in {@code projects} to its version, keyed by {@code groupId:artifactId} (first
     * occurrence wins). The returned {@link HashMap} is mutable so callers can fold in further entries.
     */
    public static Map<String, String> reactorOpenLVersions(Collection<MavenProject> projects) {
        var map = new HashMap<String, String>();
        for (var p : projects) {
            if (isOpenL(p.getPackaging())) {
                map.putIfAbsent(ga(p.getGroupId(), p.getArtifactId()), p.getVersion());
            }
        }
        return map;
    }

    /**
     * Maps OpenL packaging to its Maven dependency type: {@code openl} → {@code zip}, {@code openl-jar} →
     * {@code jar}. Returns {@link #ZIP_DEPENDENCY_TYPE} for any other packaging.
     */
    public static String dependencyType(String packaging) {
        return OPENL_JAR_PACKAGING.equals(packaging) ? JAR_DEPENDENCY_TYPE : ZIP_DEPENDENCY_TYPE;
    }

    /**
     * Parses a {@code <mavenArtifact>} coordinate in Aether's {@code DefaultArtifact} format —
     * {@code groupId:artifactId[:extension[:classifier]]:version} (version is always last). The extension maps
     * to the dependency {@code <type>} and defaults to {@link #ZIP_DEPENDENCY_TYPE}. Returns {@code null} when
     * the segment count is outside 3–5.
     * <p>
     * The returned {@link Dependency} has no scope/optional flag — callers set those.
     */
    public static @Nullable Dependency parseMavenArtifact(@Nullable String coordinates) {
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
     * Returns {@code true} when {@code basedir/rules-deploy.xml} declares an empty {@code <publishers/>} —
     * the signal to suppress the auto-attached {@code *-deployment.zip}. Returns {@code false} when the file
     * is missing or declares any publisher.
     */
    public static boolean hasEmptyPublishers(Path basedir) {
        var rulesDeploy = RulesDeploy.read(basedir);
        return rulesDeploy != null
                && rulesDeploy.getPublishers() != null
                && rulesDeploy.getPublishers().length == 0;
    }
}
