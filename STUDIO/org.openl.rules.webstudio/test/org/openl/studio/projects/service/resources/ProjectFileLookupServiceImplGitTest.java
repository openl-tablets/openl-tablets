package org.openl.studio.projects.service.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.git.GitRepositoryFactory;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.studio.projects.model.resources.ProjectFileLookupResponse;
import org.openl.util.IOUtils;

/**
 * Integration test that drives {@link ProjectFileLookupServiceImpl} against a real Git
 * repository on disk (created with JGit, opened via {@link GitRepositoryFactory}).
 *
 * <p>The repository tree seeded in {@link #setUp()} looks like:
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
 * <p>The {@link AProject} is constructed directly on top of the repository — no
 * {@code MappedRepository}, no Spring context — so the project's folder path equals
 * the repository-relative path.
 */
class ProjectFileLookupServiceImplGitTest {

    private static final String PROJECT_FOLDER = "services/rating";

    @TempDir
    private File remoteRoot;
    @TempDir
    private File localRepositoriesFolder;

    private Repository repository;
    private AProject project;
    private ProjectFileLookupServiceImpl service;

    @BeforeEach
    void setUp() throws GitAPIException, IOException {
        seedRemoteRepository();
        repository = openGitRepository();
        FileData projectData = repository.check(PROJECT_FOLDER);
        assertNotNull(projectData, "Test setup error: project folder not committed");
        project = new AProject(repository, projectData);

        AclProjectsHelper aclProjectsHelper = mock(AclProjectsHelper.class);
        lenient().when(aclProjectsHelper.hasPermission(any(AProject.class), eq(BasePermission.READ)))
                .thenReturn(true);
        lenient().when(aclProjectsHelper.hasPermission(any(AProjectArtefact.class), eq(BasePermission.READ)))
                .thenReturn(true);
        service = new ProjectFileLookupServiceImpl(aclProjectsHelper);
    }

    @AfterEach
    void tearDown() {
        if (repository != null) {
            IOUtils.closeQuietly(repository);
        }
    }

    // --- scenarios ---

    @Test
    void noSearchParents_findsProjectFile_metadataOnly() throws IOException {
        ProjectFileLookupResponse response = service.lookup(project, "AGENTS.md", false, false);

        assertEquals(1, response.files().size());
        assertEquals("services/rating/AGENTS.md", response.files().getFirst().path());
        assertNull(response.files().getFirst().content(),
                "content should be omitted when includeContent=false");
    }

    @Test
    void noSearchParents_includeContent_returnsProjectFileBody() throws IOException {
        ProjectFileLookupResponse response = service.lookup(project, "AGENTS.md", false, true);

        assertEquals(1, response.files().size());
        assertEquals("# project agents", response.files().getFirst().content());
    }

    @Test
    void noSearchParents_missingFile_returnsEmpty() throws IOException {
        ProjectFileLookupResponse response = service.lookup(project, "MISSING.md", false, false);

        assertNotNull(response.files());
        assertTrue(response.files().isEmpty());
    }

    @Test
    void noSearchParents_doesNotSeeAncestorFiles() throws IOException {
        // services/AGENTS.md and root AGENTS.md exist in the repo, but searchParents=false must ignore them.
        ProjectFileLookupResponse response = service.lookup(project, "AGENTS.md", false, true);

        assertEquals(1, response.files().size());
        assertEquals("services/rating/AGENTS.md", response.files().getFirst().path());
    }

    @Test
    void searchParents_collectsFromProjectRootToRepoRoot() throws IOException {
        ProjectFileLookupResponse response = service.lookup(project, "AGENTS.md", true, true);

        assertEquals(3, response.files().size());
        assertEquals("services/rating/AGENTS.md", response.files().get(0).path());
        assertEquals("# project agents", response.files().get(0).content());
        assertEquals("services/AGENTS.md", response.files().get(1).path());
        assertEquals("# services agents", response.files().get(1).content());
        assertEquals("AGENTS.md", response.files().get(2).path());
        assertEquals("# root agents", response.files().get(2).content());
    }

    @Test
    void searchParents_missingFile_returnsEmpty() throws IOException {
        ProjectFileLookupResponse response = service.lookup(project, "MISSING.md", true, false);

        assertNotNull(response.files());
        assertTrue(response.files().isEmpty());
    }

    @Test
    void searchParents_nestedRelativePath_walksLeafThroughDirectories() throws IOException {
        // Looking up "config/AGENTS.md" with searchParents=true walks up from services/rating/config
        // and looks for the leaf name (AGENTS.md) at every parent — including the project's own
        // subfolders. The fixture has AGENTS.md at every level, so we see them all.
        ProjectFileLookupResponse response = service.lookup(project, "config/AGENTS.md", true, true);

        assertEquals(4, response.files().size());
        assertEquals("services/rating/config/AGENTS.md", response.files().get(0).path());
        assertEquals("# nested project agents", response.files().get(0).content());
        assertEquals("services/rating/AGENTS.md", response.files().get(1).path());
        assertEquals("# project agents", response.files().get(1).content());
        assertEquals("services/AGENTS.md", response.files().get(2).path());
        assertEquals("# services agents", response.files().get(2).content());
        assertEquals("AGENTS.md", response.files().get(3).path());
        assertEquals("# root agents", response.files().get(3).content());
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
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create folder " + parent);
        }
        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }
}
