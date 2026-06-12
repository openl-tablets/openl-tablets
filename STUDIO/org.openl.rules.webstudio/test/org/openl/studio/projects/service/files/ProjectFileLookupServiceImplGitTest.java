package org.openl.studio.projects.service.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.git.GitRepositoryFactory;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.studio.projects.model.files.FileNode;
import org.openl.studio.projects.model.files.FsNode;
import org.openl.util.IOUtils;

/**
 * Verifies {@link ProjectFileLookupServiceImpl} against an on-disk repository with a real folder
 * hierarchy.
 *
 * <p>The repository holds {@code AGENTS.md} at every level — repository root, intermediate ancestor,
 * project root, and a nested project folder — plus a non-text {@code rules.xlsx}:
 * <pre>
 *   AGENTS.md                        (repository root)
 *   services/
 *     AGENTS.md                      (intermediate ancestor)
 *     rating/
 *       AGENTS.md                    (project root)
 *       rules.xlsx                   (other project file)
 *       config/
 *         AGENTS.md                  (nested project file)
 * </pre>
 *
 * <p>The lookup walks up from the anchor to the repository root, collecting the same-named file at
 * each level, ordered nearest first; it crosses the project boundary but does not descend into
 * subfolders. The nested {@code config/} file therefore appears only when the anchor sits at or below
 * it.
 */
class ProjectFileLookupServiceImplGitTest {

    @TempDir
    private File remoteRoot;
    @TempDir
    private File localRepositoriesFolder;

    private Repository repository;
    private RepositoryAclService aclService;
    private ProjectFileLookupServiceImpl service;

    @BeforeEach
    void setUp() throws GitAPIException, IOException {
        seedRemoteRepository();
        repository = openGitRepository();
        AclProjectsHelper aclProjectsHelper = mock(AclProjectsHelper.class);
        lenient().when(aclProjectsHelper.hasPermission(any(AProjectArtefact.class), eq(BasePermission.READ)))
                .thenReturn(true);
        aclService = mock(RepositoryAclService.class);
        lenient().when(aclService.isGranted(anyString(), anyString(), anyBoolean(), any(Permission.class)))
                .thenReturn(true);
        var aclProvider = mock(RepositoryAclServiceProvider.class);
        lenient().when(aclProvider.getDesignRepoAclService()).thenReturn(aclService);
        service = new ProjectFileLookupServiceImpl(aclProjectsHelper, aclProvider);
    }

    @AfterEach
    void tearDown() {
        if (repository != null) {
            IOUtils.closeQuietly(repository);
        }
    }

    // --- scenarios ---

    @Test
    void collectsAncestorsUpToRoot_nearestFirst() throws IOException {
        List<FsNode> files = service.lookup(repository, "services/rating/AGENTS.md", true);

        // Walk up from services/rating: the file itself (d0), the intermediate ancestor (d1), the
        // repository root (d2). The nested config/ file is a descendant and is not visited.
        assertEquals(3, files.size());
        assertEquals("services/rating/AGENTS.md", files.get(0).getPath());
        assertEquals("# project agents", content(files.get(0)));
        assertEquals("services/AGENTS.md", files.get(1).getPath());
        assertEquals("# services agents", content(files.get(1)));
        assertEquals("AGENTS.md", files.get(2).getPath());
        assertEquals("# root agents", content(files.get(2)));
    }

    @Test
    void includeContentFalse_returnsMetadataOnly() throws IOException {
        List<FsNode> files = service.lookup(repository, "services/rating/AGENTS.md", false);

        assertEquals(3, files.size());
        assertNull(content(files.getFirst()), "content should be omitted when includeContent=false");
    }

    @Test
    void anchorAtRepositoryRoot_returnsRootOnly() throws IOException {
        List<FsNode> files = service.lookup(repository, "AGENTS.md", false);

        // There is nothing above the repository root, and descendants are not visited.
        assertEquals(1, files.size());
        assertEquals("AGENTS.md", files.get(0).getPath());
    }

    @Test
    void anchorAtNestedFolder_ordersFromThatFolder() throws IOException {
        List<FsNode> files = service.lookup(repository, "services/rating/config/AGENTS.md", false);

        // The deepest anchor reaches every level above it, up to the repository root.
        assertEquals(4, files.size());
        assertEquals("services/rating/config/AGENTS.md", files.get(0).getPath());
        assertEquals("services/rating/AGENTS.md", files.get(1).getPath());
        assertEquals("services/AGENTS.md", files.get(2).getPath());
        assertEquals("AGENTS.md", files.get(3).getPath());
    }

    @Test
    void missingFile_returnsEmpty() throws IOException {
        assertTrue(service.lookup(repository, "services/rating/MISSING.md", true).isEmpty());
    }

    @Test
    void nonTextFile_returnsEmpty() throws IOException {
        assertTrue(service.lookup(repository, "services/rating/rules.xlsx", true).isEmpty());
    }

    @Test
    void projectMount_combinesInProjectTreeAndOutsideRepository() throws IOException {
        // The services/rating project tree supplies its own AGENTS.md (project root); the design
        // repository supplies the ancestors above the project (services + repository root). The nested
        // config/ file is a descendant and is not visited.
        var project = new AProject(repository, repository.check("services/rating"));

        List<FsNode> files = service.lookup(project, repository, "services/rating/AGENTS.md", true);

        assertEquals(3, files.size());
        assertEquals("services/rating/AGENTS.md", files.get(0).getPath());
        assertEquals("# project agents", content(files.get(0)));
        assertEquals("services/AGENTS.md", files.get(1).getPath());
        assertEquals("AGENTS.md", files.get(2).getPath());
    }

    @Test
    void localProject_nullRepository_searchesWithinProjectOnly() throws IOException {
        // A local-only project has no design repository; the walk up stays within the project
        // (config -> project root) and never reaches files above it (services, repository root).
        var project = new AProject(repository, repository.check("services/rating"));

        List<FsNode> files = service.lookup(project, null, "services/rating/config/AGENTS.md", true);

        assertEquals(2, files.size());
        assertEquals("services/rating/config/AGENTS.md", files.get(0).getPath());
        assertEquals("services/rating/AGENTS.md", files.get(1).getPath());
    }

    @Test
    void excludesFilesTheUserCannotRead() throws IOException {
        // Deny READ on the repository-root AGENTS.md: it must not appear among the ancestors.
        when(aclService.isGranted(anyString(), eq("AGENTS.md"), anyBoolean(), any(Permission.class)))
                .thenReturn(false);

        List<FsNode> files = service.lookup(repository, "services/rating/AGENTS.md", false);

        assertEquals(List.of("services/rating/AGENTS.md", "services/AGENTS.md"),
                files.stream().map(FsNode::getPath).toList());
    }

    private static String content(FsNode node) {
        return ((FileNode) node).getContent();
    }

    // --- helpers ---

    private void seedRemoteRepository() throws GitAPIException, IOException {
        try (Git git = Git.init().setDirectory(remoteRoot).call()) {
            File root = git.getRepository().getDirectory().getParentFile();
            writeFile(new File(root, "AGENTS.md"), "# root agents");
            writeFile(new File(root, "services/AGENTS.md"), "# services agents");
            writeFile(new File(root, "services/rating/AGENTS.md"), "# project agents");
            writeFile(new File(root, "services/rating/rules.xlsx"), "fake xlsx body");
            writeFile(new File(root, "services/rating/config/AGENTS.md"), "# nested project agents");
            git.add().addFilepattern(".").call();
            git.commit()
                    .setMessage("Seed AGENTS lookup fixture")
                    .setCommitter("Test", "test@openl.org")
                    .call();
        }
    }

    private Repository openGitRepository() {
        return new GitRepositoryFactory().create(key -> switch (key) {
            case "id" -> "design";
            case "uri" -> remoteRoot.toURI().toString();
            case "local-repositories-folder" -> localRepositoriesFolder.getAbsolutePath();
            case "comment-template" -> "OpenL Studio: {commit-type}. {user-message}";
            default -> null;
        });
    }

    private static void writeFile(File file, String content) throws IOException {
        Path path = file.toPath();
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
