package org.openl.rules.webstudio.dependencies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.repository.RepositoryInstatiator;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.dtr.FolderMapper;

class ProjectPomsTest {

    @TempDir
    private Path root;
    private Repository design;

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(root.resolve("rules/Rating"));
        Files.writeString(root.resolve("rules/Rating/rules.xml"), "<project/>");
        Files.writeString(root.resolve("rules/Rating/pom.xml"), "<project>pom</project>");
        design = RepositoryInstatiator.newRepository("repository.design",
                Map.of("repository.design.factory", "repo-file",
                        "repository.design.uri", root.toString())::get);
    }

    @Test
    void readsThePomOfAFolderOnDisk() throws IOException {
        var folder = root.resolve("rules/Rating");
        assertEquals("<project>pom</project>", ProjectPoms.inFolder(folder, folder).text());
        assertNull(ProjectPoms.inFolder(root.resolve("rules"), root));
    }

    @Test
    void readsThePomOfTheRepository() throws IOException {
        assertEquals("<project>pom</project>", ProjectPoms.inRepository(project(design), null, null).text());
    }

    /** The copy of an opened project holds the changes not saved yet, but not the parent beside the project. */
    @Test
    void readsTheTextOfTheCopyAndTheParentOfTheRepository(@TempDir Path copy) throws IOException {
        Files.writeString(root.resolve("rules/pom.xml"), "<project>parent</project>");
        Files.writeString(copy.resolve("pom.xml"), "<project>edited</project>");

        var pom = ProjectPoms.inRepository(project(design), copy, null);

        assertEquals("<project>edited</project>", pom.text());
        assertEquals("<project>parent</project>", new String(pom.modelSource()
                .getRelatedSource("..")
                .getInputStream()
                .readAllBytes(), StandardCharsets.UTF_8));
        assertEquals("<project>pom</project>",
                ProjectPoms.inRepository(project(design), copy.resolve("none"), null).text(),
                "a copy holding none reads the one of the repository");
    }

    @Test
    void readsTheParentOfABuildOfSeveralModulesFromTheRepositoryToo() throws IOException {
        Files.writeString(root.resolve("rules/pom.xml"), "<project>parent</project>");

        var parent = ProjectPoms.inRepository(project(design), null, null).modelSource().getRelatedSource("../pom.xml");

        assertEquals("<project>parent</project>", new String(parent.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8));
        assertEquals("<project>parent</project>", new String(ProjectPoms.inFolder(root.resolve("rules/Rating"), root)
                .modelSource()
                .getRelatedSource("..")
                .getInputStream()
                .readAllBytes(), StandardCharsets.UTF_8));
    }

    /** A POM on disk names no file outside the root it is read in, such as a project of another user. */
    @Test
    void readsNoPomOnDiskOutsideTheRoot() throws IOException {
        Files.writeString(root.resolve("rules/pom.xml"), "<project>parent</project>");
        var folder = root.resolve("rules/Rating");

        var source = ProjectPoms.inFolder(folder, folder).modelSource();

        assertNull(source.getRelatedSource(".."));
        assertNull(source.getRelatedSource("../../rules/pom.xml"));
    }

    @Test
    void readsThePomByItsRealPathPastAFolderMapper() throws IOException {
        var mapped = mock(Repository.class, withSettings().extraInterfaces(FolderMapper.class));
        when(mapped.supports()).thenReturn(new FeaturesBuilder(mapped).setFolders(true).build());
        when(((FolderMapper) mapped).getDelegate()).thenReturn(design);

        assertEquals("<project>pom</project>", ProjectPoms.inRepository(project(mapped), null, null).text());
    }

    @Test
    void readsThePomOfAnOlderRevisionOpenedAtThatRevision() throws IOException {
        var plain = mock(Repository.class);
        when(plain.supports()).thenReturn(new FeaturesBuilder(plain).setFolders(true).build());
        when(plain.readHistory("rules/Rating/pom.xml", "v1")).thenReturn(fileItem("<project>v1</project>"));
        var project = project(plain);
        var fileData = new FileData();
        fileData.setVersion("v1");
        when(project.getFileData()).thenReturn(fileData);
        when(project.isOpenedOtherVersion()).thenReturn(true);

        assertEquals("<project>v1</project>", ProjectPoms.inRepository(project, null, null).text());
    }

    /** A project at its latest revision reads its POMs at the head of the branch, as a build of the branch does. */
    @Test
    void readsThePomOfTheLatestRevisionAtTheHeadOfTheBranch() throws IOException {
        var plain = mock(Repository.class);
        when(plain.supports()).thenReturn(new FeaturesBuilder(plain).setFolders(true).build());
        when(plain.read("rules/Rating/pom.xml")).thenReturn(fileItem("<project>head</project>"));
        var project = project(plain);
        var fileData = new FileData();
        fileData.setVersion("v1");
        when(project.getFileData()).thenReturn(fileData);

        assertEquals("<project>head</project>", ProjectPoms.inRepository(project, null, null).text());
    }

    /** A repository keeping each project as one archive holds its pom.xml inside the archive, and none beside it. */
    @Test
    void readsThePomOfAProjectKeptAsAnArchiveFromTheArchive(@TempDir Path copy) throws Exception {
        var archives = mock(Repository.class);
        when(archives.supports()).thenReturn(new FeaturesBuilder(archives).build());
        var project = project(archives);
        var resource = mock(AProjectResource.class);
        when(resource.getContent()).thenAnswer(call -> new ByteArrayInputStream(
                "<project>archived</project>".getBytes(StandardCharsets.UTF_8)));
        when(project.hasArtefact(ProjectPom.FILE_NAME)).thenReturn(true);
        when(project.getArtefact(ProjectPom.FILE_NAME)).thenReturn(resource);

        var pom = ProjectPoms.inRepository(project, null, null);

        assertEquals("<project>archived</project>", pom.text());
        assertNull(pom.modelSource().getRelatedSource(".."));
        Files.writeString(copy.resolve(ProjectPom.FILE_NAME), "<project>edited</project>");
        assertEquals("<project>edited</project>", ProjectPoms.inRepository(project, copy, null).text());
    }

    /** A pom-less project has the pom.xml the OpenL Maven Plugin builds it with: its anchor is the parent. */
    @Test
    void aProjectWithoutAPomHasTheOneOfItsAnchor() throws Exception {
        Files.delete(root.resolve("rules/Rating/pom.xml"));
        Files.writeString(root.resolve("pom.xml"), anchor("<packaging>pom</packaging>"));

        var pom = ProjectPoms.inRepository(project(design),
                null,
                List.of(dependency("Common", " com.example:common:1.0.0 "), dependency(null, "com.example:lib:1.0.0")));

        var model = new MavenXpp3Reader().read(new StringReader(pom.text()));
        assertEquals("Rating", model.getArtifactId());
        assertEquals("com.example:rules-parent:1.0.0",
                model.getParent().getGroupId() + ":" + model.getParent().getArtifactId() + ":"
                        + model.getParent().getVersion());
        assertEquals("../../pom.xml", model.getParent().getRelativePath(), "the nearest pom.xml above the project");
        assertEquals("rules/Rating/pom.xml", pom.path());
        assertEquals(Map.of("Common", "com.example:common:1.0.0"), pom.projects(), "the projects rules.xml names");
        assertEquals(anchor("<packaging>pom</packaging>"), new String(pom.modelSource()
                .getRelatedSource(model.getParent().getRelativePath())
                .getInputStream()
                .readAllBytes(), StandardCharsets.UTF_8));
        assertNull(ProjectPoms.inFolder(root.resolve("rules/Rating"), root), "a folder on disk has no anchor");
    }

    /** The plugin scans no further than the nearest pom.xml, which builds pom-less projects only as an anchor. */
    @Test
    void aProjectWithoutAPomHasNoneWithoutAnAnchorNearestAboveIt() throws IOException {
        Files.delete(root.resolve("rules/Rating/pom.xml"));
        assertNull(ProjectPoms.inRepository(project(design), null, null), "no pom.xml above");

        Files.writeString(root.resolve("pom.xml"), anchor("<packaging>pom</packaging>"));
        Files.writeString(root.resolve("rules/pom.xml"), """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>modules</artifactId>
                    <version>1.0.0</version>
                    <packaging>pom</packaging>
                    <build>
                        <pluginManagement>
                            <plugins>
                                <plugin>
                                    <groupId>org.openl.rules</groupId>
                                    <artifactId>openl-maven-plugin</artifactId>
                                </plugin>
                            </plugins>
                        </pluginManagement>
                    </build>
                </project>
                """);
        assertNull(ProjectPoms.inRepository(project(design), null, null),
                "the nearest one manages the plugin, but does not build with it");

        Files.writeString(root.resolve("rules/pom.xml"), anchor("<packaging>openl</packaging>"));
        assertNull(ProjectPoms.inRepository(project(design), null, null), "an OpenL project is no anchor");

        Files.writeString(root.resolve("rules/pom.xml"), "<project>");
        assertNull(ProjectPoms.inRepository(project(design), null, null), "an unreadable one neither");
    }

    /** The plugin scans no folder named target or starting with a dot, and goes no deeper than another OpenL project. */
    @Test
    void aProjectTheScanOfItsAnchorDoesNotReachHasNoPom() throws IOException {
        Files.delete(root.resolve("rules/Rating/pom.xml"));
        Files.writeString(root.resolve("pom.xml"), anchor("<packaging>pom</packaging>"));
        Files.writeString(root.resolve("rules/rules.xml"), "<project/>");
        assertNull(ProjectPoms.inRepository(project(design), null, null), "inside another OpenL project");

        Files.delete(root.resolve("rules/rules.xml"));
        assertNotNull(ProjectPoms.inRepository(project(design), null, null));
        for (var folder : List.of("target/Rating", ".hidden/Rating", "rules/.Rating", "rules/target")) {
            Files.createDirectories(root.resolve(folder));
            assertNull(ProjectPoms.inRepository(project(design, folder), null, null), folder);
        }
    }

    /** Maven takes no space nor parenthesis in an id, which the folder of a project may hold. */
    @Test
    void aProjectWithoutAPomIsNamedAsMavenTakesIt() throws Exception {
        Files.createDirectories(root.resolve("rules/Bank Rating (2)"));
        Files.writeString(root.resolve("pom.xml"), anchor("<packaging>pom</packaging>"));

        var pom = ProjectPoms.inRepository(project(design, "rules/Bank Rating (2)"), null, null);

        var model = new MavenXpp3Reader().read(new StringReader(pom.text()));
        assertEquals("Bank_Rating__2_", model.getArtifactId());
        assertEquals("rules/Bank Rating (2)/pom.xml", pom.path());
    }

    private static String anchor(String packaging) {
        return """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>rules-parent</artifactId>
                    <version>1.0.0</version>
                    %s
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.openl.rules</groupId>
                                <artifactId>openl-maven-plugin</artifactId>
                                <extensions>true</extensions>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """.formatted(packaging);
    }

    private static ProjectDependencyDescriptor dependency(String name, String coordinates) {
        var dependency = new ProjectDependencyDescriptor();
        dependency.setName(name);
        dependency.setMavenArtifact(coordinates);
        return dependency;
    }

    private static FileItem fileItem(String text) {
        return new FileItem("rules/Rating/pom.xml", new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void aProjectOfNoDesignRepositoryReadsEveryPomOnDisk() throws IOException {
        var project = project(design);
        when(project.isLocalOnly()).thenReturn(true);

        assertNull(ProjectPoms.inRepository(project, null, null));
        assertTrue(ProjectPoms.inRepository(project, root.resolve("rules/Rating"), null).toString().startsWith("file:"),
                "read from the disk");
    }

    private static RulesProject project(Repository repository) {
        return project(repository, "rules/Rating");
    }

    private static RulesProject project(Repository repository, String path) {
        var project = mock(RulesProject.class);
        when(project.getDesignRepository()).thenReturn(repository);
        when(project.getRealPath()).thenReturn(path);
        return project;
    }
}
