package org.openl.rules.webstudio.web.repository.cache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

public class ProjectVersionCacheManager implements InitializingBean {

    private final Logger log = LoggerFactory.getLogger(ProjectVersionCacheManager.class);

    private DesignTimeRepository designRepository;

    private ProjectVersionH2CacheDB projectVersionCacheDB;

    public String getDeployedProjectVersion(AProject project) throws IOException {
        ensureCacheIsNotEmpty();
        String md5 = getProjectMD5(project, ProjectVersionH2CacheDB.RepoType.DEPLOY);
        return md5 != null ? projectVersionCacheDB
            .getVersion(project.getName(), md5, ProjectVersionH2CacheDB.RepoType.DESIGN) : null;
    }

    public String getDesignBusinessVersionOfDeployedProject(AProject project) throws IOException {
        ensureCacheIsNotEmpty();
        String md5 = getProjectMD5(project, ProjectVersionH2CacheDB.RepoType.DEPLOY);
        return md5 != null ? projectVersionCacheDB
            .getDesignBusinessVersion(project.getName(), md5, ProjectVersionH2CacheDB.RepoType.DESIGN) : null;
    }

    public boolean isCacheCalculated() {
        try {
            return projectVersionCacheDB.isCacheCalculated();
        } catch (IOException e) {
            log.error("Error during project caching", e);
            return false;
        }
    }

    public String computeMD5(AProject wsProject) {
        List<String> md5Strings = new ArrayList<>();
        try {
            if (wsProject.getRepository().supports().folders()) {
                for (AProjectArtefact artefact : wsProject.getArtefacts()) {
                    if (artefact instanceof AProjectResource) {
                        try (InputStream content = ((AProjectResource) artefact).getContent()) {
                            md5Strings.add(DigestUtils.md5Hex(content));
                            String fileName = artefact.getFileData().getName();
                            String folderPath = wsProject.getFolderPath();
                            if (!StringUtils.isEmpty(folderPath)) {
                                fileName = fileName.substring(wsProject.getFolderPath().length() + 1);
                            }
                            md5Strings.add(DigestUtils.md5Hex(fileName));
                        }
                    }
                }
            } else {
                FileItem zip = wsProject.getRepository().read(wsProject.getFolderPath());
                try (ZipInputStream zin = new ZipInputStream(zip.getStream())) {
                    ZipEntry entry;
                    while ((entry = zin.getNextEntry()) != null) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        int b = zin.read();
                        while (b >= 0) {
                            baos.write(b);
                            b = zin.read();
                        }
                        md5Strings.add(DigestUtils.md5Hex(baos.toByteArray()));
                        md5Strings.add(DigestUtils.md5Hex(entry.getName()));
                    }
                }
            }
        } catch (ProjectException | IOException e) {
            log.error("Error during computing hash", e);
            return null;
        }
        return md5Strings.isEmpty() ? null
                                    : DigestUtils.md5Hex(md5Strings.stream().sorted().collect(Collectors.joining()));
    }

    private String getProjectMD5(AProject wsProject, ProjectVersionH2CacheDB.RepoType repoType) throws IOException {
        String hash = projectVersionCacheDB.getHash(wsProject.getName(),
            wsProject.getVersion().getVersionName(),
            wsProject.getVersion().getVersionInfo().getCreatedAt(),
            repoType);
        if (StringUtils.isEmpty(hash)) {
            hash = computeMD5(wsProject);
            projectVersionCacheDB.insertProject(wsProject.getName(), wsProject.getVersion(), hash, repoType);
        }
        return hash;
    }

    private void ensureCacheIsNotEmpty() throws IOException {
        if (projectVersionCacheDB.isCacheEmpty()) {
            projectVersionCacheDB.setCacheCalculatedState(false);
        }
    }

    @Override
    public void afterPropertiesSet() {
        designRepository.addListener(() -> {
            try {
                projectVersionCacheDB.setCacheCalculatedState(false);
            } catch (IOException e) {
                log.error("Error during project caching", e);
            }
        });
    }

    public void setDesignRepository(DesignTimeRepository designRepository) {
        this.designRepository = designRepository;
    }

    public void setProjectVersionCacheDB(ProjectVersionH2CacheDB projectVersionCacheDB) {
        this.projectVersionCacheDB = projectVersionCacheDB;
    }

}
