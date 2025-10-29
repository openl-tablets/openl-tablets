package org.openl.rules.repository.git;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.LfsFactory;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.repository.api.UserInfo;
import org.openl.util.ZipUtils;

public class MergeLfsRepoTest {
    
    private static final String REPO_ID = "design";

    @TempDir
    File gitRepo;
    @TempDir
    File localRepositoriesFolder;
    @AutoClose
    private GitRepository repo;

    @BeforeEach
    public void setUp() throws Exception {
        ZipUtils.extractAll(new File("target/test-classes/repositories/MergeLfsRepoTest.zip"), gitRepo);

        repo = createRepository(gitRepo);

        Repository repository = repo.getClosableGit().getRepository();
        boolean installed = repository.getConfig().getBoolean(ConfigConstants.CONFIG_FILTER_SECTION,
                ConfigConstants.CONFIG_SECTION_LFS,
                ConfigConstants.CONFIG_KEY_USEJGITBUILTIN,
                false);
        if (!installed) {
            LfsFactory.getInstance().getInstallCommand().setRepository(repository).call();
        }

    }

    @Test
    public void testMergeWithoutError() throws IOException {
        UserInfo author = new UserInfo("test", "my@email", "Test User");

        repo.forBranch("br7").merge("main", author, null);

        assertEquals("50fa760a58f5cb4cbf025537f119ff37001d02cd",
                repo.forBranch("br7").check("DESIGN/rules/Example 3 - Auto Policy Calculation").getVersion());
    }

    private GitRepository createRepository(File gitRepo) throws IOException {
        GitRepository newRepo = new GitRepository();
        newRepo.setId(REPO_ID);
        var repoPath = gitRepo.getAbsolutePath();
        newRepo.setUri(repoPath);
        String localRepositoriesFolderString = localRepositoriesFolder.getAbsolutePath();
        newRepo.setLocalRepositoriesFolder(localRepositoriesFolderString);
        newRepo.setCommentTemplate("OpenL Studio: {commit-type}. {user-message}");
        newRepo.setGcAutoDetach(false);
        newRepo.setBranch("main");
        newRepo.initialize(TestGitUtils.mockGitRootFactory(REPO_ID, repoPath, gitRepo, localRepositoriesFolderString, false, false));
        return newRepo;
    }
}
