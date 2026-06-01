package org.openl.rules.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;


/**
 * Generates and attaches a <i>deployment BOM</i> listing every OpenL project under this anchor.
 * <p>
 * Members are the reactor {@code openl}/{@code openl-jar} projects whose basedir sits under this anchor
 * (pom-less projects plus classic OpenL modules). The generated pom shares the anchor's GAV with packaging
 * {@code pom} and is attached via classifier {@code deployment-bom}, so it installs/deploys alongside the
 * anchor.
 * <p>
 * {@code <dependencyManagement>} (canonical BOM, for {@code <scope>import</scope>}) lists every member's main
 * entry plus a {@code deployment}-classifier entry for each member predicted to produce a
 * {@code *-deployment.zip} ({@code PackageMojo} attaches one when a project has an OpenL dependency and
 * non-empty {@code <publishers/>}).
 * <p>
 * {@code <dependencies>} (single-import bundle) lists only deployable members (non-empty
 * {@code <publishers/>}): the deployment entry when they produce one, else the main entry. Members with empty
 * publishers are skipped from the bundle but stay version-managed.
 * <p>
 * Consumer usage:
 * <pre>{@code
 * <dependency>
 *     <groupId>com.example</groupId>
 *     <artifactId>rules-parent</artifactId>
 *     <version>1.0</version>
 *     <classifier>deployment-bom</classifier>
 *     <type>pom</type>
 *     <scope>import</scope>
 * </dependency>
 * }</pre>
 * Skips silently when no OpenL projects sit under this anchor.
 *
 * @author Yury Molchan
 */
@Mojo(name = PrepareDeploymentBomMojo.GOAL_NAME, defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class PrepareDeploymentBomMojo extends AbstractMojo {

    /** Mojo goal name. Referenced by {@code OpenLPomlessParticipant} when auto-binding the execution. */
    public static final String GOAL_NAME = "prepare-bom";

    /** Maven classifier attached to the BOM artifact. Forms part of the consumer-facing coordinates. */
    public static final String DEPLOYMENT_BOM_CLASSIFIER = "deployment-bom";

    /** File name written under {@code target/}. */
    static final String GENERATED_FILE_NAME_SUFFIX = "-deployment-bom.pom";

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Parameter(defaultValue = "${project.build.directory}", required = true, readonly = true)
    private File buildDirectory;

    @Parameter(defaultValue = "${project.build.finalName}", readonly = true, required = true)
    private String finalName;

    @Component
    private MavenProjectHelper projectHelper;

    @Override
    public void execute() throws MojoExecutionException {
        var members = collectOpenLMembers();
        if (members.isEmpty()) {
            getLog().info("No OpenL projects under '" + project.getId() + "'; skipping deployment BOM.");
            return;
        }

        var bom = buildBomModel(members);
        var bomFile = buildDirectory.toPath().resolve(finalName + GENERATED_FILE_NAME_SUFFIX);
        try {
            Files.createDirectories(bomFile.getParent());
            try (var out = Files.newBufferedWriter(bomFile)) {
                new MavenXpp3Writer().write(out, bom);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write deployment BOM to " + bomFile, e);
        }

        projectHelper.attachArtifact(project, "pom", DEPLOYMENT_BOM_CLASSIFIER, bomFile.toFile());
        getLog().info("Attached deployment BOM (" + members.size() + " OpenL projects): " + bomFile);
    }

    /**
     * Returns every reactor project with {@code openl}/{@code openl-jar} packaging whose basedir is
     * located under this anchor's basedir. Ordering is deterministic ({@code groupId} then
     * {@code artifactId}) so the generated BOM is stable across runs.
     */
    private List<MavenProject> collectOpenLMembers() {
        var anchorBasedir = project.getBasedir();
        if (anchorBasedir == null) {
            return List.of();
        }
        var anchorDir = anchorBasedir.toPath().toAbsolutePath().normalize();
        var members = new ArrayList<MavenProject>();
        for (var p : session.getAllProjects()) {
            if (p == project) {
                continue;
            }
            if (!OpenLPackagings.isOpenL(p.getPackaging())) {
                continue;
            }
            var memberBasedir = p.getBasedir();
            if (memberBasedir == null) {
                continue;
            }
            var memberDir = memberBasedir.toPath().toAbsolutePath().normalize();
            if (!memberDir.startsWith(anchorDir)) {
                continue;
            }
            members.add(p);
        }
        members.sort(Comparator
                .comparing(MavenProject::getGroupId)
                .thenComparing(MavenProject::getArtifactId));
        return members;
    }

    /**
     * Builds the BOM model: same GAV as the anchor, packaging {@code pom}.
     * <ul>
     *     <li>{@code <dependencyManagement>} — main entry for every member; deployment-classifier
     *         entry additionally for every member predicted to produce a {@code *-deployment.zip}.
     *         Both forms become version-managed for consumers, regardless of publisher status.</li>
     *     <li>{@code <dependencies>} — deployable members only ({@code rules-deploy.xml} doesn't
     *         declare empty {@code <publishers/>}). For deployable members with OpenL deps, the
     *         deployment-classifier entry; for deployable leaves, the main entry. Members with
     *         empty publishers are skipped from the bundle but stay in
     *         {@code <dependencyManagement>}.</li>
     * </ul>
     */
    private Model buildBomModel(List<MavenProject> members) {
        var model = new Model();
        model.setModelVersion("4.0.0");
        model.setGroupId(project.getGroupId());
        model.setArtifactId(project.getArtifactId());
        model.setVersion(project.getVersion());
        model.setPackaging("pom");
        model.setName(project.getArtifactId() + " (deployment BOM)");

        var dm = new DependencyManagement();
        for (var member : members) {
            dm.addDependency(toMainDependency(member));
            if (willProduceDeploymentArtifact(member)) {
                dm.addDependency(toDeploymentDependency(member));
            }
        }
        model.setDependencyManagement(dm);

        for (var member : members) {
            if (!isDeployable(member)) {
                continue;
            }
            if (hasDeclaredOpenLDep(member)) {
                model.addDependency(toDeploymentDependency(member));
            } else {
                model.addDependency(toMainDependency(member));
            }
        }
        return model;
    }

    /**
     * A member is <i>deployable</i> when its {@code rules-deploy.xml} does NOT declare empty
     * {@code <publishers/>}. Empty publishers is the explicit signal that the project shouldn't
     * ship as a runnable artefact — neither as a {@code *-deployment.zip} (suppressed by
     * {@code PackageMojo}) nor in this BOM's bundle.
     */
    private static boolean isDeployable(MavenProject member) {
        var basedir = member.getBasedir();
        return basedir != null && !OpenLPackagings.hasEmptyPublishers(basedir.toPath());
    }

    /**
     * Mirrors {@code PackageMojo}'s auto-detect rule: a {@code *-deployment.zip} is produced when
     * the project has at least one OpenL dependency AND is deployable (non-empty publishers).
     * Predicted from declared deps (resolved deps aren't available yet — this mojo runs at the
     * anchor's {@code package} phase, before child projects have been built).
     */
    private boolean willProduceDeploymentArtifact(MavenProject member) {
        return isDeployable(member) && hasDeclaredOpenLDep(member);
    }

    /**
     * Heuristic: a declared {@code <dependency>} of type {@code zip} (OpenL convention) OR a
     * {@code jar} dep that points to a reactor sibling whose packaging is {@code openl-jar}.
     * Doesn't catch external (non-reactor) {@code openl-jar} deps but those are vanishingly rare.
     */
    private boolean hasDeclaredOpenLDep(MavenProject member) {
        var deps = member.getDependencies();
        if (deps == null || deps.isEmpty()) {
            return false;
        }
        for (var dep : deps) {
            var type = dep.getType();
            if (OpenLPackagings.ZIP_DEPENDENCY_TYPE.equals(type)) {
                return true;
            }
            if (OpenLPackagings.JAR_DEPENDENCY_TYPE.equals(type) && isReactorOpenLJar(dep)) {
                return true;
            }
        }
        return false;
    }

    private boolean isReactorOpenLJar(Dependency dep) {
        for (var p : session.getAllProjects()) {
            if (OpenLPackagings.OPENL_JAR_PACKAGING.equals(p.getPackaging())
                    && Objects.equals(dep.getGroupId(), p.getGroupId())
                    && Objects.equals(dep.getArtifactId(), p.getArtifactId())) {
                return true;
            }
        }
        return false;
    }

    private static Dependency toMainDependency(MavenProject member) {
        var dep = new Dependency();
        dep.setGroupId(member.getGroupId());
        dep.setArtifactId(member.getArtifactId());
        dep.setVersion(member.getVersion());
        dep.setType(OpenLPackagings.dependencyType(member.getPackaging()));
        return dep;
    }

    private static Dependency toDeploymentDependency(MavenProject member) {
        var dep = toMainDependency(member);
        dep.setClassifier(PackageMojo.DEPLOYMENT_CLASSIFIER);
        return dep;
    }
}
