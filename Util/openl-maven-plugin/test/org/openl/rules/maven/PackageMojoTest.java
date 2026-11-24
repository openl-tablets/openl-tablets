package org.openl.rules.maven;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;

/**
 * Covers the packaging of the project's compiled classes by {@link PackageMojo}. The jar is built from the
 * classes folder as a deterministic {@code <finalName>-classes.jar} file, attached to the project with the
 * {@code classes} classifier, and embedded into the produced archive under the classpath folder.
 *
 * <p>The jar is not built when the classes folder is empty, when a main jar artifact already exists, or for
 * the {@code openl-jar} packaging where the classes are packed into the root of the main artifact.
 *
 * <p>The {@code includeGeneratedClasspathJar} parameter and its automatic decision control only the copy
 * embedded into the archive; the {@code classes} artifact is attached regardless, so Java consumers can
 * always depend on it.
 *
 * <p>The deployment archive of a project that depends on other OpenL projects is named after the {@code finalName}
 * of the build; the deprecated {@code deploymentName} parameter is ignored with a warning.
 *
 * <p>The main archive holds no file of the {@code tests} folder, so its {@code rules.xml} names no module of the
 * folder: a workbook of any format or a folder of Markdown files, in the sources and in their Markdown copy. A path
 * pattern of the folder stays, and a project of test modules only keeps the pattern of every file of the folder, not
 * the default modules. A descriptor without such modules is packaged as it is.
 *
 * @author Yury Molchan
 */
class PackageMojoTest {

    private static final String FINAL_NAME = "demo-1.0";

    @Test
    void attachesClassesJarBuiltFromClassesFolder(@TempDir Path tmp) throws Exception {
        var mojo = newMojo(tmp, "zip");
        touch(mojo.classesDirectory.toPath().resolve("com/example/Service.class"));

        mojo.execute(openlSources(tmp), false);

        var classesJar = new File(mojo.outputDirectory, FINAL_NAME + "-classes.jar");
        assertTrue(classesJar.isFile());
        assertTrue(zipEntries(classesJar).contains("com/example/Service.class"));

        var helper = (RecordingProjectHelper) mojo.projectHelper;
        assertEquals(List.of(new Attached("jar", "classes", classesJar)), helper.attached);

        // The main artifact is the ZIP archive; the classes jar is embedded into it as a classpath entry.
        var mainArtifact = mojo.project.getArtifact().getFile();
        assertEquals(new File(mojo.outputDirectory, FINAL_NAME + ".zip"), mainArtifact);
        assertEquals(Set.of("rules/Main.xlsx", "lib/" + FINAL_NAME + ".jar"), zipEntries(mainArtifact));
    }

    @Test
    void noClassesArtifactWhenClassesFolderIsEmpty(@TempDir Path tmp) throws Exception {
        var mojo = newMojo(tmp, "zip");

        mojo.execute(openlSources(tmp), false);

        assertFalse(new File(mojo.outputDirectory, FINAL_NAME + "-classes.jar").exists());
        assertTrue(((RecordingProjectHelper) mojo.projectHelper).attached.isEmpty());
        assertEquals(Set.of("rules/Main.xlsx"), zipEntries(mojo.project.getArtifact().getFile()));
    }

    @Test
    void noClassesArtifactForOpenLJarPackaging(@TempDir Path tmp) throws Exception {
        var mojo = newMojo(tmp, OpenLPackagings.OPENL_JAR_PACKAGING);
        touch(mojo.classesDirectory.toPath().resolve("com/example/Service.class"));

        mojo.execute(openlSources(tmp), false);

        // The compiled classes are packed into the root of the main jar instead of a supplementary artifact.
        assertFalse(new File(mojo.outputDirectory, FINAL_NAME + "-classes.jar").exists());
        assertTrue(((RecordingProjectHelper) mojo.projectHelper).attached.isEmpty());
        var mainArtifact = mojo.project.getArtifact().getFile();
        assertEquals(new File(mojo.outputDirectory, FINAL_NAME + ".jar"), mainArtifact);
        assertEquals(Set.of("rules/Main.xlsx", "com/example/Service.class"), zipEntries(mainArtifact));
    }

    @Test
    void noClassesArtifactWhenMainArtifactAlreadyExists(@TempDir Path tmp) throws Exception {
        var mojo = newMojo(tmp, "jar");
        touch(mojo.classesDirectory.toPath().resolve("com/example/Service.class"));
        // A jar built by another plugin (e.g. maven-jar-plugin) is already registered as the main artifact.
        var prebuiltJar = mojo.outputDirectory.toPath().resolve(FINAL_NAME + ".jar");
        touch(prebuiltJar);
        mojo.project.getArtifact().setFile(prebuiltJar.toFile());

        mojo.execute(openlSources(tmp), false);

        // The existing jar is embedded as the classpath entry; no classes jar is built or attached.
        assertFalse(new File(mojo.outputDirectory, FINAL_NAME + "-classes.jar").exists());
        var outputZip = new File(mojo.outputDirectory, FINAL_NAME + ".zip");
        assertEquals(Set.of("rules/Main.xlsx", "lib/" + FINAL_NAME + ".jar"), zipEntries(outputZip));
        assertEquals(List.of(new Attached("zip", null, outputZip)),
                ((RecordingProjectHelper) mojo.projectHelper).attached);
    }

    @Test
    void autoDecisionAttachesClassesArtifactButKeepsItOffTheArchive(@TempDir Path tmp) throws Exception {
        var mojo = newMojo(tmp, "zip");
        configureGenerateGoal(mojo);
        touch(mojo.classesDirectory.toPath().resolve("com/example/Service.class"));

        // The project depends on other OpenL projects and pre-generates classes: the automatic decision
        // keeps the jar off the deployed runtime classpath, but Java consumers still get the artifact.
        mojo.execute(openlSources(tmp), true);

        var classesJar = new File(mojo.outputDirectory, FINAL_NAME + "-classes.jar");
        assertTrue(classesJar.isFile());
        assertEquals(List.of(new Attached("jar", "classes", classesJar)),
                ((RecordingProjectHelper) mojo.projectHelper).attached);
        assertEquals(Set.of("rules/Main.xlsx"), zipEntries(mojo.project.getArtifact().getFile()));
    }

    @Test
    void autoDecisionKeepsTheJarInTheArchiveWithoutGenerateGoal(@TempDir Path tmp) throws Exception {
        var mojo = newMojo(tmp, "zip");
        touch(mojo.classesDirectory.toPath().resolve("com/example/Handwritten.class"));

        // OpenL dependencies alone do not trigger the exclusion: without the generate goal the classes
        // folder holds only hand-written classes or resources which exist nowhere else.
        mojo.execute(openlSources(tmp), true);

        assertEquals(Set.of("rules/Main.xlsx", "lib/" + FINAL_NAME + ".jar"),
                zipEntries(mojo.project.getArtifact().getFile()));
    }

    @Test
    void explicitFalseKeepsTheJarOffTheArchiveButAttachesClassesArtifact(@TempDir Path tmp) throws Exception {
        var mojo = newMojo(tmp, "zip");
        mojo.includeGeneratedClasspathJar = Boolean.FALSE;
        touch(mojo.classesDirectory.toPath().resolve("com/example/Service.class"));

        mojo.execute(openlSources(tmp), false);

        var classesJar = new File(mojo.outputDirectory, FINAL_NAME + "-classes.jar");
        assertEquals(List.of(new Attached("jar", "classes", classesJar)),
                ((RecordingProjectHelper) mojo.projectHelper).attached);
        assertEquals(Set.of("rules/Main.xlsx"), zipEntries(mojo.project.getArtifact().getFile()));
    }

    @Test
    void explicitTrueOverridesTheAutomaticExclusion(@TempDir Path tmp) throws Exception {
        var mojo = newMojo(tmp, "zip");
        configureGenerateGoal(mojo);
        mojo.includeGeneratedClasspathJar = Boolean.TRUE;
        touch(mojo.classesDirectory.toPath().resolve("com/example/Service.class"));

        mojo.execute(openlSources(tmp), true);

        assertEquals(Set.of("rules/Main.xlsx", "lib/" + FINAL_NAME + ".jar"),
                zipEntries(mojo.project.getArtifact().getFile()));
    }

    @Test
    void repackagingOverwritesTheClassesJarInsteadOfAccumulatingCopies(@TempDir Path tmp) throws Exception {
        var mojo = newMojo(tmp, OpenLPackagings.OPENL_PACKAGING);
        touch(mojo.classesDirectory.toPath().resolve("com/example/Service.class"));
        var sourcePath = openlSources(tmp);

        mojo.execute(sourcePath, false);
        // 'mvn package verify' runs the package phase twice on a fresh project state without cleaning target.
        mojo.project.getArtifact().setFile(null);
        mojo.execute(sourcePath, false);

        var classesJars = mojo.outputDirectory.list((dir, name) -> name.endsWith("-classes.jar"));
        assertEquals(1, classesJars.length);
        assertEquals(FINAL_NAME + "-classes.jar", classesJars[0]);
        assertEquals(2, ((RecordingProjectHelper) mojo.projectHelper).attached.size());
    }

    @Test
    void packagesTheMarkdownCopyOfTheProjectInsteadOfTheSources(@TempDir Path tmp) throws Exception {
        var mojo = newMojo(tmp, "zip");
        mojo.markdown = true;
        mojo.markdownDirectory = tmp.resolve("target/openl-markdown").toFile();
        touch(mojo.markdownDirectory.toPath().resolve("rules/Main/main.md"));

        mojo.execute(openlSources(tmp), false);

        assertEquals(Set.of("rules/Main/main.md"), zipEntries(mojo.project.getArtifact().getFile()));
    }

    @Test
    void markdownPackagingWithoutTheCopyFails(@TempDir Path tmp) throws Exception {
        var mojo = newMojo(tmp, "zip");
        mojo.markdown = true;
        mojo.markdownDirectory = tmp.resolve("target/openl-markdown").toFile();

        var failure = assertThrows(MojoFailureException.class, () -> mojo.execute(openlSources(tmp), false));

        assertTrue(failure.getMessage().contains("The 'compile' goal writes it"), failure::getMessage);
    }

    // The deprecated parameter is covered until it is removed.
    @SuppressWarnings({"java:S5738", "removal"})
    @Test
    void deploymentArchiveIsNamedAfterFinalNameIgnoringDeploymentName(@TempDir Path tmp) throws Exception {
        var mojo = newMojo(tmp, OpenLPackagings.OPENL_PACKAGING);
        mojo.deploymentName = "Custom name";
        var warnings = new ArrayList<String>();
        mojo.setLog(new SystemStreamLog() {
            @Override
            public void warn(CharSequence content) {
                warnings.add(content.toString());
            }
        });
        var dependency = new DefaultArtifact("com.example", "dep", "1.0", "compile", "zip", null,
                new DefaultArtifactHandler("zip"));
        dependency.setFile(openlZip(tmp.resolve("dep-1.0.zip")));
        mojo.project.setArtifacts(Set.of(dependency));

        mojo.execute(openlSources(tmp), true);

        var deploymentZip = new File(mojo.outputDirectory, FINAL_NAME + "-deployment.zip");
        var helper = (RecordingProjectHelper) mojo.projectHelper;
        assertEquals(List.of(new Attached("zip", "deployment", deploymentZip)), helper.attached);
        var yaml = new String(entryBytes(deploymentZip, "deployment.yaml"), UTF_8);
        assertEquals("name: " + FINAL_NAME, yaml.strip());
        assertEquals(List.of("Parameter 'deploymentName' is deprecated and has no effect. "
                + "The deployment archive is always named after 'finalName' of the build."), warnings);
    }

    @Test
    void removesTheDeploymentArchiveOfAnEarlierBuild(@TempDir Path tmp) throws Exception {
        var mojo = newMojo(tmp, OpenLPackagings.OPENL_PACKAGING);
        var stale = Files.writeString(mojo.outputDirectory.toPath().resolve(FINAL_NAME + "-deployment.zip"), "stub");

        mojo.execute(openlSources(tmp), false);

        assertFalse(Files.exists(stale));
        assertTrue(((RecordingProjectHelper) mojo.projectHelper).attached.isEmpty());
    }

    @Test
    void keepsTheArchiveOfThisBuildWithTheDeploymentClassifier(@TempDir Path tmp) throws Exception {
        var mojo = newMojo(tmp, "jar");
        mojo.classifier = "deployment";

        mojo.execute(openlSources(tmp), false);

        var archive = new File(mojo.outputDirectory, FINAL_NAME + "-deployment.zip");
        assertTrue(archive.isFile());
        assertEquals(List.of(new Attached("zip", "deployment", archive)),
                ((RecordingProjectHelper) mojo.projectHelper).attached);
    }

    @Test
    void leavesTheTestsModulesOutOfTheDescriptorOfTheArchive(@TempDir Path tmp) throws Exception {
        var mojo = newMojo(tmp, OpenLPackagings.OPENL_PACKAGING);
        var sources = openlSources(tmp, """
                <project>
                    <name>Explicit</name>
                    <modules>
                        <module><rules-root path="rules/Main.xlsx"/></module>
                        <module><rules-root path="rules/Rates"/></module>
                        <module><name>MyTests</name><rules-root path="tests/MyTests.xlsx"/></module>
                        <module><rules-root path="tests/Legacy.xls"/></module>
                        <module><rules-root path="tests/Cases"/></module>
                        <module><rules-root path="./tests/../tests/Dotted.xlsm"/></module>
                        <module><rules-root path="tests/*.xlsx"/></module>
                        <module><rules-root path="testsuite/Suite.xlsx"/></module>
                    </modules>
                </project>
                """);
        touch(sources.resolve("rules/Rates/rate.md"));
        touch(sources.resolve("tests/MyTests.xlsx"));
        touch(sources.resolve("tests/Cases/case.md"));

        mojo.execute(sources.toString(), false);

        var archive = mojo.project.getArtifact().getFile();
        assertEquals(Set.of("rules.xml", "rules/Main.xlsx", "rules/Rates/rate.md"), zipEntries(archive));
        var descriptor = readDescriptor(archive);
        assertEquals("Explicit", descriptor.getName());
        assertEquals(List.of("rules/Main.xlsx", "rules/Rates", "tests/*.xlsx", "testsuite/Suite.xlsx"),
                modulePaths(descriptor));
        var testsArchive = new File(mojo.outputDirectory, FINAL_NAME + "-tests.zip");
        assertEquals(Set.of("tests/MyTests.xlsx", "tests/Cases/case.md"), zipEntries(testsArchive));
    }

    @Test
    void keepsThePatternOfTheTestsFolderWhenOnlyTestsModulesAreDeclared(@TempDir Path tmp) throws IOException {
        var mojo = newMojo(tmp, OpenLPackagings.OPENL_PACKAGING);
        var sources = openlSources(tmp, """
                <project>
                    <modules>
                        <module><name>Without path</name></module>
                        <module><rules-root path="tests/MyTests.xlsx"/></module>
                        <module><rules-root path="tests/Cases"/></module>
                    </modules>
                </project>
                """);

        var descriptor = mojo.descriptorWithoutTests(sources);

        assertNotNull(descriptor);
        var packaged = ProjectDescriptor.read(new ByteArrayInputStream(descriptor.toBytes()));
        assertNotNull(packaged);
        assertEquals(List.of("tests/**"), modulePaths(packaged));
    }

    @Test
    void leavesTheTestsModulesOutOfTheDescriptorOfTheMarkdownCopy(@TempDir Path tmp) throws Exception {
        var mojo = newMojo(tmp, OpenLPackagings.OPENL_PACKAGING);
        mojo.markdown = true;
        mojo.markdownDirectory = tmp.resolve("target/openl-markdown").toFile();
        var copy = mojo.markdownDirectory.toPath();
        touch(copy.resolve("rules/Main/main.md"));
        touch(copy.resolve("tests/MyTests/test.md"));
        Files.writeString(copy.resolve(ProjectDescriptor.FILE_NAME), """
                <project>
                    <modules>
                        <module><rules-root path="rules/Main"/></module>
                        <module><rules-root path="tests/MyTests"/></module>
                    </modules>
                </project>
                """);

        mojo.execute(openlSources(tmp), false);

        var archive = mojo.project.getArtifact().getFile();
        assertEquals(Set.of("rules.xml", "rules/Main/main.md"), zipEntries(archive));
        assertEquals(List.of("rules/Main"), modulePaths(readDescriptor(archive)));
    }

    @Test
    void packagesTheDescriptorWithoutTestsModulesAsItIs(@TempDir Path tmp) throws Exception {
        var mojo = newMojo(tmp, OpenLPackagings.OPENL_PACKAGING);
        var xml = """
                <project>
                    <!-- kept as written -->
                    <modules>
                        <module><rules-root path="rules/Main.xlsx"/></module>
                        <module><rules-root path="tests/**/*.xlsx"/></module>
                    </modules>
                </project>
                """;
        var sources = openlSources(tmp, xml);

        mojo.execute(sources.toString(), false);

        assertArrayEquals(xml.getBytes(UTF_8),
                entryBytes(mojo.project.getArtifact().getFile(), ProjectDescriptor.FILE_NAME));
    }

    @Test
    void packagesNoDescriptorOfAProjectWithoutIt(@TempDir Path tmp) {
        var mojo = new PackageMojo();

        assertNull(mojo.descriptorWithoutTests(tmp));
    }

    /** Creates the OpenL source folder with a rules file and the descriptor, and returns its path. */
    private static Path openlSources(Path tmp, String rulesXml) throws IOException {
        var sources = Path.of(openlSources(tmp));
        Files.writeString(sources.resolve(ProjectDescriptor.FILE_NAME), rulesXml);
        return sources;
    }

    private static ProjectDescriptor readDescriptor(File archive) throws IOException {
        var bytes = entryBytes(archive, ProjectDescriptor.FILE_NAME);
        var descriptor = ProjectDescriptor.read(new ByteArrayInputStream(bytes));
        assertNotNull(descriptor);
        return descriptor;
    }

    static List<String> modulePaths(ProjectDescriptor descriptor) {
        return descriptor.getModules().stream().map(Module::getRulesRootPath).toList();
    }

    // The deprecated parameter is covered until it is removed.
    @SuppressWarnings("java:S5738")
    private static PackageMojo newMojo(Path tmp, String packaging) throws IOException {
        var model = new Model();
        model.setGroupId("com.example");
        model.setArtifactId("demo");
        model.setVersion("1.0");
        var project = new MavenProject(model);
        project.setArtifact(new DefaultArtifact("com.example", "demo", "1.0", null, packaging, null,
                new DefaultArtifactHandler(packaging)));

        var mojo = new PackageMojo();
        mojo.project = project;
        mojo.projectHelper = new RecordingProjectHelper();
        mojo.packaging = packaging;
        mojo.format = "zip";
        mojo.finalName = FINAL_NAME;
        mojo.outputDirectory = Files.createDirectories(tmp.resolve("target")).toFile();
        mojo.classesDirectory = Files.createDirectories(tmp.resolve("target/classes")).toFile();
        mojo.classpathFolder = "lib/";
        mojo.projectBaseDir = tmp.toString();
        return mojo;
    }

    /** Declares this plugin's {@code generate} goal in the test project's build, the way a real POM does. */
    private static void configureGenerateGoal(PackageMojo mojo) {
        var descriptor = new PluginDescriptor();
        descriptor.setGroupId("org.openl.rules");
        descriptor.setArtifactId("openl-maven-plugin");
        mojo.plugin = descriptor;

        var execution = new PluginExecution();
        execution.addGoal("generate");
        var plugin = new Plugin();
        plugin.setGroupId("org.openl.rules");
        plugin.setArtifactId("openl-maven-plugin");
        plugin.addExecution(execution);

        var build = new Build();
        build.addPlugin(plugin);
        mojo.project.getModel().setBuild(build);
    }

    /** Creates the OpenL source folder with a single rules file and returns its path. */
    private static String openlSources(Path tmp) {
        var openl = tmp.resolve("openl");
        touch(openl.resolve("rules/Main.xlsx"));
        return openl.toString();
    }

    /** Writes an OpenL project archive, the way a dependent OpenL project is resolved by Maven. */
    private static File openlZip(Path zipPath) throws IOException {
        try (var out = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            out.putNextEntry(new ZipEntry("rules.xml"));
            out.write("<project><name>dep</name></project>".getBytes(UTF_8));
            out.closeEntry();
        }
        return zipPath.toFile();
    }

    private static void touch(Path path) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, "stub");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] entryBytes(File file, String name) throws IOException {
        try (var zip = new ZipFile(file)) {
            return zip.getInputStream(zip.getEntry(name)).readAllBytes();
        }
    }

    private static Set<String> zipEntries(File file) throws IOException {
        try (var zip = new ZipFile(file)) {
            return zip.stream().map(ZipEntry::getName).collect(Collectors.toSet());
        }
    }

    private record Attached(String type, String classifier, File file) {
    }

    /** Records every attached artifact instead of wiring the Maven internals. */
    private static final class RecordingProjectHelper implements MavenProjectHelper {
        private final List<Attached> attached = new ArrayList<>();

        @Override
        public void attachArtifact(MavenProject project, File artifactFile, String artifactClassifier) {
            attached.add(new Attached(null, artifactClassifier, artifactFile));
        }

        @Override
        public void attachArtifact(MavenProject project, String artifactType, File artifactFile) {
            attached.add(new Attached(artifactType, null, artifactFile));
        }

        @Override
        public void attachArtifact(MavenProject project, String artifactType, String artifactClassifier,
                                   File artifactFile) {
            attached.add(new Attached(artifactType, artifactClassifier, artifactFile));
        }

        @Override
        public void addResource(MavenProject project, String resourceDirectory, List<String> includes,
                                List<String> excludes) {
                                    // The test does not need the resources to be registered.
        }

        @Override
        public void addTestResource(MavenProject project, String resourceDirectory, List<String> includes,
                                    List<String> excludes) {
                                        // The test does not need the resources to be registered.
        }
    }
}
