package org.openl.studio.dependencies.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.dependencies.MavenDependencyResolver;
import org.openl.studio.dependencies.model.MavenProjectSummary;

class MavenProjectViewerTest {

    @TempDir
    private Path temp;

    private final WebStudio studio = mock(WebStudio.class);
    private MavenDependencyResolver resolver;
    private MavenProjectViewer viewer;

    @BeforeEach
    void setUp() throws IOException {
        var remote = Files.createDirectories(temp.resolve("remote"));
        publishProject(remote.resolve("com/example/domain/1.0.0/domain-1.0.0.zip"), """
                <project><name>Domain</name><dependencies>\
                <dependency><name>Shared</name><mavenArtifact>com.example:shared:1.0.0</mavenArtifact></dependency>\
                <dependency><mavenArtifact>org.example:lib:jar:1.0</mavenArtifact></dependency>\
                </dependencies></project>""");
        publishProject(remote.resolve("com/example/shared/1.0.0/shared-1.0.0.zip"),
                "<project><name>Shared</name></project>");
        publishProject(remote.resolve("com/example/other/2.0.0/other-2.0.0.zip"),
                "<project><name>Other</name></project>");
        resolver = new MavenDependencyResolver(remote.toUri().toString(), null, null, temp.resolve("local").toString());
        when(studio.getMavenDependencyResolver()).thenReturn(resolver);
        when(studio.getAllProjects())
                .thenReturn(List.of(consumer("com.example:domain:1.0.0"), consumer("com.example:missing:1.0.0")));
        viewer = new MavenProjectViewer() {
            @Override
            public WebStudio getWebStudio() {
                return studio;
            }
        };
    }

    @AfterEach
    void tearDown() {
        resolver.destroy();
    }

    @Test
    void listsTheProjectsTheWorkspaceDependsOnThatCanBeDownloaded() {
        var projects = viewer.projects();

        assertEquals(List.of(new MavenProjectSummary("com.example", "domain", "1.0.0", "Domain"),
                new MavenProjectSummary("com.example", "shared", "1.0.0", "Shared")), projects,
                "a project a listed one depends on is listed, the library and a project no one names are not");
    }

    @Test
    void listsNoProjectOfAClassifiedArchiveAndFollowsItsDependencies() throws IOException {
        publishProject(temp.resolve("remote/com/example/rated/1.0.0/rated-1.0.0-rules.zip"), """
                <project><name>Rated</name><dependencies>\
                <dependency><name>Shared</name><mavenArtifact>com.example:shared:1.0.0</mavenArtifact></dependency>\
                </dependencies></project>""");
        var dependency = new ProjectDependencyDescriptor();
        dependency.setName("Rated");
        dependency.setMavenArtifact("com.example:rated:zip:rules:1.0.0");
        var project = new ProjectDescriptor();
        project.setName("Consumer of Rated");
        project.setDependencies(List.of(dependency));
        when(studio.getAllProjects()).thenReturn(List.of(project));

        assertEquals(List.of(new MavenProjectSummary("com.example", "shared", "1.0.0", "Shared")), viewer.projects(),
                "its coordinates would open the plain archive, another one");
    }

    @Test
    void listsNoProjectWhileNoMavenRepositoryIsSet() {
        when(studio.getMavenDependencyResolver()).thenReturn(null);

        assertEquals(List.of(), viewer.projects());
    }

    /** A project of the workspace that depends on the project the coordinates name. */
    private static ProjectDescriptor consumer(String coordinates) {
        var dependency = new ProjectDependencyDescriptor();
        dependency.setName(coordinates.contains("missing") ? "Missing" : "Domain");
        dependency.setMavenArtifact(coordinates);
        var project = new ProjectDescriptor();
        project.setName("Consumer of " + dependency.getName());
        project.setDependencies(List.of(dependency));
        return project;
    }

    /** An archive of a project holding the rules.xml given. */
    private static void publishProject(Path archive, String rulesXml) throws IOException {
        Files.createDirectories(archive.getParent());
        try (var zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("rules.xml"));
            zip.write(rulesXml.getBytes());
            zip.closeEntry();
        }
    }
}
