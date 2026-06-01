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
import java.util.function.BiPredicate;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.graph.DefaultProjectDependencyGraph;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
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
 * {@code <extensions>true</extensions>}. Each project that directly declares the plugin (and isn't itself
 * an OpenL packaging) acts as an anchor: its basedir is scanned for {@code rules.xml} folders, its groupId
 * is the base for derived coordinates, and its plugin config feeds the {@code flattenGroupId} flag. The
 * scanner never descends past a folder with a {@code pom.xml}, so multiple anchors don't double-scan.
 * <p>
 * Each discovered folder is synthesised into a Maven {@link Model} (see {@link OpenLModelSynthesizer}) with
 * the anchor as {@code <parent>}, so it inherits the anchor's {@code <build>}, {@code <pluginManagement>},
 * {@code <properties>}, {@code <dependencyManagement>}, etc. Anchor build plugins therefore fire on every
 * pom-less project too; mark them {@code <inherited>false</inherited>} to opt out. The model is built via an
 * in-memory {@link InMemoryModelSource} and the projects are added to the reactor in topological order.
 * <b>No pom is ever written to the project root</b> — the install/deploy pom is generated to
 * {@code target/openl-pom.xml} by {@code openl:prepare-pom}.
 * <p>
 * Classic OpenL projects (a real {@code pom.xml}) are untouched.
 *
 * @author Yury Molchan
 */
public class OpenLPomlessParticipant extends AbstractMavenLifecycleParticipant {

    private static final String POM_XML = "pom.xml";
    private static final String TARGET_DIR = "target";

    /**
     * Session-data key marking that pom-less projects were already injected this session. Maven calls
     * {@code afterProjectsRead} once per extension realm, so without this guard the participant would
     * re-run when several reactor poms declare the plugin — repeating the scan and tripping
     * {@code DuplicateProjectException} on the second add.
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

        // Auto-bind the deployment-BOM execution on every anchor; the mojo skips when it has no OpenL members.
        for (var anchor : anchors) {
            bindDeploymentBomExecution(anchor);
        }

        var request = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
        // Sibling artefacts aren't in the local repo yet; a failed lookup here would poison Aether's
        // negative-resolution cache for the real lookup during compile.
        request.setResolveDependencies(false);

        // Phase 1 — stage every anchor and build the session-wide reactor index (groupId:artifactId →
        // version) so the synthesiser can force a reactor sibling's version onto a <mavenArtifact> pointing
        // at it (see OpenLModelSynthesizer).
        var reactorVersions = OpenLPackagings.reactorOpenLVersions(session.getAllProjects());
        var stagings = new ArrayList<AnchorStaging>();
        for (var anchor : anchors) {
            var staging = stageAnchor(anchor, session);
            if (staging == null) {
                continue;
            }
            stagings.add(staging);
            for (var s : staging.staged()) {
                reactorVersions.putIfAbsent(ga(s.coordinates().groupId(), s.coordinates().artifactId()),
                        staging.version());
            }
        }
        if (stagings.isEmpty()) {
            return;
        }

        // Phase 2 — synthesise every staged project against the reactor index, skipping any whose GAV is
        // already in the reactor (a parallel anchor with the same coordinates, or a re-fired lifecycle).
        var existing = new HashSet<String>();
        for (var p : session.getAllProjects()) {
            existing.add(gav(p.getGroupId(), p.getArtifactId(), p.getVersion()));
        }
        var added = new ArrayList<MavenProject>();
        for (var staging : stagings) {
            for (var s : staging.staged()) {
                MavenProject built;
                try {
                    built = buildPomlessProject(s, staging, reactorVersions, request);
                } catch (IOException | ProjectBuildingException e) {
                    throw new MavenExecutionException(
                            "Failed to build pom-less OpenL project at '" + s.folder() + "'.", e);
                }
                LOG.info("Discovered pom-less OpenL project '{}:{}' at '{}'.",
                        s.coordinates().groupId(), s.coordinates().artifactId(), s.folder());
                if (existing.add(gav(built.getGroupId(), built.getArtifactId(), built.getVersion()))) {
                    added.add(built);
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
        // Maven 3.9.x ReactorReader snapshots session.getProjects() once and never refreshes — without this
        // poke a sibling can't resolve the pom-less zip from the reactor. See ReactorReaderInjector.
        ReactorReaderInjector.inject(session, added);
    }

    /**
     * Returns every reactor project that declares {@code openl-maven-plugin} in its own raw
     * {@code <build><plugins>} and isn't an OpenL packaging. Inherited and {@code <pluginManagement>}-only
     * entries don't count — they never bind the plugin to the build. Each is an independent anchor.
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
     * Scans an anchor's basedir and stages each discovered {@code rules.xml} folder: reads its descriptor,
     * computes its coordinates, and builds the name → coordinates index used to resolve {@code <name>}
     * dependencies. Returns {@code null} when the anchor hosts no readable pom-less project.
     * <p>
     * Synthesis is deferred to {@link #buildPomlessProject} so it runs against the session-wide reactor
     * index, letting a {@code <mavenArtifact>} pick up a sibling's reactor version across anchors.
     */
    private AnchorStaging stageAnchor(MavenProject anchor, MavenSession session)
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
            return null;
        }

        var anchorPlugin = findOpenLPlugin(anchor.getModel());
        var flattenGroupId = resolveFlattenGroupId(session, anchorPlugin);

        // Read descriptors, compute coordinates, build the name → coordinates index.
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
            return null;
        }

        var openlPluginVersion = anchorPlugin == null ? null : anchorPlugin.getVersion();
        var dependencyManagement = collectDependencyManagement(anchor.getModel());
        return new AnchorStaging(anchor, anchor.getVersion(), openlPluginVersion, names,
                dependencyManagement, staged);
    }

    /**
     * Synthesises a single pom-less {@link MavenProject} against the session-wide reactor index. Dependency
     * resolution is left to the per-project lifecycle (siblings aren't installed yet).
     */
    private MavenProject buildPomlessProject(Staged s, AnchorStaging staging,
                                             Map<String, String> reactorVersions,
                                             ProjectBuildingRequest request)
            throws IOException, ProjectBuildingException {
        var anchor = staging.anchor();
        var model = OpenLModelSynthesizer.synthesize(s.coordinates(), staging.version(), s.descriptor(),
                staging.names(), staging.dependencyManagement(), anchor, reactorVersions);
        decorateForModelBuilder(model, staging.openlPluginVersion(), findFlattenPlugin(anchor.getModel()));

        var rulesXml = s.folder().resolve(ProjectDescriptor.FILE_NAME);
        var built = projectBuilder.build(new InMemoryModelSource(model, rulesXml), request).getProject();
        // setFile derives basedir from rules.xml's folder; materialiseInstallPom then retargets getFile()
        // at target/openl-pom.xml via reflection, keeping basedir intact.
        built.setFile(rulesXml.toFile());
        materialiseInstallPom(built, s.folder());
        return built;
    }

    /**
     * Eagerly materialises the install/deploy pom to {@code <folder>/target/openl-pom.xml} and retargets
     * {@code project.getFile()} at it, so {@code ReactorReader} returns a valid POM the first time a consumer
     * looks the project up (before {@code install} runs). {@code openl:prepare-pom} re-creates it at
     * {@code validate} after {@code clean} wipes {@code target/}.
     * <p>
     * The retarget uses reflection rather than {@link MavenProject#setFile} so {@code basedir} keeps pointing
     * at the OpenL folder — see {@link #retargetProjectFile}.
     */
    private static void materialiseInstallPom(MavenProject built, Path folder) throws IOException {
        var pomFile = OpenLPackagings.materialiseInstallPom(built.getOriginalModel(), folder.resolve("target"));
        retargetProjectFile(built, pomFile.toFile());
    }

    /**
     * Sets {@link MavenProject#file} via reflection, bypassing {@link MavenProject#setFile(File)} so the
     * derived {@code basedir} (the OpenL folder) is preserved. {@code setFile} would re-derive it from the new
     * file's parent ({@code target/}) and break every OpenL phase that reads {@code ${project.basedir}}.
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
     * Adds a transient {@code openl-maven-plugin} {@code <build>} stub to the synthesised model so ModelBuilder
     * loads its extension descriptor and registers the {@code openl} {@code ArtifactHandler} in time for
     * validation. The version is pinned from the anchor to avoid resolving LATEST.
     * {@code PrepareRepositoryPomMojo} strips the stub before the installed pom is written.
     * <p>
     * An inherited {@code flatten-maven-plugin} is neutralised too — see {@link #disableFlatten}.
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
     * Neutralises an inherited {@code flatten-maven-plugin} on a pom-less project (it has no on-disk pom to
     * flatten; {@code openl:prepare-pom} emits the install pom). Sets the {@code flatten.skip} property, and
     * — since that switch only exists since {@link OpenLPackagings#FLATTEN_MIN_VERSION} — bumps an older
     * inherited version via an explicit {@code <build>} entry. {@code PrepareRepositoryPomMojo} strips both
     * from the installed pom.
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
     * Finds {@code flatten-maven-plugin} in a model's {@code <build>} (plugins or pluginManagement), or
     * {@code null}. Run against the anchor's effective model to detect an inherited flatten plugin.
     */
    static Plugin findFlattenPlugin(Model model) {
        return findPlugin(model, OpenLPackagings::isFlattenPlugin);
    }

    /**
     * Auto-attaches the {@code prepare-bom} goal (bound to {@code package}) to the anchor's
     * {@code openl-maven-plugin} so the deployment BOM is installed without an explicit {@code <executions>}
     * block. Idempotent: skips when the execution is already present.
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
     * Resolves the {@code flattenGroupId} flag with Maven's {@code @Parameter} precedence: the
     * {@code -Dopenl.flattenGroupId} system property first, then the anchor plugin's
     * {@code <configuration><flattenGroupId>}, then the default {@code false}.
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
     * Indexes the anchor's effective {@code <dependencyManagement>} by {@code groupId:artifactId} so the
     * synthesiser can override translated dependency versions. Returns {@code null} when none is declared.
     */
    private static Map<String, Dependency> collectDependencyManagement(Model anchor) {
        var dm = anchor.getDependencyManagement();
        if (dm == null || dm.getDependencies() == null || dm.getDependencies().isEmpty()) {
            return null;
        }
        var map = new HashMap<String, Dependency>(dm.getDependencies().size());
        for (var d : dm.getDependencies()) {
            map.put(ga(d.getGroupId(), d.getArtifactId()), d);
        }
        return map;
    }

    /**
     * Returns the {@code openl-maven-plugin} entry from the model's {@code <build><plugins>} or
     * {@code <pluginManagement>}, or {@code null}.
     * <p>
     * Use only to read default config values (plugin version, {@code flattenGroupId}) that
     * {@code <pluginManagement>} may supply. To decide anchor status or host the {@code prepare-bom}
     * execution, use {@link #findOpenLPluginInBuildPlugins} — those need the plugin that actually executes.
     */
    private static Plugin findOpenLPlugin(Model model) {
        return findPlugin(model, OpenLPackagings::isOpenLPlugin);
    }

    /**
     * Returns the {@code openl-maven-plugin} entry from the model's {@code <build><plugins>}, or {@code null}.
     * Ignores {@code <pluginManagement>} — a managed-only plugin never binds to the build, so it can't be an
     * anchor or host the {@code prepare-bom} execution.
     */
    static Plugin findOpenLPluginInBuildPlugins(Model model) {
        if (model == null || model.getBuild() == null) {
            return null;
        }
        return matchPlugin(model.getBuild().getPlugins(), OpenLPackagings::isOpenLPlugin);
    }

    /**
     * Finds the first plugin in the model's {@code <build>} — {@code <plugins>} first, then
     * {@code <pluginManagement>} — whose {@code groupId}/{@code artifactId} satisfies {@code matches},
     * or {@code null} when the model declares no {@code <build>} or nothing matches.
     */
    private static Plugin findPlugin(Model model, BiPredicate<String, String> matches) {
        if (model == null || model.getBuild() == null) {
            return null;
        }
        var match = matchPlugin(model.getBuild().getPlugins(), matches);
        if (match != null) {
            return match;
        }
        var mgmt = model.getBuild().getPluginManagement();
        return mgmt == null ? null : matchPlugin(mgmt.getPlugins(), matches);
    }

    private static Plugin matchPlugin(List<Plugin> plugins, BiPredicate<String, String> matches) {
        if (plugins == null) {
            return null;
        }
        for (var p : plugins) {
            if (matches.test(p.getGroupId(), p.getArtifactId())) {
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

    private static String ga(String groupId, String artifactId) {
        return groupId + ':' + artifactId;
    }

    private record Staged(Path folder, OpenLCoordinates coordinates, ProjectDescriptor descriptor) {
    }

    /**
     * One anchor's staged state, captured before synthesis so the reactor index can be assembled across all
     * anchors first: the anchor, the version applied to its pom-less projects, the pinned
     * {@code openl-maven-plugin} version, the name → coordinates index, the effective
     * {@code <dependencyManagement>}, and the staged projects awaiting synthesis.
     */
    private record AnchorStaging(MavenProject anchor, String version, String openlPluginVersion,
                                 Map<String, OpenLCoordinates> names,
                                 Map<String, Dependency> dependencyManagement, List<Staged> staged) {
    }
}
