package org.openl.rules.webstudio.web.repository.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.VersionInfo;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.BranchStatus;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.repository.git.GitRepositoryFactory;
import org.openl.rules.workspace.dtr.BranchedProject;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.util.IOUtils;

class ProjectVersionCacheMonitorTest {

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
    void setUp() throws IOException {
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
    void testCacheProjects() throws IOException {
        var path = "project/test";
        var data = repo.save(createFileData(path, path), IOUtils.toInputStream(path + "1"));
        var data2 = repo.save(createFileData(path, path), IOUtils.toInputStream(path + "2"));
        var data3 = repo.save(createFileData(path, path), IOUtils.toInputStream(path + "3"));
        var project = new AProject(repo, "project", data.getVersion());
        var project2 = new AProject(repo, "project", data2.getVersion());
        var project3 = new AProject(repo, "project", data3.getVersion());
        projectVersionCacheMonitor.cacheProjectVersion(project, ProjectVersionH2CacheDB.RepoType.DESIGN);
        projectVersionCacheMonitor.cacheProjectVersion(project2, ProjectVersionH2CacheDB.RepoType.DESIGN);
        projectVersionCacheMonitor.cacheProjectVersion(project2, ProjectVersionH2CacheDB.RepoType.DEPLOY);
        projectVersionCacheMonitor.cacheProjectVersion(project3, ProjectVersionH2CacheDB.RepoType.DESIGN);
        var deployedProjectVersion = projectVersionCacheManager.getDeployedProjectVersion(project2);
        assertEquals(data2.getVersion(), deployedProjectVersion);
        projectVersionCacheDB.closeDb();
    }

    @Test
    void cachesVersionUsingItsIndexedBranchAndPath() throws Exception {
        var designRepository = mock(DesignTimeRepository.class);
        var cacheManager = mock(ProjectVersionCacheManager.class);
        var cacheDB = mock(ProjectVersionH2CacheDB.class);
        var baseRepository = mock(BranchRepository.class);
        var branchRepository = mock(BranchRepository.class);
        var homeProject = mock(AProject.class);
        var branchProject = mock(AProject.class);
        var historicProject = mock(AProject.class);
        var version = mock(ProjectVersion.class);
        var versionInfo = mock(VersionInfo.class);
        var createdAt = new Date(1_721_000_000_000L);

        when(cacheManager.isCacheCalculated()).thenReturn(false);
        when(baseRepository.getId()).thenReturn("design");
        when(baseRepository.supports()).thenReturn(new FeaturesBuilder(baseRepository).setBranches(true).build());
        when(branchRepository.getId()).thenReturn("design");
        when(branchRepository.getBranch()).thenReturn("feature/rates");
        when(branchRepository.supports()).thenReturn(new FeaturesBuilder(branchRepository).setBranches(true).build());
        when(homeProject.getRepository()).thenReturn(baseRepository);
        when(homeProject.getName()).thenReturn("Pricing");
        when(homeProject.isDeleted()).thenReturn(false);
        when(branchProject.getRepository()).thenReturn(branchRepository);
        when(branchProject.getBusinessName()).thenReturn("Pricing");
        when(branchProject.getRealPath()).thenReturn("mapped/Pricing");
        when(branchProject.getVersions()).thenReturn(List.of(version));
        when(version.getVersionName()).thenReturn("revision-1");
        when(version.getVersionInfo()).thenReturn(versionInfo);
        when(versionInfo.getCreatedAt()).thenReturn(createdAt);
        doReturn(List.of(homeProject)).when(designRepository).getProjects();
        when(designRepository.getRepository("design")).thenReturn(baseRepository);
        when(designRepository.getBranchedProject("design", "Pricing"))
                .thenReturn(Optional.of(BranchedProject.create(
                        "Pricing",
                        "main",
                        Map.of("feature/rates",
                                new BranchedProject.BranchEntry(branchProject, mock(BranchStatus.class))))));
        when(designRepository.getProjectByPath("design", "feature/rates", "mapped/Pricing", "revision-1"))
                .thenReturn(historicProject);

        var monitor = spy(new ProjectVersionCacheMonitor(new SimpleGrantedAuthority("Administrators")));
        monitor.setProjectVersionCacheDB(cacheDB);
        monitor.setProjectVersionCacheManager(cacheManager);
        monitor.setDesignRepository(designRepository);
        monitor.setEnabled(true);
        doNothing().when(monitor).cacheProjectVersion(historicProject, ProjectVersionH2CacheDB.RepoType.DESIGN);

        monitor.run();

        verify(designRepository).getProjectByPath("design", "feature/rates", "mapped/Pricing", "revision-1");
        verify(monitor).cacheProjectVersion(historicProject, ProjectVersionH2CacheDB.RepoType.DESIGN);
        verify(cacheDB).setCacheCalculatedState(true);
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
            }
            return null;
        });
    }

    private FileData createFileData(String path, String text) {
        var fileData = new FileData();
        fileData.setName(path);
        fileData.setSize(text.length());
        fileData.setComment(text + "-comment");
        fileData.setAuthor(new UserInfo("DEFAULT", "DEFAULT@email", "Default"));
        return fileData;
    }
}
