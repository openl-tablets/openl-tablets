package org.openl.rules.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

    private static void touch(Path path) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, "stub");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
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
        }

        @Override
        public void addTestResource(MavenProject project, String resourceDirectory, List<String> includes,
                                    List<String> excludes) {
        }
    }
}
