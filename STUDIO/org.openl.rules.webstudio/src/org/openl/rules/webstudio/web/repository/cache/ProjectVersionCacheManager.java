package org.openl.rules.webstudio.web.repository.cache;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.webstudio.web.repository.DeploymentManager;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectVersionCacheManager {

    private final Logger log = LoggerFactory.getLogger(ProjectVersionCacheManager.class);

    private DesignTimeRepository designRepository;

    private DeploymentManager deploymentManager;

    private H2CacheDB h2CacheDB;

    public String checkDeployedProject(AProject project) throws IOException {

        if (h2CacheDB.isCacheEmpty()) {
            recalculateAllCache(H2CacheDB.RepoType.DESIGN);
        }

        String md5 = getProjectMD5(project, H2CacheDB.RepoType.DEPLOY);

        return h2CacheDB.getVersion(project.getName(), md5, H2CacheDB.RepoType.DESIGN);
    }

    public void cacheProject(AProject project, H2CacheDB.RepoType repoType) throws IOException {
        List<ProjectVersion> versions = project.getVersions();
        // TODO
        List<ProjectVersion> sortedVersions = versions.stream().sorted(new Comparator<ProjectVersion>() {
            @Override
            public int compare(ProjectVersion o1, ProjectVersion o2) {
                return o2.getVersionInfo().getCreatedAt().compareTo(o1.getVersionInfo().getCreatedAt());
            }
        }).collect(Collectors.toList());
        for (ProjectVersion version : sortedVersions) {
            AProject designProject = designRepository.getProject(project.getName(), version);
            String md5 = getProjectMD5(designProject, repoType);
            String versionName = designProject.getVersion().getVersionName();

            String storedVersionName = h2CacheDB.getVersion(project.getName(), md5, H2CacheDB.RepoType.DESIGN);
            if (version.getVersionName().equals(storedVersionName)) {
                break;
            }
            h2CacheDB.insert(project.getName(), versionName, md5, H2CacheDB.RepoType.DESIGN);
        }
    }

    public void recalculateAllCache(H2CacheDB.RepoType repoType) throws IOException {
        Collection<? extends AProject> projects = designRepository.getProjects();
        for (AProject project : projects) {
            cacheProject(project, repoType);
        }
    }

    public String getProjectMD5(AProject wsProject, H2CacheDB.RepoType repoType) throws IOException {
        String hash = h2CacheDB.getHash(wsProject.getName(), wsProject.getVersion().getVersionName(), repoType);
        if (hash != null) {
            return hash;
        }
        StringBuilder md5Builder = new StringBuilder();
        try {
            for (AProjectArtefact artefact : wsProject.getArtefacts()) {
                if (artefact instanceof AProjectResource) {
                    InputStream content = ((AProjectResource) artefact).getContent();
                    md5Builder.append(DigestUtils.md5Hex(content));
                    md5Builder.append(DigestUtils.md5Hex(artefact.getName()));
                } else {
                    System.out.println("TODO REMOVE");
                }
            }
        } catch (NullPointerException | ProjectException e) {
            System.out.println("!");
        }
        hash = DigestUtils.md5Hex(md5Builder.toString());
        h2CacheDB.insert(wsProject.getName(), wsProject.getVersion().getVersionName(), hash, repoType);
        return hash;
    }

    public void setDesignRepository(DesignTimeRepository designRepository) {
        this.designRepository = designRepository;
        Listener listener = new Listener() {
            @Override
            public void onChange() {
                try {
                    recalculateAllCache(H2CacheDB.RepoType.DESIGN);
                } catch (IOException e) {
                    log.error("Error during project caching", e);
                }
            }
        };
        this.designRepository.getRepository().setListener(listener);
    }

    public void setDeploymentManager(DeploymentManager deploymentManager) {
        this.deploymentManager = deploymentManager;
    }

    public void setH2CacheDB(H2CacheDB h2CacheDB) {
        this.h2CacheDB = h2CacheDB;
    }
}
