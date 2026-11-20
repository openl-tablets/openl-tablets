package org.openl.rules.webstudio.dependencies;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.impl.local.DummyLockEngine;
import org.openl.rules.project.impl.local.MetainfoRegistry;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.common.exception.NotFoundException;

/**
 * A project of the Maven repository is downloaded on request and opened read-only in the session, under the id of its
 * coordinates.
 *
 * @author Yury Molchan
 */
class DownloadedProjectsTest {

    private static final String ID = "maven:com.example:domain:1.0.0";
    private static final String COORDINATES = "com.example:domain:1.0.0";

    @TempDir
    Path temp;

    private final MavenDependencyResolver resolver = mock(MavenDependencyResolver.class);
    private final UserWorkspace workspace = mock(UserWorkspace.class);
    private final DownloadedProjects projects = new DownloadedProjects();

    @BeforeEach
    void setUp() throws IOException {
        var localWorkspace = mock(LocalWorkspace.class);
        when(localWorkspace.getMetainfoRegistry()).thenReturn(MetainfoRegistry.open(temp.resolve("user")));
        when(workspace.getLocalWorkspace()).thenReturn(localWorkspace);
        when(workspace.getUser()).thenReturn(new WorkspaceUserImpl("jdoe", id -> new UserInfo("jdoe")));
        when(workspace.getProjectsLockEngine()).thenReturn(new DummyLockEngine());
        when(resolver.isEnabled()).thenReturn(true);
    }

    /** The project unpacked into the folder, as the resolver describes a download. */
    private ProjectDescriptor downloadedInto(String folderName) throws IOException {
        var folder = Files.createDirectories(temp.resolve("downloads").resolve(folderName));
        Files.writeString(folder.resolve("rules.xml"), "<project><name>Domain</name></project>");
        var descriptor = new ProjectDescriptor();
        descriptor.setName("Domain");
        descriptor.setProjectFolder(folder);
        return descriptor;
    }

    private void resolves(ProjectDescriptor descriptor) {
        when(resolver.projectAt(COORDINATES)).thenReturn(new MavenDependencyResolver.DownloadedProject(descriptor,
                null));
    }

    @Test
    void opensTheDownloadedProjectReadOnlyUnderTheIdOfItsCoordinates() throws Exception {
        resolves(downloadedInto("domain-1.0.0"));

        var project = projects.open(ID, resolver, workspace);

        assertInstanceOf(DownloadedRulesProject.class, project);
        assertTrue(project.isReadOnly());
        assertTrue(project.isLocalOnly(), "no design repository holds it");
        assertEquals(ID, project.getProjectId().value());
        assertEquals(ID, project.getRepository().getId());
        assertEquals("Maven", project.getRepository().getName());
        assertEquals("domain-1.0.0", project.getFolderPath());
        var rulesXml = (AProjectResource) project.getArtefact("rules.xml");
        try (var content = rulesXml.getContent()) {
            assertEquals("<project><name>Domain</name></project>", new String(content.readAllBytes()));
        }
    }

    @Test
    void isNamedAsItsRulesXmlNamesIt() throws Exception {
        resolves(downloadedInto("domain-1.0.0"));

        var project = projects.open(ID, resolver, workspace);

        assertEquals("Domain", project.getName());
        assertEquals("Domain", project.getBusinessName());
        assertEquals("Domain", project.getMainBusinessName());
    }

    @Test
    void findsTheOpenedProjectByItsNameOrItsFolder() throws Exception {
        var descriptor = downloadedInto("domain-1.0.0");
        resolves(descriptor);
        assertFalse(projects.holds(ID), "nothing is opened before it is asked for");

        var project = projects.open(ID, resolver, workspace);

        assertTrue(projects.holds(ID));
        assertFalse(projects.holds("design"));
        assertSame(project, projects.project(ID, "Domain"));
        assertSame(project, projects.project(ID, "domain-1.0.0"));
        assertNull(projects.project(ID, "Other"));
        assertNull(projects.project("design", "Domain"));
        assertSame(descriptor, projects.descriptor(ID, "Domain"));
        assertSame(descriptor, projects.descriptor(ID, "domain-1.0.0"));
        assertNull(projects.descriptor(ID, "Other"));
        assertNull(projects.descriptor("design", "Domain"));
    }

    @Test
    void keepsTheProjectWhileItStaysTheSameDownload() throws Exception {
        resolves(downloadedInto("domain-1.0.0"));
        var first = projects.open(ID, resolver, workspace);
        // The resolver describes the same download anew
        resolves(downloadedInto("domain-1.0.0"));

        assertSame(first, projects.open(ID, resolver, workspace));
    }

    /** A snapshot is downloaded again into a folder of its own: the session opens the new download. */
    @Test
    void opensANewDownloadAnew() throws Exception {
        resolves(downloadedInto("domain-1.0.0"));
        var first = projects.open(ID, resolver, workspace);
        var renewed = downloadedInto("domain-1.0.0-renewed");
        resolves(renewed);

        var second = projects.open(ID, resolver, workspace);

        assertNotSame(first, second);
        assertEquals("domain-1.0.0-renewed", second.getFolderPath());
        assertSame(second, projects.project(ID, "Domain"));
        assertSame(renewed, projects.descriptor(ID, "Domain"));
    }

    @Test
    void answersNothingForAnIdOfNoDownloadedProject() {
        assertNull(projects.open("ZGVzaWduOkJhbms=", resolver, workspace));
        assertNull(projects.open("maven:com.example:domain", resolver, workspace));
        verifyNoInteractions(resolver);
    }

    @Test
    void refusesWhileNoMavenRepositoryIsSet() {
        var refusal = assertThrows(NotFoundException.class, () -> projects.open(ID, null, workspace));
        assertEquals("openl.error.404.dependency.maven.no-repository.message", refusal.getErrorCode());
        assertArrayEquals(new Object[]{COORDINATES}, refusal.getArgs());

        when(resolver.isEnabled()).thenReturn(false);
        var disabled = assertThrows(NotFoundException.class, () -> projects.open(ID, resolver, workspace));
        assertEquals("openl.error.404.dependency.maven.no-repository.message", disabled.getErrorCode());
        verify(resolver, never()).projectAt(any());
    }

    @Test
    void refusesAProjectThatCannotBeDownloadedAndSaysWhy() {
        when(resolver.projectAt(COORDINATES)).thenReturn(new MavenDependencyResolver.DownloadedProject(null,
                new OpenLMessage("Could not find artifact", Severity.ERROR)));

        var refusal = assertThrows(NotFoundException.class, () -> projects.open(ID, resolver, workspace));

        assertEquals("openl.error.404.dependency.maven.not-downloaded.message", refusal.getErrorCode());
        assertArrayEquals(new Object[]{COORDINATES, "Could not find artifact"}, refusal.getArgs());
        assertFalse(projects.holds(ID));
    }

    @Test
    void refusesAProjectThatCannotBeDownloadedWithNoReasonGiven() {
        when(resolver.projectAt(COORDINATES)).thenReturn(new MavenDependencyResolver.DownloadedProject(null, null));

        var refusal = assertThrows(NotFoundException.class, () -> projects.open(ID, resolver, workspace));

        assertArrayEquals(new Object[]{COORDINATES, ""}, refusal.getArgs());
    }

    /** The project as the resolver downloads it from a Maven repository. */
    @Test
    void opensAProjectDownloadedFromAMavenRepository() throws Exception {
        var remote = Files.createDirectories(temp.resolve("remote"));
        publish(remote.resolve("com/example/domain/1.0.0/domain-1.0.0.zip"),
                Map.of("rules.xml", "<project><name>Domain</name></project>", "rules/Main.txt", "main"));
        var maven = new MavenDependencyResolver(remote.toUri().toString(), null, null,
                temp.resolve("local").toString());
        try {
            var project = projects.open(ID, maven, workspace);

            assertNotNull(project);
            assertEquals("Domain", project.getName());
            assertTrue(project.hasArtefact("rules/Main.txt"));
            assertTrue(Files.isRegularFile(project.getLocalRepository().getRoot()
                    .resolve(project.getFolderPath())
                    .resolve("rules.xml")));
        } finally {
            maven.destroy();
        }
    }

    private static void publish(Path file, Map<String, String> entries) throws IOException {
        Files.createDirectories(file.getParent());
        try (var zip = new ZipOutputStream(Files.newOutputStream(file))) {
            for (var entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes());
                zip.closeEntry();
            }
        }
    }
}
