package org.openl.rules.webstudio.web.util;

import org.apache.commons.lang3.StringUtils;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.impl.local.ProjectState;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.workspace.dtr.impl.FileMappingData;

public final class ProjectArtifactUtils {
    private ProjectArtifactUtils() {
    }

    private static String getRepoPath(FileData fileData) {
        FileMappingData fileMappingData = fileData.getAdditionalData(FileMappingData.class);
        if (fileMappingData != null) {
            return fileMappingData.getInternalPath();
        } else {
            return fileData.getName();
        }
    }

    private static String failSafePath(String s) {
        int d = s.lastIndexOf(":");
        if (d > 0) {
            return s.substring(0, d);
        }
        return s;
    }

    public static String extractResourceName(AProjectArtefact projectArtefact) {
        if (projectArtefact.getRepository() instanceof LocalRepository) {
            LocalRepository localRepository = (LocalRepository) projectArtefact.getRepository();
            ProjectState projectState = localRepository
                .getProjectState(projectArtefact.getProject().getFileData().getName());
            if (projectState.getFileData() == null) {
                return failSafePath(projectArtefact.getArtefactPath().getStringValue());
            }
            String repoPath = getRepoPath(projectState.getFileData());
            return StringUtils.isBlank(
                projectArtefact.getInternalPath()) ? repoPath : repoPath + "/" + projectArtefact.getInternalPath();
        } else {
            return getRepoPath(projectArtefact.getFileData());
        }
    }
}
