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
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.webstudio.web.repository.DeploymentManager;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectVersionCacheManager {

    private final Logger log = LoggerFactory.getLogger(ProjectVersionCacheManager.class);

    private DesignTimeRepository designRepository;

    private ProjectVersionCacheDB projectVersionCacheDB;

    public String getDeployedProjectVersion(AProject project) throws IOException {

        if (projectVersionCacheDB.isCacheEmpty()) {
            recalculateAllCache(ProjectVersionCacheDB.RepoType.DESIGN);
        }

        String md5 = getProjectMD5(project, ProjectVersionCacheDB.RepoType.DEPLOY);

        return projectVersionCacheDB.getVersion(project.getName(), md5, ProjectVersionCacheDB.RepoType.DESIGN);
    }

    public void cacheProject(AProject project, ProjectVersionCacheDB.RepoType repoType) throws IOException {
        List<ProjectVersion> versions = project.getVersions();
        versions.sort((ProjectVersion pr1, ProjectVersion pr2) -> pr2.getVersionInfo()
            .getCreatedAt()
            .compareTo(pr1.getVersionInfo().getCreatedAt()));
        for (ProjectVersion version : versions) {
            AProject designProject = designRepository.getProject(project.getName(), version);
            String md5 = computeMD5(designProject);
            String versionName = designProject.getVersion().getVersionName();
            String storedVersionName = projectVersionCacheDB.getVersion(project.getName(), md5, repoType);
            if (version.getVersionName().equals(storedVersionName)) {
                break;
            }
            projectVersionCacheDB.insertProject(project.getName(), versionName, md5, repoType);
        }
    }

    public void recalculateAllCache(ProjectVersionCacheDB.RepoType repoType) throws IOException {
        Collection<? extends AProject> projects = designRepository.getProjects();
        for (AProject project : projects) {
            cacheProject(project, repoType);
        }
    }

    public String getProjectMD5(AProject wsProject, ProjectVersionCacheDB.RepoType repoType) throws IOException {
        String hash = projectVersionCacheDB
            .getHash(wsProject.getName(), wsProject.getVersion().getVersionName(), repoType);
        if (hash != null) {
            return hash;
        }
        hash = computeMD5(wsProject);
        projectVersionCacheDB
            .insertProject(wsProject.getName(), wsProject.getVersion().getVersionName(), hash, repoType);
        return hash;
    }

    public void setDesignRepository(DesignTimeRepository designRepository) {
        this.designRepository = designRepository;
        Repository repository = designRepository.getRepository();
        repository.setListener(() -> {
            try {
                recalculateAllCache(ProjectVersionCacheDB.RepoType.DESIGN);
            } catch (IOException e) {
                log.error("Error during project caching", e);
            }
        });
    }

    public void setProjectVersionCacheDB(ProjectVersionCacheDB projectVersionCacheDB) {
        this.projectVersionCacheDB = projectVersionCacheDB;
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
        return DigestUtils.md5Hex(md5Builder.toString());
    }
}
