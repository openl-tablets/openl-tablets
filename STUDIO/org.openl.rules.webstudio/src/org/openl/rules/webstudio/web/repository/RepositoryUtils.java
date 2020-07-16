package org.openl.rules.webstudio.web.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;

/**
 * Repository Utilities
 *
 * @author Aleh Bykhavets
 */
public final class RepositoryUtils {
    public static final Comparator<AProjectArtefact> ARTEFACT_COMPARATOR = (o1, o2) -> {
        if (o1.isFolder() == o2.isFolder()) {
            return o1.getName().compareTo(o2.getName());
        } else {
            return o1.isFolder() ? -1 : 1;
        }
    };

    private RepositoryUtils() {
    }

    public static String getTreeNodeId(AProjectArtefact artefact) {
        if (artefact == null) {
            return null;
        }

        String repoId = artefact.getRepository().getId();
        String name = artefact.getName();
        return getTreeNodeId(repoId, name);
    }

    static String getTreeNodeId(String repoId, String name) {
        return getTreeNodeId(repoId) + "_" + getTreeNodeId(name);
    }

    private static String getTreeNodeId(String name) {
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
