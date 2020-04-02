package org.openl.rules.webstudio.web.repository.cache;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectVersionCacheManager {

    private final Logger log = LoggerFactory.getLogger(ProjectVersionCacheManager.class);

    private DesignTimeRepository designRepository;

    private ProjectVersionCacheDB projectVersionCacheDB;

    public String getDeployedProjectVersion(AProject project) throws IOException {
        ensureCacheIsNotEmpty();
        String md5 = getProjectMD5(project, ProjectVersionCacheDB.RepoType.DEPLOY);
        return projectVersionCacheDB.getVersion(project.getName(), md5, ProjectVersionCacheDB.RepoType.DESIGN);
    }

    public String getDesignBusinessVersionOfDeployedProject(AProject project) throws IOException {
        ensureCacheIsNotEmpty();
        String projectMD5 = getProjectMD5(project, ProjectVersionCacheDB.RepoType.DEPLOY);
        return projectVersionCacheDB
            .getDesignBusinessVersion(project.getName(), projectMD5, ProjectVersionCacheDB.RepoType.DESIGN);
    }

    private String getProjectMD5(AProject wsProject, ProjectVersionCacheDB.RepoType repoType) throws IOException {
        String hash = projectVersionCacheDB
            .getHash(wsProject.getName(), wsProject.getVersion().getVersionName(), repoType);
        if (hash != null) {
            return hash;
        }
        hash = computeMD5(wsProject);

        projectVersionCacheDB.insertProject(wsProject.getName(), wsProject.getVersion(), hash, repoType);
        return hash;
    }

    private void recalculateDesignRepositoryCache() {
        Collection<? extends AProject> projects = designRepository.getProjects();
        for (AProject project : projects) {
            try {
                cacheProject(project, ProjectVersionCacheDB.RepoType.DESIGN);
            } catch (IOException e) {
                log.error("Error during project caching", e);
            }
        }
    }

    void cacheProject(AProject project, ProjectVersionCacheDB.RepoType repoType) throws IOException {
        List<ProjectVersion> versions = project.getVersions();
        versions.sort((ProjectVersion pr1, ProjectVersion pr2) -> pr2.getVersionInfo()
            .getCreatedAt()
            .compareTo(pr1.getVersionInfo().getCreatedAt()));
        for (ProjectVersion projectVersion : versions) {
            AProject designProject = designRepository.getProject(project.getName(), projectVersion);
            boolean cached = cacheProjectVersion(designProject, repoType);
            if (!cached) {
                break;
            }
        }
    }

    boolean cacheProjectVersion(AProject project, ProjectVersionCacheDB.RepoType repoType) throws IOException {
        String md5 = computeMD5(project);
        String storedVersionName = projectVersionCacheDB.getVersion(project.getName(), md5, repoType);
        if (project.getVersion().getVersionName().equals(storedVersionName)) {
            return false;
        }
        projectVersionCacheDB.insertProject(project.getName(), project.getVersion(), md5, repoType);
        return true;
    }

    private String computeMD5(AProject wsProject) throws IOException {
        StringBuilder md5Builder = new StringBuilder();
        try {
            for (AProjectArtefact artefact : wsProject.getArtefacts()) {
                if (artefact instanceof AProjectResource) {
                    InputStream content = ((AProjectResource) artefact).getContent();
                    md5Builder.append(DigestUtils.md5Hex(content));
                    md5Builder.append(DigestUtils.md5Hex(artefact.getName()));
                }
            }
        } catch (ProjectException e) {
            log.error("Error during computing hash", e);
            return null;
        }
        return md5Builder.length() != 0 ? DigestUtils.md5Hex(md5Builder.toString()) : null;
    }

    private void ensureCacheIsNotEmpty() throws IOException {
        if (projectVersionCacheDB.isCacheEmpty()) {
            recalculateDesignRepositoryCache();
        }
    }

    public void setDesignRepository(DesignTimeRepository designRepository) {
        this.designRepository = designRepository;
        recalculateDesignRepositoryCache();
        designRepository.addListener(() -> {
            recalculateDesignRepositoryCache();
        });
    }

    public void setProjectVersionCacheDB(ProjectVersionCacheDB projectVersionCacheDB) {
        this.projectVersionCacheDB = projectVersionCacheDB;
    }
}
