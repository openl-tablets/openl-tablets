package org.openl.rules.webstudio.web.repository.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.repository.git.GitRepositoryFactory;
import org.openl.util.IOUtils;

public class ProjectVersionCacheMonitorTest {

    @TempDir
    private File root;
    @TempDir
    private File localRepositoriesFolder;
    
    @AutoClose
    private Repository repo;
    private ProjectVersionCacheMonitor projectVersionCacheMonitor;
    private ProjectVersionCacheManager projectVersionCacheManager;
    private ProjectVersionH2CacheDB projectVersionCacheDB;

    @BeforeEach
    public void setUp() throws IOException {
        repo = createRepository(new File(root, "design-repository"));
        projectVersionCacheMonitor = new ProjectVersionCacheMonitor(new SimpleGrantedAuthority("Administrators"));
        projectVersionCacheManager = new ProjectVersionCacheManager();
        projectVersionCacheDB = new ProjectVersionH2CacheDB();
        projectVersionCacheDB.setOpenLHome(root.getAbsolutePath());
        projectVersionCacheMonitor.setProjectVersionCacheDB(projectVersionCacheDB);
        projectVersionCacheManager.setProjectVersionCacheDB(projectVersionCacheDB);
        projectVersionCacheMonitor.setProjectVersionCacheManager(projectVersionCacheManager);
    }

    @Test
    public void testCacheProjects() throws IOException {
        String path = "project/test";
        FileData data = repo.save(createFileData(path, path), IOUtils.toInputStream(path + "1"));
        FileData data2 = repo.save(createFileData(path, path), IOUtils.toInputStream(path + "2"));
        FileData data3 = repo.save(createFileData(path, path), IOUtils.toInputStream(path + "3"));
        AProject project = new AProject(repo, "project", data.getVersion());
        AProject project2 = new AProject(repo, "project", data2.getVersion());
        AProject project3 = new AProject(repo, "project", data3.getVersion());
        projectVersionCacheMonitor.cacheProjectVersion(project, ProjectVersionH2CacheDB.RepoType.DESIGN);
        projectVersionCacheMonitor.cacheProjectVersion(project2, ProjectVersionH2CacheDB.RepoType.DESIGN);
        projectVersionCacheMonitor.cacheProjectVersion(project2, ProjectVersionH2CacheDB.RepoType.DEPLOY);
        projectVersionCacheMonitor.cacheProjectVersion(project3, ProjectVersionH2CacheDB.RepoType.DESIGN);
        String deployedProjectVersion = projectVersionCacheManager.getDeployedProjectVersion(project2);
        assertEquals(data2.getVersion(), deployedProjectVersion);
        projectVersionCacheDB.closeDb();
    }

    private Repository createRepository(File local) {
        return new GitRepositoryFactory().create(s -> {
            switch (s) {
                case "id":
                    return "design";
                case "uri":
                    return local.toURI().toString();
                case "local-repositories-folder":
                    return localRepositoriesFolder.getAbsolutePath();
                case "comment-template":
                    return "OpenL Studio: {commit-type}. {user-message}";
            }
            return null;
        });
    }

    private FileData createFileData(String path, String text) {
        FileData fileData = new FileData();
        fileData.setName(path);
        fileData.setSize(text.length());
        fileData.setComment(text + "-comment");
        fileData.setAuthor(new UserInfo("DEFAULT", "DEFAULT@email", "Default"));
        return fileData;
    }
}
