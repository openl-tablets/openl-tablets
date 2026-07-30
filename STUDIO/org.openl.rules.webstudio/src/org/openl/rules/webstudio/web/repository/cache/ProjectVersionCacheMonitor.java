package org.openl.rules.webstudio.web.repository.cache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import org.openl.rules.common.ProjectVersion;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.security.SimpleGroup;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.util.StringUtils;

@Slf4j
public class ProjectVersionCacheMonitor implements Runnable, InitializingBean {


    private ScheduledExecutorService scheduledPool;
    private ProjectVersionH2CacheDB projectVersionCacheDB;
    private ProjectVersionCacheManager projectVersionCacheManager;
    private DesignTimeRepository designRepository;
    @Setter
    private boolean enabled;

    private final Authentication relevantSystemWideGrantedAuthority;

    private final static int PERIOD = 10;

    public ProjectVersionCacheMonitor(GrantedAuthority relevantSystemWideGrantedAuthority) {
        var group = new SimpleGroup();
        group.setName(relevantSystemWideGrantedAuthority.getAuthority());
        var principal = SimpleUser.builder().setUsername("admin").setPrivileges(List.of(group)).build();
        this.relevantSystemWideGrantedAuthority = new UsernamePasswordAuthenticationToken(principal,
                "",
                principal.getAuthorities());
    }

    @Override
    public void run() {
        if (!enabled) {
            return;
        }
        var oldAuthentication = SecurityContextHolder.getContext().getAuthentication();
        try {
            SecurityContextHolder.getContext().setAuthentication(relevantSystemWideGrantedAuthority);
            try {
                if (!projectVersionCacheManager.isCacheCalculated()) {
                    recalculateDesignRepositoryCache();
                }
            } catch (Exception e) {
                log.error("Error during project caching", e);
            }
        } finally {
            SecurityContextHolder.getContext().setAuthentication(oldAuthentication);
        }
    }

    private void recalculateDesignRepositoryCache() throws IOException, InterruptedException {
        Collection<? extends AProject> projects = designRepository.getProjects();
        for (AProject project : projects) {
            if (project.isDeleted()) {
                continue;
            }
            cacheDesignProject(project);
            Thread.yield();
        }
        projectVersionCacheDB.setCacheCalculatedState(true);
    }

    private void cacheDesignProject(AProject project) throws IOException, InterruptedException {
        var repositoryId = project.getRepository().getId();
        var repository = designRepository.getRepository(repositoryId);
        var versions = new ArrayList<ProjectVersionSource>();
        if (repository.supports().branches()) {
            designRepository.getBranchedProject(repositoryId, project.getName())
                    .stream()
                    .flatMap(branchedProject -> branchedProject.entries().values().stream())
                    .map(entry -> entry.project())
                    .forEach(branchProject -> readVersions(branchProject)
                            .forEach(version -> versions.add(new ProjectVersionSource(branchProject, version))));
        } else {
            readVersions(project).forEach(version -> versions.add(new ProjectVersionSource(project, version)));
        }
        versions.sort(Comparator.comparing(source -> source.version().getVersionInfo().getCreatedAt(),
                Comparator.reverseOrder()));
        for (ProjectVersionSource source : versions) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Project monitor cache task is interrupted.");
            }
            var branchProject = source.project();
            var projectVersion = source.version();
            if (projectVersion.isDeleted()) {
                continue;
            }

            var hash = projectVersionCacheDB.getHash(branchProject.getBusinessName(),
                    projectVersion.getVersionName(),
                    projectVersion.getVersionInfo().getCreatedAt(),
                    ProjectVersionH2CacheDB.RepoType.DESIGN);
            if (StringUtils.isEmpty(hash)) {
                var repo = branchProject.getRepository();
                String branch = repo.supports().branches() ? ((BranchRepository) repo).getBranch() : null;
                var designProject = designRepository.getProjectByPath(repo.getId(),
                        branch,
                        branchProject.getRealPath(),
                        projectVersion.getVersionName());
                if (designProject.isDeleted()) {
                    continue;
                }
                cacheProjectVersion(designProject, ProjectVersionH2CacheDB.RepoType.DESIGN);
            }
        }
    }

    private List<ProjectVersion> readVersions(AProject project) {
        // Snapshot projects are long-lived. Read through a disposable view so a complete Git history is not retained
        // by every project/branch membership after the cache has been calculated.
        return new AProject(project.getRepository(), project.getFolderPath()).getVersions();
    }

    private record ProjectVersionSource(AProject project, ProjectVersion version) {
    }

    void cacheProjectVersion(AProject project, ProjectVersionH2CacheDB.RepoType repoType) throws IOException {
        var md5 = projectVersionCacheManager.computeMD5(project);
        projectVersionCacheDB.insertProject(project.getBusinessName(), project.getVersion(), md5, repoType);
    }

    public void setProjectVersionCacheDB(ProjectVersionH2CacheDB projectVersionCacheDB) {
        release();
        this.projectVersionCacheDB = projectVersionCacheDB;
    }

    public void setProjectVersionCacheManager(ProjectVersionCacheManager projectVersionCacheManager) {
        release();
        this.projectVersionCacheManager = projectVersionCacheManager;
    }

    public void setDesignRepository(DesignTimeRepository designRepository) {
        release();
        this.designRepository = designRepository;
    }

    @Override
    public void afterPropertiesSet() {
        if (projectVersionCacheDB != null && projectVersionCacheManager != null && designRepository != null) {
            scheduledPool = Executors.newSingleThreadScheduledExecutor(r -> {
                var t = Executors.defaultThreadFactory().newThread(r);
                t.setDaemon(true);
                return t;
            });
            scheduledPool.scheduleWithFixedDelay(this, 1, PERIOD, TimeUnit.SECONDS);
        }
    }

    /**
     * @see <a href=
     * "https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html">ExecutorService</a>
     */
    public synchronized void release() {
        if (scheduledPool == null) {
            return;
        }
        scheduledPool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!scheduledPool.awaitTermination(PERIOD * 3, TimeUnit.SECONDS)) {
                scheduledPool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!scheduledPool.awaitTermination(PERIOD * 3, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Unable to terminate project version cache monitor task.");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            scheduledPool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
        scheduledPool = null;
    }
}
