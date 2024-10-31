package org.openl.security.acl.utils;

import java.util.List;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.impl.local.ProjectState;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.workspace.dtr.impl.FileMappingData;
import org.openl.util.StringUtils;

public final class AclPathUtils {

    private AclPathUtils() {
    }

    /**
     * Concatenates two paths.
     *
     * @param path1 first path
     * @param path2 second path
     * @return concatenated path
     */
    public static String concatPaths(String path1, String path2) {
        if (StringUtils.isBlank(path1)) {
            return path2;
        }
        if (StringUtils.isBlank(path2)) {
            return path1;
        }
        var result = path1;
        if (!result.endsWith("/")) {
            result += "/";
        }
        return result + path2;
    }

    /**
     * Builds full repository path.
     *
     * @param repoId repository id
     * @param path   path
     * @return repository path
     */
    public static String buildRepositoryPath(String repoId, String path) {
        if (StringUtils.isBlank(path)) {
            return repoId;
        }

        var parts = path.split("/");
        var normalizedPath = new StringBuilder(repoId);

        int i = 0;
        for (var part : parts) {
            if (!StringUtils.isBlank(part)) {
                if (i == 0) {
                    normalizedPath.append(':');
                }
                normalizedPath.append('/').append(part.trim());
                i++;
            }
        }

        return normalizedPath.toString();
    }

    /**
     * Extracts internal path from project artefact.
     *
     * @param projectArtefact project artefact
     * @return internal path
     */
    public static String extractInternalPath(AProjectArtefact projectArtefact) {
        if (projectArtefact.getRepository() instanceof LocalRepository) {
            LocalRepository localRepository = (LocalRepository) projectArtefact.getRepository();
            ProjectState projectState = localRepository
                    .getProjectState(projectArtefact.getProject().getFileData().getName());
            if (projectState.getFileData() != null) {
                return getRepoPath(projectState.getFileData()) + "/" + projectArtefact.getInternalPath();
            }
        }
        // Folders has empty file data
        if (projectArtefact.getFileData() != null) {
            return getRepoPath(projectArtefact.getFileData());
        } else {
            // For deleted project fileData is null
            if (projectArtefact.getProject() != null) {
                if (projectArtefact.getProject().getFileData() != null) {
                    return extractInternalPath(projectArtefact.getProject()) + "/" + projectArtefact.getInternalPath();
                } else {
                    List<FileData> fileDatas = projectArtefact.getProject().getHistoryFileDatas();
                    return getRepoPath(fileDatas.get(fileDatas.size() - 1));
                }
            } else {
                // For deployments fileData is null and project is null
                return projectArtefact.getArtefactPath().getStringValue();
            }
        }
    }

    private static String getRepoPath(FileData fileData) {
        FileMappingData fileMappingData = fileData.getAdditionalData(FileMappingData.class);
        if (fileMappingData != null) {
            return fileMappingData.getInternalPath();
        } else {
            return fileData.getName();
        }
    }

}
