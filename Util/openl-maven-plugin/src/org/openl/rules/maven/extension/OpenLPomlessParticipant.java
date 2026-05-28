package org.openl.rules.maven.extension;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.graph.DefaultProjectDependencyGraph;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.rules.maven.OpenLPackagings;
import org.openl.rules.maven.PrepareDeploymentBomMojo;
import org.openl.rules.maven.PrepareRepositoryPomMojo;
import org.openl.rules.project.model.ProjectDescriptor;

/**
 * Adds OpenL projects with no {@code pom.xml} to the reactor.
 * <p>
 * Activated when any reactor pom declares {@code openl-maven-plugin} with
 * {@code <extensions>true</extensions>}. Every reactor project that <i>directly</i> declares the
 * plugin (and isn't itself an OpenL packaging) acts as its own anchor: its basedir is scanned for
 * {@code rules.xml} folders, its groupId is the base for derived coordinates, and its plugin
 * {@code <configuration>} feeds the {@code flattenGroupId} flag. Multiple anchors coexist in the
 * same build — the scanner won't descend past any folder containing a {@code pom.xml}, so nested
 * anchors don't double-scan the same subtree.
 * <p>
 * For each discovered folder the participant synthesises a Maven {@link Model} (see
 * {@link OpenLModelSynthesizer}) with the anchor wired as {@code <parent>} — so the pom-less
 * project's effective model inherits the anchor's {@code <build>}, {@code <pluginManagement>},
 * {@code <properties>}, {@code <dependencyManagement>}, {@code <distributionManagement>} and the
 * rest through standard Maven semantics. Additional build plugins declared on the anchor (e.g. a
 * source generator bound to {@code generate-sources}) therefore fire on every pom-less project as
 * well; mark them {@code <inherited>false</inherited>} on the anchor to opt out. The model is run
 * through {@code ProjectBuilder} via an in-memory {@link InMemoryModelSource}, {@code project.setFile}
 * is pointed at the project's {@code rules.xml} so basedir resolves correctly, and the resulting
 * projects are added to the reactor in topological order. <b>No synthesised pom is ever written
 * to the project root.</b> The XML representation needed by install/deploy is generated to
 * {@code target/openl-pom.xml} by {@code openl:prepare-pom}, which strips {@code <parent>} and
 * {@code <build>} so the installed pom is flat.
 * <p>
 * Classic OpenL projects ({@code <packaging>openl</packaging>} alongside a real {@code pom.xml})
 * are untouched.
 *
 * @author Yury Molchan
 */
public class OpenLPomlessParticipant extends AbstractMavenLifecycleParticipant {

    private static final String POM_XML = "pom.xml";
    private static final String TARGET_DIR = "target";

    /**
     * Session-data key marking that the participant has already injected pom-less projects in this
     * session. Maven calls {@code afterProjectsRead} once per extension realm in which this class
     * is loaded, so when multiple reactor poms declare {@code openl-maven-plugin} with
     * {@code <extensions>true</extensions>} the participant otherwise runs more than once in the
     * same session — repeating the scan and tripping {@code DuplicateProjectException} on the
     * second add.
     */
    private static final String SESSION_DONE_KEY = OpenLPomlessParticipant.class.getName() + ".done";

    private static final Logger LOG = LoggerFactory.getLogger(OpenLPomlessParticipant.class);

    /** Injected by Plexus via {@code components.xml}. */
    private ProjectBuilder projectBuilder;

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        var sessionData = session.getRepositorySession().getData();
        if (sessionData.get(SESSION_DONE_KEY) != null) {
            return;
        }
        sessionData.set(SESSION_DONE_KEY, Boolean.TRUE);

        var anchors = findAnchors(session);
        if (anchors.isEmpty()) {
            return;
        }

        // Auto-bind the deployment-BOM execution on every anchor (covers both pom-less and
        // classic OpenL subtrees). The mojo skips silently when an anchor has no OpenL members.
        for (var anchor : anchors) {
            bindDeploymentBomExecution(anchor);
        }

        var request = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
        // Disable transitive resolution: sibling artefacts aren't in the local repo yet and a
        // failed lookup here would poison Aether's negative-resolution cache for the real lookup
        // during compile.
        request.setResolveDependencies(false);

        // Belt-and-braces dedup: skip any synthesised project whose GAV already sits in the
        // reactor (e.g. a parallel anchor declared the same coordinates). Cheap to compute and
        // makes the participant safe to re-run in tooling that re-fires the lifecycle.
        var existing = new HashSet<String>();
        for (var p : session.getAllProjects()) {
            existing.add(gav(p.getGroupId(), p.getArtifactId(), p.getVersion()));
        }
        var added = new ArrayList<MavenProject>();
        for (var anchor : anchors) {
            for (var p : discoverUnder(anchor, session, request)) {
                if (existing.add(gav(p.getGroupId(), p.getArtifactId(), p.getVersion()))) {
                    added.add(p);
                }
            }
        }
        if (added.isEmpty()) {
            return;
        }

        var all = new ArrayList<>(session.getAllProjects());
        all.addAll(added);
        try {
            var graph = new DefaultProjectDependencyGraph(all);
            var sorted = graph.getSortedProjects();
            session.setAllProjects(sorted);
            session.setProjects(sorted);
            session.setProjectDependencyGraph(graph);
        } catch (CycleDetectedException | DuplicateProjectException e) {
            throw new MavenExecutionException(
                    "Failed to resort reactor after adding pom-less OpenL projects.", e);
        }
        // Maven 3.9.x ReactorReader snapshots session.getProjects() once in its constructor and never
        // refreshes — without this poke a sibling war module's dependency resolution can't see the
        // pom-less zip and falls through to the remote repositories. See ReactorReaderInjector.
        ReactorReaderInjector.inject(session, added);
    }

    /**
     * Returns every reactor project that declares {@code openl-maven-plugin} in its own raw
     * {@code <build><plugins>} and isn't an OpenL packaging itself. Inherited declarations and
     * {@code <pluginManagement>}-only entries don't count — the latter never bind the plugin to
     * the build, so they never execute. Each returned project acts as an independent anchor.
     */
    private static List<MavenProject> findAnchors(MavenSession session) {
        var result = new ArrayList<MavenProject>();
        for (var p : session.getProjects()) {
            if (OpenLPackagings.isOpenL(p.getPackaging())) {
                continue;
            }
            if (findOpenLPluginInBuildPlugins(p.getOriginalModel()) == null) {
                continue;
            }
            result.add(p);
        }
        return result;
    }

    /**
     * Scans a single anchor's basedir, stages each discovered {@code rules.xml} folder, and runs
     * each through {@code ProjectBuilder}. Returns the synthesised projects ready to be added to
     * the reactor.
     */
    private List<MavenProject> discoverUnder(MavenProject anchor, MavenSession session,
                                             ProjectBuildingRequest request)
            throws MavenExecutionException {
        var baseGroupId = anchor.getGroupId();
        if (baseGroupId == null || baseGroupId.isBlank()) {
            throw new MavenExecutionException(
                    "Anchor pom '" + anchor.getId() + "' must declare a <groupId>; it is used as"
                            + " the base for pom-less OpenL projects.",
                    anchor.getFile());
        }

        var anchorDir = anchor.getBasedir().toPath().toAbsolutePath().normalize();
        List<Path> folders;
        try {
            folders = scan(anchorDir);
        } catch (IOException e) {
            throw new MavenExecutionException(
                    "Failed to scan for pom-less OpenL projects under '" + anchorDir + "'.", e);
        }
        if (folders.isEmpty()) {
            return List.of();
        }

        var anchorPlugin = findOpenLPlugin(anchor.getModel());
        var flattenGroupId = resolveFlattenGroupId(session, anchorPlugin);

        // Pass 1 — read descriptors, compute coordinates, build the name → coordinates index.
        var staged = new ArrayList<Staged>(folders.size());
        var names = new HashMap<String, OpenLCoordinates>();
        for (var folder : folders) {
            var descriptor = ProjectDescriptor.read(folder);
            if (descriptor == null) {
                LOG.warn("Skipping pom-less OpenL project '{}': unable to read rules.xml.", folder);
                continue;
            }
            var coords = OpenLCoordinates.of(anchorDir, folder, baseGroupId, flattenGroupId);
            staged.add(new Staged(folder, coords, descriptor));
            if (descriptor.getName() != null && !descriptor.getName().isBlank()) {
                names.putIfAbsent(descriptor.getName().trim(), coords);
            }
            names.putIfAbsent(coords.artifactId(), coords);
        }
        if (staged.isEmpty()) {
            return List.of();
        }

        var openlPluginVersion = anchorPlugin == null ? null : anchorPlugin.getVersion();
        var dependencyManagement = collectDependencyManagement(anchor.getModel());
        var version = anchor.getVersion();
        var built = new ArrayList<MavenProject>(staged.size());
        for (var s : staged) {
            try {
                built.add(buildPomlessProject(s, version, openlPluginVersion, names,
                        dependencyManagement, anchor, request));
            } catch (IOException | ProjectBuildingException e) {
                throw new MavenExecutionException(
                        "Failed to build pom-less OpenL project at '" + s.folder + "'.", e);
            }
            LOG.info("Discovered pom-less OpenL project '{}:{}' at '{}'.",
                    s.coordinates.groupId(), s.coordinates.artifactId(), s.folder);
        }
        return built;
    }

    /**
     * Synthesises a single pom-less {@link MavenProject}. {@code request.setResolveDependencies(false)}
     * keeps {@code ProjectBuilder} from calling Aether on sibling artefacts that aren't installed
     * yet — the real resolution happens later in the per-project lifecycle.
     */
    private MavenProject buildPomlessProject(Staged s, String version, String openlPluginVersion,
                                             Map<String, OpenLCoordinates> names,
                                             Map<String, Dependency> dependencyManagement,
                                             MavenProject anchor,
                                             ProjectBuildingRequest request)
            throws IOException, ProjectBuildingException {
        var model = OpenLModelSynthesizer.synthesize(
                s.coordinates, version, s.descriptor, names, dependencyManagement, anchor);
        decorateForModelBuilder(model, openlPluginVersion, findFlattenPlugin(anchor.getModel()));

        var rulesXml = s.folder.resolve(ProjectDescriptor.FILE_NAME);
        var built = projectBuilder.build(new InMemoryModelSource(model, rulesXml), request).getProject();
        // First call setFile to derive basedir = the OpenL folder (rulesXml.getParent()), then write the
        // install pom to target/ and retarget getFile() at it via reflection — see materialiseInstallPom.
        built.setFile(rulesXml.toFile());
        materialiseInstallPom(built, s.folder);
        return built;
    }

    /**
     * Eagerly materialises the pom-less project's install/deploy pom to {@code <folder>/target/openl-pom.xml}
     * and retargets {@code project.getFile()} at it. This way {@code ReactorReader} returns a valid POM the
     * very first time a downstream consumer looks the project up (the workspace reader caches that file for
     * the rest of the session, so a war sibling's {@code mvn verify} resolves the pom-less zip correctly even
     * before {@code install} runs). {@code openl:prepare-pom} runs again at {@code validate} to re-create the
     * file after {@code clean:clean} wipes {@code target/}.
     * <p>
     * The retarget goes through reflection on {@link MavenProject#file} rather than {@link MavenProject#setFile}
     * so that {@code basedir} keeps pointing at the OpenL folder — every OpenL phase that follows still finds
     * {@code rules.xml}, the rules sources, and the build directory in the right place.
     */
    private static void materialiseInstallPom(MavenProject built, Path folder) throws IOException {
        var pomFile = OpenLPackagings.materialiseInstallPom(built.getOriginalModel(), folder.resolve("target"));
        retargetProjectFile(built, pomFile.toFile());
    }

    /**
     * Sets {@link MavenProject#file} via reflection without going through {@link MavenProject#setFile(File)},
     * so the already-derived {@code basedir} (= the OpenL folder) is preserved. {@code setFile} would re-derive
     * {@code basedir} from the new file's parent ({@code target/}), which would break every OpenL phase that
     * reads {@code ${project.basedir}}.
     */
    private static void retargetProjectFile(MavenProject project, File pomFile) {
        try {
            var fileField = MavenProject.class.getDeclaredField("file");
            fileField.setAccessible(true);
            fileField.set(project, pomFile);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to retarget MavenProject.file via reflection", e);
        }
    }

    /**
     * Adds a transient {@code <build><plugin openl-maven-plugin extensions=true></build>} stub to
     * the synthesised model. ModelBuilder loads the plugin's extension descriptor during the
     * in-memory model build, which registers the {@code ArtifactHandler} for {@code packaging=openl}
     * in time for validation. Pinning the version (read from the anchor) avoids Maven resolving the
     * plugin to whatever LATEST is in central. {@code PrepareRepositoryPomMojo} strips the stub
     * before the installed pom is written.
     * <p>
     * When the anchor's lineage declares {@code flatten-maven-plugin}, it is neutralised too: a pom-less
     * project has no on-disk pom for flatten to read ({@code getFile()} points at {@code rules.xml}), and
     * {@code openl:prepare-pom} already emits the flattened install/deploy pom. See {@link #disableFlatten}.
     */
    static void decorateForModelBuilder(Model model, String openlPluginVersion, Plugin inheritedFlatten) {
        var plugin = new Plugin();
        plugin.setGroupId(OpenLPackagings.PLUGIN_GROUP_ID);
        plugin.setArtifactId(OpenLPackagings.PLUGIN_ARTIFACT_ID);
        plugin.setVersion(openlPluginVersion);
        plugin.setExtensions(true);
        var build = new Build();
        build.addPlugin(plugin);
        if (inheritedFlatten != null) {
            disableFlatten(model, build, inheritedFlatten.getVersion());
        }
        model.setBuild(build);
    }

    /**
     * Neutralises an inherited {@code flatten-maven-plugin} on a pom-less project. The skip switch
     * ({@code flatten.skip}) only exists since {@link OpenLPackagings#FLATTEN_MIN_VERSION}, so the effective
     * version is bumped to it (via an explicit {@code <build><plugins>} entry that overrides the inherited
     * version) when older, then the goal is skipped through the {@code flatten.skip} project property — which
     * the mojo honours regardless of how the inherited execution is configured. {@code PrepareRepositoryPomMojo}
     * strips the synthesised {@code <build>} and this property from the installed pom, so neither leaks.
     */
    private static void disableFlatten(Model model, Build build, String inheritedVersion) {
        model.getProperties().setProperty(OpenLPackagings.FLATTEN_SKIP_PROPERTY, "true");
        if (needsVersionBump(inheritedVersion)) {
            var flatten = new Plugin();
            flatten.setGroupId(OpenLPackagings.FLATTEN_GROUP_ID);
            flatten.setArtifactId(OpenLPackagings.FLATTEN_ARTIFACT_ID);
            flatten.setVersion(OpenLPackagings.FLATTEN_MIN_VERSION);
            build.addPlugin(flatten);
        }
    }

    /** True when {@code version} is missing or older than {@link OpenLPackagings#FLATTEN_MIN_VERSION}. */
    static boolean needsVersionBump(String version) {
        if (version == null || version.isBlank()) {
            return true;
        }
        return new ComparableVersion(version)
                .compareTo(new ComparableVersion(OpenLPackagings.FLATTEN_MIN_VERSION)) < 0;
    }

    /**
     * Finds {@code org.codehaus.mojo:flatten-maven-plugin} in a model's {@code <build>} (plugins or
     * pluginManagement), or {@code null}. Run against the anchor's effective model to detect a flatten
     * plugin inherited from its lineage, so it can be neutralised on the pom-less projects beneath it.
     */
    static Plugin findFlattenPlugin(Model model) {
        if (model == null || model.getBuild() == null) {
            return null;
        }
        var match = matchFlattenPlugin(model.getBuild().getPlugins());
        if (match != null) {
            return match;
        }
        var mgmt = model.getBuild().getPluginManagement();
        return mgmt == null ? null : matchFlattenPlugin(mgmt.getPlugins());
    }

    private static Plugin matchFlattenPlugin(List<Plugin> plugins) {
        if (plugins == null) {
            return null;
        }
        for (var p : plugins) {
            if (OpenLPackagings.FLATTEN_GROUP_ID.equals(p.getGroupId())
                    && OpenLPackagings.FLATTEN_ARTIFACT_ID.equals(p.getArtifactId())) {
                return p;
            }
        }
        return null;
    }

    /**
     * Auto-attaches the {@code prepare-bom} goal to the {@code <build><plugins>} openl-maven-plugin
     * execution list (bound to the {@code package} phase). Adds the BOM artefact (classifier
     * {@code deployment-bom}, type {@code pom}) to the anchor's install/deploy without the user
     * having to declare {@code <executions>} explicitly. Idempotent: skips when the execution is
     * already present (e.g. the anchor declared it manually, or this participant fires a second
     * time in the same session).
     */
    private static void bindDeploymentBomExecution(MavenProject anchor) {
        var plugin = findOpenLPluginInBuildPlugins(anchor.getModel());
        if (plugin == null) {
            return;
        }
        for (var existing : plugin.getExecutions()) {
            if (existing.getGoals() != null
                    && existing.getGoals().contains(PrepareDeploymentBomMojo.GOAL_NAME)) {
                return;
            }
        }
        var exec = new PluginExecution();
        exec.setId("default-" + PrepareDeploymentBomMojo.GOAL_NAME);
        exec.setPhase("package");
        exec.addGoal(PrepareDeploymentBomMojo.GOAL_NAME);
        plugin.addExecution(exec);
    }

    /**
     * Resolves the {@code flattenGroupId} flag using the same precedence Maven applies to
     * {@code @Parameter} injection: CLI/system property ({@code -Dopenl.flattenGroupId=...}) first,
     * then the anchor plugin's {@code <configuration><flattenGroupId>...</flattenGroupId>}, then the
     * declared default ({@code false}). Keeps {@link PrepareRepositoryPomMojo} as the single source
     * of truth for both names.
     */
    private static boolean resolveFlattenGroupId(MavenSession session, Plugin anchorPlugin) {
        var fromCli = systemProperty(session, PrepareRepositoryPomMojo.FLATTEN_GROUP_ID_PROPERTY);
        if (fromCli != null) {
            return Boolean.parseBoolean(fromCli.trim());
        }
        return readBooleanConfig(anchorPlugin, PrepareRepositoryPomMojo.FLATTEN_GROUP_ID_PARAM, false);
    }

    private static String systemProperty(MavenSession session, String name) {
        var userProps = session.getUserProperties();
        if (userProps != null) {
            var value = userProps.getProperty(name);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        var sysProps = session.getSystemProperties();
        if (sysProps != null) {
            var value = sysProps.getProperty(name);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    /**
     * Reads a boolean child element from the plugin's {@code <configuration>} block. Returns
     * {@code defaultValue} when the plugin isn't declared, has no configuration, or the named
     * child is absent.
     */
    private static boolean readBooleanConfig(Plugin plugin, String paramName, boolean defaultValue) {
        if (plugin == null || !(plugin.getConfiguration() instanceof Xpp3Dom dom)) {
            return defaultValue;
        }
        var child = dom.getChild(paramName);
        if (child == null || child.getValue() == null || child.getValue().isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(child.getValue().trim());
    }

    /**
     * Indexes the anchor's effective {@code <dependencyManagement>} by {@code groupId:artifactId}
     * so the synthesiser can override the version on each translated dependency. Returns
     * {@code null} when the anchor declares no management — saves an empty-map allocation per
     * synthesised project.
     */
    private static Map<String, Dependency> collectDependencyManagement(Model anchor) {
        DependencyManagement dm = anchor.getDependencyManagement();
        if (dm == null || dm.getDependencies() == null || dm.getDependencies().isEmpty()) {
            return null;
        }
        var map = new HashMap<String, Dependency>(dm.getDependencies().size());
        for (var d : dm.getDependencies()) {
            map.put(d.getGroupId() + ':' + d.getArtifactId(), d);
        }
        return map;
    }

    /**
     * Returns the {@code openl-maven-plugin} {@link Plugin} entry declared by the given model in
     * either {@code <build><plugins>} or {@code <build><pluginManagement><plugins>}, or {@code null}
     * if absent.
     * <p>
     * Use this only to read default configuration values (plugin version, {@code flattenGroupId})
     * that {@code <pluginManagement>} may legitimately supply. To decide whether a project is an
     * anchor or to host the auto-bound {@code prepare-bom} execution, use
     * {@link #findOpenLPluginInBuildPlugins} instead — those concern the plugin that actually
     * executes, which pluginManagement alone never makes happen.
     */
    private static Plugin findOpenLPlugin(Model model) {
        if (model == null || model.getBuild() == null) {
            return null;
        }
        var match = matchOpenLPlugin(model.getBuild().getPlugins());
        if (match != null) {
            return match;
        }
        var mgmt = model.getBuild().getPluginManagement();
        return mgmt == null ? null : matchOpenLPlugin(mgmt.getPlugins());
    }

    /**
     * Returns the {@code openl-maven-plugin} {@link Plugin} entry declared in the model's
     * {@code <build><plugins>}, or {@code null} if absent.
     * <p>
     * Ignores {@code <pluginManagement>}: a plugin managed there is not bound to the build and never
     * executes, so it can neither act as an anchor nor host the auto-bound {@code prepare-bom}
     * execution.
     */
    static Plugin findOpenLPluginInBuildPlugins(Model model) {
        if (model == null || model.getBuild() == null) {
            return null;
        }
        return matchOpenLPlugin(model.getBuild().getPlugins());
    }

    private static Plugin matchOpenLPlugin(List<Plugin> plugins) {
        if (plugins == null) {
            return null;
        }
        for (var p : plugins) {
            if (OpenLPackagings.isOpenLPlugin(p.getGroupId(), p.getArtifactId())) {
                return p;
            }
        }
        return null;
    }

    /**
     * Walks {@code root} and returns folders containing {@code rules.xml} but no {@code pom.xml}.
     * Stops descending into folders hosting a Maven project, hidden directories, or any directory
     * named {@code target}.
     */
    static List<Path> scan(Path root) throws IOException {
        var found = new ArrayList<Path>();
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!dir.equals(root)) {
                    var name = dir.getFileName().toString();
                    if (name.startsWith(".") || TARGET_DIR.equals(name) || Files.isHidden(dir)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                }
                if (Files.isRegularFile(dir.resolve(POM_XML))) {
                    return dir.equals(root) ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
                }
                if (Files.isRegularFile(dir.resolve(ProjectDescriptor.FILE_NAME))) {
                    found.add(dir);
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return found;
    }

    private static String gav(String groupId, String artifactId, String version) {
        return groupId + ':' + artifactId + ':' + version;
    }

    private record Staged(Path folder, OpenLCoordinates coordinates, ProjectDescriptor descriptor) {
    }
}
