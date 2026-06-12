package org.openl.studio.projects.service.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.git.GitRepositoryFactory;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.util.IOUtils;

/**
 * Verifies version-aware reads of {@link ProjectFilesServiceImpl} against an on-disk repository
 * that holds two committed revisions of the same file.
 *
 * <p>A blank version reads the latest revision, an explicit revision reads that historical content,
 * and an unknown revision is reported as not found.
 *
 * @author Yury Molchan
 */
class ProjectFilesServiceImplVersionGitTest {

    private static final String FILE_PATH = "data/sample.txt";

    @TempDir
    private File remoteRoot;
    @TempDir
    private File localRepositoriesFolder;

    private Repository repository;
    private RepoFileRoot root;
    private ProjectFilesServiceImpl service;
    private String firstVersion;
    private String secondVersion;

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
                new ProjectFileLookupServiceImpl(aclProjectsHelper, mock(RepositoryAclServiceProvider.class)));
        service = new ProjectFilesServiceImpl(aclProjectsHelper, mock(FileNodeMapper.class),
                mock(FileSearchSupport.class), mock(FileArchiveSupport.class));
    }

    @AfterEach
    void tearDown() {
        if (repository != null) {
            IOUtils.closeQuietly(repository);
        }
    }

    @Test
    void blankVersionReadsLatestRevision() throws Exception {
        assertEquals("version two", read(service.getResource(root, FILE_PATH, null)));
    }

    @Test
    void firstVersionReadsOriginalRevision() throws Exception {
        assertEquals("version one", read(service.getResource(root, FILE_PATH, firstVersion)));
    }

    @Test
    void secondVersionReadsUpdatedRevision() throws Exception {
        assertEquals("version two", read(service.getResource(root, FILE_PATH, secondVersion)));
    }

    @Test
    void unknownVersionIsReportedAsNotFound() {
        var notFound = assertThrows(NotFoundException.class,
                () -> service.getResource(root, FILE_PATH, "0000000000000000000000000000000000000000"));
        assertEquals("openl.error.404.file.version.not.found.message", notFound.getErrorCode());
    }

    private static String read(AProjectResource resource) throws Exception {
        try (var in = resource.getContent()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void seedRemoteRepository() throws GitAPIException, IOException {
        try (Git git = Git.init().setDirectory(remoteRoot).call()) {
            File rootDir = git.getRepository().getDirectory().getParentFile();

            writeFile(new File(rootDir, FILE_PATH), "version one");
            git.add().addFilepattern(".").call();
            RevCommit first = git.commit()
                    .setMessage("Add sample")
                    .setCommitter("Test", "test@openl.org")
                    .call();
            firstVersion = first.getName();

            writeFile(new File(rootDir, FILE_PATH), "version two");
            git.add().addFilepattern(".").call();
            RevCommit second = git.commit()
                    .setMessage("Update sample")
                    .setCommitter("Test", "test@openl.org")
                    .call();
            secondVersion = second.getName();
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
