package org.openl.rules.webstudio.web.repository.cache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.InitializingBean;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.util.StringUtils;

@Slf4j
public class ProjectVersionCacheManager implements InitializingBean {


    @Setter
    private DesignTimeRepository designRepository;

    @Setter
    private ProjectVersionH2CacheDB projectVersionCacheDB;

    /**
     * Whether the design repository is indexed at all. Nothing is looked up while it is not: without an index
     * every lookup would hash the deployed project only to find nothing.
     */
    @Setter
    private boolean enabled = true;

    /**
     * Finds the design repository revision a deployed project was built from.
     *
     * <p>A deployed project carries no reference back to its origin, so it is recognized by its content: the
     * revision returned is the latest design revision of the project whose files are the same. Deployments of
     * two design revisions with the same content are therefore indistinguishable.
     *
     * <p>The design repository is indexed in the background by {@link ProjectVersionCacheMonitor}. Until the
     * revision reaches the index it cannot be found, and a project deployed from a design repository this
     * OpenL Studio does not have is never found.
     *
     * @param project the deployed project
     * @return the design revision, or {@code null} when none matches
     */
    public CachedProjectVersion getDesignVersionOfDeployedProject(AProject project) throws IOException {
        if (!enabled) {
            return null;
        }
        // Before anything is written: hashing the deployed project below would itself fill an emptied cache
        // and hide that the design index has to be rebuilt.
        ensureCacheIsNotEmpty();
        var md5 = getProjectMD5(project, ProjectVersionH2CacheDB.RepoType.DEPLOY);
        return md5 != null ? projectVersionCacheDB
                .getVersion(project.getBusinessName(), md5, ProjectVersionH2CacheDB.RepoType.DESIGN) : null;
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
        var md5Strings = new ArrayList<String>();
        try {
            if (wsProject.getRepository().supports().folders()) {
                final var manName = wsProject.getProject().getFolderPath() + "/" + JarFile.MANIFEST_NAME;
                for (AProjectArtefact artefact : wsProject.getArtefacts()) {
                    if (manName.equals(artefact.getFileData().getName())) {
                        //skip manifest from hash calculation
                        continue;
                    }
                    if (artefact instanceof AProjectResource resource) {
                        try (var content = resource.getContent()) {
                            md5Strings.add(DigestUtils.md5Hex(content));
                            var fileName = artefact.getFileData().getName();
                            var folderPath = wsProject.getFolderPath();
                            if (!StringUtils.isEmpty(folderPath)) {
                                fileName = fileName.substring(wsProject.getFolderPath().length() + 1);
                            }
                            md5Strings.add(DigestUtils.md5Hex(fileName));
                        }
                    }
                }
            } else {
                var zip = wsProject.getRepository().read(wsProject.getFolderPath());
                try (var zin = new ZipInputStream(zip.getStream())) {
                    ZipEntry entry;
                    while ((entry = zin.getNextEntry()) != null) {
                        if (JarFile.MANIFEST_NAME.equals(entry.getName())) {
                            //skip manifest from hash calculation
                            continue;
                        }
                        var baos = new ByteArrayOutputStream();
                        var b = zin.read();
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
        var hash = projectVersionCacheDB.getHash(wsProject.getBusinessName(),
                wsProject.getVersion().getVersionName(),
                wsProject.getVersion().getVersionInfo().getCreatedAt(),
                repoType);
        if (StringUtils.isEmpty(hash)) {
            hash = computeMD5(wsProject);
            projectVersionCacheDB.insertProject(wsProject.getBusinessName(), wsProject.getVersion(), hash, repoType);
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

}
