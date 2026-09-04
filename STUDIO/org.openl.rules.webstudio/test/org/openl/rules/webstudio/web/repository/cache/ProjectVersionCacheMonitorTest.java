package org.openl.rules.webstudio.web.repository.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

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
        var fileHashCache = new HashMap<String, String>();
        projectVersionCacheMonitor.cacheProjectVersion(project, ProjectVersionH2CacheDB.RepoType.DESIGN, fileHashCache);
        projectVersionCacheMonitor.cacheProjectVersion(project2, ProjectVersionH2CacheDB.RepoType.DESIGN, fileHashCache);
        projectVersionCacheMonitor.cacheProjectVersion(project2, ProjectVersionH2CacheDB.RepoType.DEPLOY, fileHashCache);
        projectVersionCacheMonitor.cacheProjectVersion(project3, ProjectVersionH2CacheDB.RepoType.DESIGN, fileHashCache);
        var designVersion = projectVersionCacheManager.getDesignVersionOfDeployedProject(project2);
        assertEquals(data2.getVersion(), designVersion.version());
        assertEquals("Default", designVersion.createdBy());
        projectVersionCacheDB.closeDb();
    }

    @Test
    void nothingIsLookedUpWhileTheDesignRepositoryIsNotIndexed() throws IOException {
        var path = "project/test";
        var data = repo.save(createFileData(path, path), IOUtils.toInputStream(path + "1"));
        var project = new AProject(repo, "project", data.getVersion());
        projectVersionCacheMonitor.cacheProjectVersion(project,
                ProjectVersionH2CacheDB.RepoType.DESIGN,
                new HashMap<>());

        // Without the indexer nothing can ever match, so hashing the deployed project would be pure loss.
        projectVersionCacheManager.setEnabled(false);

        assertNull(projectVersionCacheManager.getDesignVersionOfDeployedProject(project));
        projectVersionCacheDB.closeDb();
    }

    @Test
    void cachesEachVersionOfAProjectSharingTheHashOfAnUnchangedFile() throws IOException {
        var versions = twoVersionsSharingAnUntouchedFile();
        var projectV2 = versions.getFirst();
        var projectV3 = versions.getLast();

        // Index both design versions through one file-hash cache, the way the monitor indexes a project.
        var fileHashCache = new HashMap<String, String>();
        projectVersionCacheMonitor.cacheProjectVersion(projectV2, ProjectVersionH2CacheDB.RepoType.DESIGN, fileHashCache);
        projectVersionCacheMonitor.cacheProjectVersion(projectV3, ProjectVersionH2CacheDB.RepoType.DESIGN, fileHashCache);

        // Each content still resolves to its own design version, despite the shared file b.
        assertEquals(projectV3.getVersion().getVersionName(),
                projectVersionCacheManager.getDesignVersionOfDeployedProject(projectV3).version());
        assertEquals(projectV2.getVersion().getVersionName(),
                projectVersionCacheManager.getDesignVersionOfDeployedProject(projectV2).version());
        projectVersionCacheDB.closeDb();
    }

    @Test
    void computesTheSameHashWithAWarmCacheAndAColdOne() throws IOException {
        var versions = twoVersionsSharingAnUntouchedFile();
        var projectV2 = versions.getFirst();
        var projectV3 = versions.getLast();

        // A cache warmed by an earlier version must not change the hash of a later one.
        var warmCache = new HashMap<String, String>();
        projectVersionCacheManager.computeMD5(projectV2, warmCache);
        assertEquals(2, warmCache.size(), "both files of the earlier version are memoized");

        var hashWarm = projectVersionCacheManager.computeMD5(projectV3, warmCache);       // b taken from the cache
        var hashCold = projectVersionCacheManager.computeMD5(projectV3, new HashMap<>()); // b read again

        assertEquals(3, warmCache.size(), "only the changed file is read and memoized again");
        assertNotNull(hashCold);
        assertEquals(hashCold, hashWarm);
    }

    /**
     * Two consecutive versions of a two-file project in which only file a changes, so file b is untouched
     * between them. Returned earlier version first.
     */
    private List<AProject> twoVersionsSharingAnUntouchedFile() throws IOException {
        repo.save(createFileData("project/a", "a"), IOUtils.toInputStream("a1"));
        var v2 = repo.save(createFileData("project/b", "b"), IOUtils.toInputStream("b1"));
        var v3 = repo.save(createFileData("project/a", "a"), IOUtils.toInputStream("a2"));
        return List.of(new AProject(repo, "project", v2.getVersion()),  // a1, b1
                new AProject(repo, "project", v3.getVersion()));        // a2, b1
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
        var createdAt = new Date(1_721_000_000_000L);
        var versionData = new FileData();
        versionData.setName("mapped/Pricing");
        versionData.setVersion("revision-1");
        versionData.setModifiedAt(createdAt);
        versionData.setAuthor(new UserInfo("author"));

        when(cacheManager.isCacheCalculated()).thenReturn(false);
        when(baseRepository.getId()).thenReturn("design");
        when(baseRepository.supports()).thenReturn(new FeaturesBuilder(baseRepository).setBranches(true).build());
        when(branchRepository.getId()).thenReturn("design");
        when(branchRepository.getBranch()).thenReturn("feature/rates");
        when(branchRepository.supports())
                .thenReturn(new FeaturesBuilder(branchRepository).setBranches(true).setVersions(true).build());
        when(branchRepository.listHistory("mapped/Pricing")).thenReturn(List.of(versionData));
        when(homeProject.getRepository()).thenReturn(baseRepository);
        when(homeProject.getName()).thenReturn("Pricing");
        when(homeProject.isDeleted()).thenReturn(false);
        when(branchProject.getRepository()).thenReturn(branchRepository);
        when(branchProject.getBusinessName()).thenReturn("Pricing");
        when(branchProject.getFolderPath()).thenReturn("mapped/Pricing");
        when(branchProject.getRealPath()).thenReturn("mapped/Pricing");
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
        doNothing().when(monitor)
                .cacheProjectVersion(eq(historicProject), eq(ProjectVersionH2CacheDB.RepoType.DESIGN), any());

        monitor.run();

        verify(branchProject, never()).getVersions();
        verify(branchRepository).listHistory("mapped/Pricing");
        verify(designRepository).getProjectByPath("design", "feature/rates", "mapped/Pricing", "revision-1");
        verify(monitor).cacheProjectVersion(eq(historicProject), eq(ProjectVersionH2CacheDB.RepoType.DESIGN), any());
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
