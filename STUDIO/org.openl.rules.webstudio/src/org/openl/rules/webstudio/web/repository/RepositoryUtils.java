package org.openl.rules.webstudio.web.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.repository.api.*;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Repository Utilities
 *
 * @author Aleh Bykhavets
 */
public final class RepositoryUtils {
    public static final Comparator<AProjectArtefact> ARTEFACT_COMPARATOR = new Comparator<AProjectArtefact>() {
        public int compare(AProjectArtefact o1, AProjectArtefact o2) {
            if (o1.isFolder() == o2.isFolder()) {
                return o1.getName().compareTo(o2.getName());
            } else {
                return (o1.isFolder() ? -1 : 1);
            }
        }
    };

    private RepositoryUtils() {
    }

    /**
     * @deprecated
     */
    public static RulesUserSession getRulesUserSession() {
        return (RulesUserSession) FacesUtils.getSessionParam(Constants.RULES_USER_SESSION);
    }

    /**
     * @return user's workspace or <code>null</code>
     * @deprecated
     */
    public static UserWorkspace getWorkspace() {
        final Logger log = LoggerFactory.getLogger(RepositoryUtils.class);
        try {
            return getRulesUserSession().getUserWorkspace();
        } catch (Exception e) {
            log.error("Error obtaining user workspace", e);
        }
        return null;
    }

    public static String getTreeNodeId(String name) {
        if (StringUtils.isNotBlank(name)) {
            // FIXME name.hashCode() can produce collisions. Not good for id.
            return String.valueOf(name.hashCode());
        }
        return null;
    }

    public static void archive(FolderRepository folderRepository,
            String rulesPath,
            String projectName,
            String version,
            OutputStream out) throws IOException {
        ZipOutputStream zipOutputStream = null;
        try {
            zipOutputStream = new ZipOutputStream(out);

            String projectPath = rulesPath + projectName + "/";
            folderRepository = getRepositoryForVersion(folderRepository, rulesPath, projectName, version);
            List<FileData> files = folderRepository.listFiles(projectPath, version);

            for (FileData file : files) {
                String internalPath = file.getName().substring(projectPath.length());
                zipOutputStream.putNextEntry(new ZipEntry(internalPath));

                FileItem fileItem = folderRepository.readHistory(file.getName(), file.getVersion());
                try (InputStream content = fileItem.getStream()) {
                    IOUtils.copy(content, zipOutputStream);
                }

                zipOutputStream.closeEntry();
            }
            zipOutputStream.finish();
        } finally {
            IOUtils.closeQuietly(zipOutputStream);
        }
    }

    static FolderRepository getRepositoryForVersion(FolderRepository folderRepo,
            String rulesPath,
            String projectName,
            String version) throws IOException {
        String srcProjectPath = rulesPath + projectName + "/";
        if (folderRepo.supports().branches()) {
            BranchRepository branchRepository = (BranchRepository) folderRepo;
            if (branchRepository.checkHistory(srcProjectPath, version) != null) {
                // Use main branch
                return folderRepo;
            } else {
                // Use secondary branch
                List<String> branches = branchRepository.getBranches(projectName);
                for (String branch : branches) {
                    BranchRepository secondaryBranch = branchRepository.forBranch(branch);
                    FileData fileData = secondaryBranch.checkHistory(srcProjectPath, version);
                    if (fileData != null) {
                        return (FolderRepository) secondaryBranch;
                    }
                }

                return folderRepo;
            }
        } else {
            return folderRepo;
        }
    }
}
