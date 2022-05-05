package org.openl.rules.repository.git;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.LfsFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openl.rules.repository.api.UserInfo;
import org.openl.util.FileUtils;
import org.openl.util.ZipUtils;

public class MergeLfsRepoTest {

    private static final String REPO_URI = "target/test-classes/repositories/MergeLfsRepoTest";

    private GitRepository repo;

    @Before
    public void setUp() throws Exception {
        Path repoPath = Path.of(REPO_URI);
        Files.createDirectories(repoPath);
        File gitRepo = Files.createTempDirectory(repoPath, "lfs-merge").toFile();

        ZipUtils.extractAll(new File(REPO_URI + ".zip"), gitRepo);

        repo = createRepository(gitRepo.getAbsolutePath());

        Repository repository = repo.getClosableGit().getRepository();
        boolean installed = repository.getConfig().getBoolean(ConfigConstants.CONFIG_FILTER_SECTION,
                ConfigConstants.CONFIG_SECTION_LFS,
                ConfigConstants.CONFIG_KEY_USEJGITBUILTIN,
                false);
        if (!installed) {
            LfsFactory.getInstance().getInstallCommand().setRepository(repository).call();
        }

    }

    @After
    public void tearDown() {
        if (repo != null) {
            repo.close();
        }
        FileUtils.deleteQuietly(Path.of(REPO_URI));
    }

    @Test
    public void testMergeWithoutError() throws IOException {
        UserInfo author = new UserInfo("test", "my@email", "Test User");

        repo.forBranch("br7").merge("main", author, null);

        assertEquals("50fa760a58f5cb4cbf025537f119ff37001d02cd",
            repo.forBranch("br7").check("DESIGN/rules/Example 3 - Auto Policy Calculation").getVersion());
    }

    private GitRepository createRepository(String repoPath) {
        GitRepository repo = new GitRepository();
        repo.setLocalRepositoryPath(repoPath);
        repo.setCommentTemplate("WebStudio: {commit-type}. {user-message}");
        repo.setGcAutoDetach(false);
        repo.setBranch("main");
        repo.initialize();
        return repo;
    }
}
