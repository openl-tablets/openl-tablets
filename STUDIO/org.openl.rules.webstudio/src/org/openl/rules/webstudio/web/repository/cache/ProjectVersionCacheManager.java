package org.openl.rules.webstudio.web.repository.cache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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

    /**
     * Computes the content hash of a project version.
     *
     * <p>The hash is built from the content and the name of every file of the project, so two versions with
     * the same content get the same hash whatever repository holds them. The deployment manifest is left out,
     * because it is written at deploy time and describes the deployment rather than the project.
     *
     * <p>Where the repository gives every file an id of its own, the hash of a single file is looked up in
     * {@code fileHashCache} by that id, so a file left untouched by a revision is read and hashed once for
     * all the versions sharing the cache. A repository whose ids change on every write, and one that has no
     * ids at all, simply never hits the cache and reads every file of every version.
     *
     * @param wsProject the project version to hash
     * @param fileHashCache file id to file content hash, shared by the versions of one project
     * @return the project content hash, or {@code null} when the project has no file to hash
     */
    String computeMD5(AProject wsProject, Map<String, String> fileHashCache) {
        var md5Strings = new ArrayList<String>();
        try {
            if (wsProject.getRepository().supports().folders()) {
                var contentAddressable = wsProject.getRepository().supports().uniqueFileId();
                final var manName = wsProject.getProject().getFolderPath() + "/" + JarFile.MANIFEST_NAME;
                for (AProjectArtefact artefact : wsProject.getArtefacts()) {
                    if (manName.equals(artefact.getFileData().getName())) {
                        //skip manifest from hash calculation
                        continue;
                    }
                    if (artefact instanceof AProjectResource resource) {
                        md5Strings.add(computeFileMD5(resource, contentAddressable, fileHashCache));
                        var fileName = artefact.getFileData().getName();
                        var folderPath = wsProject.getFolderPath();
                        if (!StringUtils.isEmpty(folderPath)) {
                            fileName = fileName.substring(folderPath.length() + 1);
                        }
                        md5Strings.add(DigestUtils.md5Hex(fileName));
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

    /**
     * Returns the content hash of a single file, reusing the hash already computed for the same file id.
     *
     * <p>The id keys the already computed hashes: a file a revision left untouched keeps its id, so its
     * content is read and hashed only once. A file without an id is always read.
     */
    private String computeFileMD5(AProjectResource resource,
                                  boolean contentAddressable,
                                  Map<String, String> fileHashCache) throws ProjectException, IOException {
        var fileId = contentAddressable ? resource.getFileData().getUniqueId() : null;
        var cached = fileId != null ? fileHashCache.get(fileId) : null;
        if (cached != null) {
            return cached;
        }
        try (var content = resource.getContent()) {
            var md5 = DigestUtils.md5Hex(content);
            if (fileId != null) {
                fileHashCache.put(fileId, md5);
            }
            return md5;
        }
    }

    private String getProjectMD5(AProject wsProject, ProjectVersionH2CacheDB.RepoType repoType) throws IOException {
        var hash = projectVersionCacheDB.getHash(wsProject.getBusinessName(),
                wsProject.getVersion().getVersionName(),
                wsProject.getVersion().getVersionInfo().getCreatedAt(),
                repoType);
        if (StringUtils.isEmpty(hash)) {
            // One version on its own: there is no sibling version to share file hashes with.
            hash = computeMD5(wsProject, new HashMap<>());
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
