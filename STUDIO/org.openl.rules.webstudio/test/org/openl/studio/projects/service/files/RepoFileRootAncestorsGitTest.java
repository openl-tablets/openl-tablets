package org.openl.studio.projects.service.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

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

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.git.GitRepositoryFactory;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.studio.projects.model.files.FsNode;
import org.openl.util.IOUtils;

/**
 * Verifies the repository-mount ancestor search walks from a path up to the repository root,
 * returning matches nearest first.
 *
 * @author Yury Molchan
 */
class RepoFileRootAncestorsGitTest {

    @TempDir
    private File remoteRoot;
    @TempDir
    private File localRepositoriesFolder;

    private Repository repository;
    private RepoFileRoot root;

    @BeforeEach
    void setUp() throws GitAPIException, IOException {
        seedRemoteRepository();
        repository = openGitRepository();

        AclProjectsHelper aclProjectsHelper = mock(AclProjectsHelper.class);
        lenient().when(aclProjectsHelper.hasPermission(any(AProject.class), eq(BasePermission.READ)))
                .thenReturn(true);
        lenient().when(aclProjectsHelper.hasPermission(any(AProjectArtefact.class), eq(BasePermission.READ)))
                .thenReturn(true);

        root = new RepoFileRoot(repository, aclProjectsHelper,
                new ProjectFileLookupServiceImpl(aclProjectsHelper));
    }

    @AfterEach
    void tearDown() {
        if (repository != null) {
            IOUtils.closeQuietly(repository);
        }
    }

    @Test
    void walksFromPathUpToRepositoryRoot() {
        List<FsNode> matches = root.searchAncestors("services/rating/AGENTS.md");

        assertEquals(3, matches.size());
        assertEquals("services/rating/AGENTS.md", matches.get(0).getPath());
        assertEquals("services/AGENTS.md", matches.get(1).getPath());
        assertEquals("AGENTS.md", matches.get(2).getPath());
        assertEquals("AGENTS.md", matches.get(0).getName());
        assertEquals("services/rating", matches.get(0).getBasePath());
    }

    @Test
    void missingLeafYieldsNoMatches() {
        assertTrue(root.searchAncestors("services/rating/MISSING.md").isEmpty());
    }

    private void seedRemoteRepository() throws GitAPIException, IOException {
        try (Git git = Git.init().setDirectory(remoteRoot).call()) {
            File rootDir = git.getRepository().getDirectory().getParentFile();
            writeFile(new File(rootDir, "AGENTS.md"), "# root");
            writeFile(new File(rootDir, "services/AGENTS.md"), "# services");
            writeFile(new File(rootDir, "services/rating/AGENTS.md"), "# rating");
            git.add().addFilepattern(".").call();
            git.commit().setMessage("Seed ancestor fixture").setCommitter("Test", "test@openl.org").call();
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
