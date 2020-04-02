package org.openl.rules.webstudio.web.repository.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.repository.git.GitRepository;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;

public class ProjectVersionCacheManagerTest {

    private File root;
    private GitRepository repo;
    private ProjectVersionCacheManager projectVersionCacheManager;

    @Before
    public void setUp() throws IOException, RRepositoryException {
        root = Files.createTempDirectory("openl").toFile();
        repo = createRepository(new File(root, "design-repository"));
        projectVersionCacheManager = new ProjectVersionCacheManager();
        ProjectVersionCacheDB projectVersionCacheDB = new ProjectVersionCacheDB();
        projectVersionCacheDB.setOpenLHome(root.getAbsolutePath());
        projectVersionCacheManager.setProjectVersionCacheDB(projectVersionCacheDB);
    }

    @After
    public void tearDown() throws IOException {
        if (repo != null) {
            repo.close();
        }
        FileUtils.delete(root);
        if (root.exists()) {
            fail("Cannot delete folder " + root);
        }
    }

    @Test
    public void testSaveFile() throws IOException {
        String path = "project/test";
        FileData data = repo.save(createFileData(path, path), IOUtils.toInputStream(path + "1"));
        FileData data2 = repo.save(createFileData(path, path), IOUtils.toInputStream(path + "2"));
        FileData data3 = repo.save(createFileData(path, path), IOUtils.toInputStream(path + "3"));
        AProject project = new AProject(repo, "project", data.getVersion());
        AProject project2 = new AProject(repo, "project", data2.getVersion());
        AProject project3 = new AProject(repo, "project", data3.getVersion());
        projectVersionCacheManager.cacheProjectVersion(project, ProjectVersionCacheDB.RepoType.DESIGN);
        projectVersionCacheManager.cacheProjectVersion(project2, ProjectVersionCacheDB.RepoType.DESIGN);
        projectVersionCacheManager.cacheProjectVersion(project2, ProjectVersionCacheDB.RepoType.DEPLOY);
        projectVersionCacheManager.cacheProjectVersion(project3, ProjectVersionCacheDB.RepoType.DESIGN);
        String deployedProjectVersion = projectVersionCacheManager.getDeployedProjectVersion(project2);
        assertEquals(data2.getVersion(), deployedProjectVersion);
    }

    private GitRepository createRepository(File local) throws RRepositoryException {
        GitRepository repo = new GitRepository();
        repo.setLocalRepositoryPath(local.getAbsolutePath());
        repo.setGitSettingsPath(local.getParent() + "/git-settings");
        repo.setCommentTemplate("WebStudio: {commit-type}. {user-message}");
        repo.initialize();
        return repo;
    }

    private FileData createFileData(String path, String text) {
        FileData fileData = new FileData();
        fileData.setName(path);
        fileData.setSize(text.length());
        fileData.setComment(text + "-comment");
        fileData.setAuthor("DEFAULT");
        return fileData;
    }
}
