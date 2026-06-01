package org.openl.studio.projects.service.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.projects.model.resources.ProjectFileLookupResponse;

class ProjectFileLookupServiceImplTest {

    private static final String LOOKUP_PATH = "AGENTS.md";

    private AclProjectsHelper aclProjectsHelper;
    private RulesProject project;
    private Repository repository;

    private ProjectFileLookupServiceImpl service;

    @BeforeEach
    void setUp() {
        aclProjectsHelper = mock(AclProjectsHelper.class);
        project = mock(RulesProject.class);
        repository = mock(Repository.class);

        when(project.getRepository()).thenReturn(repository);
        lenient().when(aclProjectsHelper.hasPermission(any(RulesProject.class), eq(BasePermission.READ)))
                .thenReturn(true);
        lenient().when(aclProjectsHelper.hasPermission(any(AProjectArtefact.class), eq(BasePermission.READ)))
                .thenReturn(true);

        service = new ProjectFileLookupServiceImpl(aclProjectsHelper);
    }

    // --- ACL ---

    @Test
    void lookup_noReadPermission_throwsForbidden() {
        when(aclProjectsHelper.hasPermission(project, BasePermission.READ)).thenReturn(false);

        assertThrows(ForbiddenException.class,
                () -> service.lookup(project, LOOKUP_PATH, false, false));
    }

    // --- Path validation ---

    @ParameterizedTest(name = "[{index}] invalid path: \"{0}\"")
    @NullSource
    @ValueSource(strings = {
            "   ",                       // blank
            "/AGENTS.md",                // absolute
            "../AGENTS.md",              // parent traversal
            "config/../../AGENTS.md",    // embedded parent traversal
            "config\\AGENTS.md"          // backslash separator
    })
    void lookup_invalidPath_throwsBadRequest(String path) {
        configureProjectRoot("services/rating", true);

        assertThrows(BadRequestException.class,
                () -> service.lookup(project, path, false, false));
    }

    // --- searchParents=false ---

    @Test
    void lookup_noSearchParents_fileFoundInProject_returnsSingleMatch() throws IOException {
        configureProjectRoot("services/rating", true);
        addProjectFile(project, "AGENTS.md", "hello");

        ProjectFileLookupResponse response = service.lookup(project, "AGENTS.md", false, false);

        assertEquals(1, response.files().size());
        assertEquals("services/rating/AGENTS.md", response.files().getFirst().path());
        assertNull(response.files().getFirst().content(), "content should be omitted when includeContent=false");
    }

    @Test
    void lookup_noSearchParents_fileNotFound_returnsEmpty() throws IOException {
        configureProjectRoot("services/rating", true);

        ProjectFileLookupResponse response = service.lookup(project, "AGENTS.md", false, false);

        assertNotNull(response.files());
        assertTrue(response.files().isEmpty());
    }

    @Test
    void lookup_noSearchParents_includeContentTrue_returnsContent() throws IOException {
        configureProjectRoot("services/rating", true);
        addProjectFile(project, "AGENTS.md", "hello world");

        ProjectFileLookupResponse response = service.lookup(project, "AGENTS.md", false, true);

        assertEquals(1, response.files().size());
        assertEquals("hello world", response.files().getFirst().content());
    }

    @Test
    void lookup_noSearchParents_doesNotWalkAncestors() throws IOException {
        configureProjectRoot("services/rating", true);
        // Ancestor file exists, but searchParents=false must not see it.
        when(repository.check("services/AGENTS.md"))
                .thenReturn(fileData("services/AGENTS.md"));

        ProjectFileLookupResponse response = service.lookup(project, "AGENTS.md", false, false);

        assertTrue(response.files().isEmpty());
        verify(repository, never()).check("services/AGENTS.md");
    }

    @Test
    void lookup_noSearchParents_pathIsFolder_returnsEmpty() throws IOException {
        configureProjectRoot("services/rating", true);
        var folder = mock(AProjectFolder.class);
        when(folder.isFolder()).thenReturn(true);
        Map<String, AProjectArtefact> artefacts = new HashMap<>();
        artefacts.put("AGENTS.md", folder);
        stubArtefacts(project, artefacts);

        ProjectFileLookupResponse response = service.lookup(project, "AGENTS.md", false, false);

        assertTrue(response.files().isEmpty());
    }

    @Test
    void lookup_noSearchParents_noReadPermissionOnArtefact_returnsEmpty() throws IOException {
        configureProjectRoot("services/rating", true);
        var resource = addProjectFile(project, "AGENTS.md", "x");
        when(aclProjectsHelper.hasPermission(resource, BasePermission.READ)).thenReturn(false);

        ProjectFileLookupResponse response = service.lookup(project, "AGENTS.md", false, false);

        assertTrue(response.files().isEmpty());
    }

    // --- searchParents=true ---

    @Test
    void lookup_searchParents_collectsFromNearestToFarthest() throws IOException {
        configureProjectRoot("services/rating", true);
        addProjectFile(project, "AGENTS.md", "rating");
        when(repository.check("services/AGENTS.md")).thenReturn(fileData("services/AGENTS.md"));
        when(repository.check("AGENTS.md")).thenReturn(fileData("AGENTS.md"));

        ProjectFileLookupResponse response = service.lookup(project, "AGENTS.md", true, false);

        assertEquals(3, response.files().size());
        assertEquals("services/rating/AGENTS.md", response.files().get(0).path());
        assertEquals("services/AGENTS.md", response.files().get(1).path());
        assertEquals("AGENTS.md", response.files().get(2).path());
    }

    @Test
    void lookup_searchParents_skipsMissingAncestors() throws IOException {
        configureProjectRoot("services/rating", true);
        addProjectFile(project, "AGENTS.md", "rating");
        // services/AGENTS.md missing; AGENTS.md at root exists
        when(repository.check("AGENTS.md")).thenReturn(fileData("AGENTS.md"));

        ProjectFileLookupResponse response = service.lookup(project, "AGENTS.md", true, false);

        assertEquals(2, response.files().size());
        assertEquals("services/rating/AGENTS.md", response.files().get(0).path());
        assertEquals("AGENTS.md", response.files().get(1).path());
    }

    @Test
    void lookup_searchParents_skipsDeletedAncestors() throws IOException {
        configureProjectRoot("services/rating", true);
        var deleted = fileData("AGENTS.md");
        deleted.setDeleted(true);
        when(repository.check("AGENTS.md")).thenReturn(deleted);

        ProjectFileLookupResponse response = service.lookup(project, "AGENTS.md", true, false);

        assertTrue(response.files().isEmpty());
    }

    @Test
    void lookup_searchParents_nothingFoundAnywhere_returnsEmpty() throws IOException {
        configureProjectRoot("services/rating", true);

        ProjectFileLookupResponse response = service.lookup(project, "AGENTS.md", true, false);

        assertNotNull(response.files());
        assertTrue(response.files().isEmpty());
    }

    @Test
    void lookup_searchParents_stopsAtRepositoryRoot() throws IOException {
        configureProjectRoot("services/rating", true);
        when(repository.check("services/AGENTS.md")).thenReturn(fileData("services/AGENTS.md"));
        when(repository.check("AGENTS.md")).thenReturn(fileData("AGENTS.md"));

        service.lookup(project, "AGENTS.md", true, false);

        // Only two ancestors are visited; we never look above the repository root.
        verify(repository).check("services/AGENTS.md");
        verify(repository).check("AGENTS.md");
        verify(repository, never()).check("/AGENTS.md");
        verify(repository, never()).check("../AGENTS.md");
    }

    @Test
    void lookup_searchParents_projectAtRepositoryRoot_noWalk() throws IOException {
        configureProjectRoot("", true);
        addProjectFile(project, "AGENTS.md", "x");

        ProjectFileLookupResponse response = service.lookup(project, "AGENTS.md", true, false);

        assertEquals(1, response.files().size());
        assertEquals("AGENTS.md", response.files().getFirst().path());
        verify(repository, never()).check(any());
    }

    @Test
    void lookup_searchParents_flatRepository_noWalk() throws IOException {
        configureProjectRoot("services/rating", false);
        addProjectFile(project, "AGENTS.md", "x");

        ProjectFileLookupResponse response = service.lookup(project, "AGENTS.md", true, false);

        assertEquals(1, response.files().size());
        assertEquals("services/rating/AGENTS.md", response.files().getFirst().path());
        verify(repository, never()).check(any());
    }

    @Test
    void lookup_searchParents_includeContent_readsAncestorContent() throws IOException {
        configureProjectRoot("services/rating", true);
        addProjectFile(project, "AGENTS.md", "in-project");

        when(repository.check("services/AGENTS.md")).thenReturn(fileData("services/AGENTS.md"));
        when(repository.read("services/AGENTS.md"))
                .thenReturn(fileItem("ancestor-content"));

        ProjectFileLookupResponse response = service.lookup(project, "AGENTS.md", true, true);

        assertEquals(2, response.files().size());
        assertEquals("in-project", response.files().get(0).content());
        assertEquals("ancestor-content", response.files().get(1).content());
    }

    @Test
    void lookup_searchParents_nestedRelativePath_walksDirsByLeafName() throws IOException {
        configureProjectRoot("services/rating", true);
        // Project artefacts include AGENTS.md at the project root and nested under config/.
        // The within-project walk resolves "AGENTS.md" via the project's artefact tree.
        addProjectFiles(project,
                "AGENTS.md", "at-project-root",
                "config/AGENTS.md", "in-config");

        // Once the walk leaves the project boundary, ancestors are resolved via the raw repository.
        when(repository.check("services/AGENTS.md")).thenReturn(fileData("services/AGENTS.md"));
        when(repository.check("AGENTS.md")).thenReturn(fileData("AGENTS.md"));

        ProjectFileLookupResponse response = service.lookup(project, "config/AGENTS.md", true, false);

        assertEquals(4, response.files().size());
        assertEquals("services/rating/config/AGENTS.md", response.files().get(0).path());
        assertEquals("services/rating/AGENTS.md", response.files().get(1).path());
        assertEquals("services/AGENTS.md", response.files().get(2).path());
        assertEquals("AGENTS.md", response.files().get(3).path());
    }

    @Test
    void lookup_searchParents_nestedPath_flatRepository_walksWithinProjectOnly() throws IOException {
        configureProjectRoot("EPBDS-16012-db", false); // flat (e.g. JDBC) repo, no folders feature
        addProjectFiles(project,
                "AGENTS.md", "at-project-root",
                "services/AGENTS.md", "in-services",
                "services/rating/AGENTS.md", "in-rating");

        ProjectFileLookupResponse response = service.lookup(project, "services/rating/AGENTS.md", true, false);

        // Flat repos can't expose anything above the project, but the within-project walk
        // still climbs from services/rating up to the project root.
        assertEquals(3, response.files().size());
        assertEquals("EPBDS-16012-db/services/rating/AGENTS.md", response.files().get(0).path());
        assertEquals("EPBDS-16012-db/services/AGENTS.md", response.files().get(1).path());
        assertEquals("EPBDS-16012-db/AGENTS.md", response.files().get(2).path());
        verify(repository, never()).check(any());
    }

    // --- validation: non-text files, size, count ---

    @Test
    void lookup_nonTextExtension_returnsEmpty() throws IOException {
        configureProjectRoot("services/rating", true);
        // The artefact exists but the extension is not in the text whitelist.
        addProjectFile(project, "rules.xlsx", "binary");

        ProjectFileLookupResponse response = service.lookup(project, "rules.xlsx", false, false);

        assertTrue(response.files().isEmpty());
    }

    @Test
    void lookup_noExtension_returnsEmpty() throws IOException {
        configureProjectRoot("services/rating", true);
        addProjectFile(project, "LICENSE", "MIT");

        ProjectFileLookupResponse response = service.lookup(project, "LICENSE", false, false);

        assertTrue(response.files().isEmpty());
    }

    @Test
    void lookup_searchParents_nonTextAncestor_isSkipped() throws IOException {
        configureProjectRoot("services/rating", true);
        // The leaf is non-text, so the ancestor walk must skip it before ever touching the repository.

        ProjectFileLookupResponse response = service.lookup(project, "rules.xlsx", true, false);

        assertTrue(response.files().isEmpty());
        verify(repository, never()).check("services/rules.xlsx");
    }

    @Test
    void lookup_projectFileTooLarge_returnsEmpty() throws IOException {
        configureProjectRoot("services/rating", true);
        var resource = addProjectFile(project, "AGENTS.md", "x");
        var data = fileData("services/rating/AGENTS.md");
        data.setSize(ProjectFileLookupServiceImpl.MAX_FILE_SIZE_BYTES + 1);
        when(resource.getFileData()).thenReturn(data);

        ProjectFileLookupResponse response = service.lookup(project, "AGENTS.md", false, false);

        assertTrue(response.files().isEmpty());
    }

    @Test
    void lookup_includeContent_undefinedSizeButOversizeContent_isSkipped() throws IOException {
        configureProjectRoot("services/rating", true);
        // The repository reports no size up front (UNDEFINED_SIZE), but the content exceeds the cap.
        // The bounded read must reject it instead of loading the whole blob into memory.
        var oversize = "a".repeat((int) ProjectFileLookupServiceImpl.MAX_FILE_SIZE_BYTES + 1);
        addProjectFile(project, "AGENTS.md", oversize);

        ProjectFileLookupResponse response = service.lookup(project, "AGENTS.md", false, true);

        assertTrue(response.files().isEmpty());
    }

    @Test
    void lookup_searchParents_ancestorTooLarge_isSkipped() throws IOException {
        configureProjectRoot("services/rating", true);
        var large = fileData("AGENTS.md");
        large.setSize(ProjectFileLookupServiceImpl.MAX_FILE_SIZE_BYTES + 1);
        when(repository.check("AGENTS.md")).thenReturn(large);
        // services/AGENTS.md is a normal-sized ancestor and must still be returned.
        when(repository.check("services/AGENTS.md")).thenReturn(fileData("services/AGENTS.md"));

        ProjectFileLookupResponse response = service.lookup(project, "AGENTS.md", true, false);

        assertEquals(1, response.files().size());
        assertEquals("services/AGENTS.md", response.files().getFirst().path());
    }

    @Test
    void lookup_searchParents_countLimit_truncatesAncestorWalk() throws IOException {
        // Project sits very deep so the walk has more ancestors than the cap.
        var deep = "a" + "/a".repeat(ProjectFileLookupServiceImpl.MAX_FILES_COUNT + 5);
        configureProjectRoot(deep, true);
        addProjectFile(project, "AGENTS.md", "x");
        // Every ancestor has the file → without a cap we'd add way more than allowed.
        when(repository.check(any())).thenAnswer(invocation -> {
            String name = invocation.getArgument(0);
            return fileData(name);
        });

        ProjectFileLookupResponse response = service.lookup(project, "AGENTS.md", true, false);

        assertEquals(ProjectFileLookupServiceImpl.MAX_FILES_COUNT, response.files().size());
    }

    // --- helpers ---

    private void configureProjectRoot(String realPath, boolean supportsFolders) {
        when(project.getRealPath()).thenReturn(realPath);
        var features = new FeaturesBuilder(repository).setFolders(supportsFolders).build();
        when(repository.supports()).thenReturn(features);
        // No artefacts by default — concrete tests override this.
        when(project.isFolder()).thenReturn(true);
    }

    private AProjectResource addProjectFile(AProjectFolder folder,
                                            String name,
                                            String content) throws IOException {
        var resource = mock(AProjectResource.class);
        when(resource.isFolder()).thenReturn(false);
        try {
            when(resource.getContent())
                    .thenAnswer(invocation -> new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        } catch (ProjectException e) {
            throw new IllegalStateException(e);
        }
        var artefacts = new HashMap<String, AProjectArtefact>();
        artefacts.put(name, resource);
        stubArtefacts(folder, artefacts);
        return resource;
    }

    private void addProjectFiles(AProjectFolder folder, String... nameContentPairs) {
        if (nameContentPairs.length % 2 != 0) {
            throw new IllegalArgumentException("Expected (name, content) pairs");
        }
        var artefacts = new HashMap<String, AProjectArtefact>();
        for (int i = 0; i < nameContentPairs.length; i += 2) {
            String name = nameContentPairs[i];
            String content = nameContentPairs[i + 1];
            var resource = mock(AProjectResource.class);
            when(resource.isFolder()).thenReturn(false);
            try {
                when(resource.getContent())
                        .thenAnswer(invocation -> new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
            } catch (ProjectException e) {
                throw new IllegalStateException(e);
            }
            artefacts.put(name, resource);
        }
        stubArtefacts(folder, artefacts);
    }

    private static void stubArtefacts(AProjectFolder folder, Map<String, AProjectArtefact> artefacts) {
        when(folder.isFolder()).thenReturn(true);
        try {
            when(folder.getArtefact(any())).thenAnswer(invocation -> {
                String name = invocation.getArgument(0);
                var artefact = artefacts.get(name);
                if (artefact == null) {
                    throw new ProjectException("Cannot find ''{0}''", null, name);
                }
                return artefact;
            });
        } catch (ProjectException e) {
            throw new IllegalStateException(e);
        }
    }

    private static FileData fileData(String name) {
        var data = new FileData();
        data.setName(name);
        return data;
    }

    private static FileItem fileItem(String content) {
        return new FileItem(fileData("ignored"), new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }
}
