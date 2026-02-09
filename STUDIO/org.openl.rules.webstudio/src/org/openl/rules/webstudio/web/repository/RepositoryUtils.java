package org.openl.rules.webstudio.web.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.webstudio.web.repository.deployment.DeploymentOutputStream;
import org.openl.rules.workspace.dtr.FolderMapper;
import org.openl.rules.workspace.dtr.impl.FileMappingData;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;

/**
 * Repository Utilities
 *
 * @author Aleh Bykhavets
 */
public final class RepositoryUtils {
    public static final Comparator<AProjectArtefact> ARTEFACT_COMPARATOR = Comparator
            .comparing(AProjectArtefact::isFolder)
            .reversed()
            .thenComparing(AProjectArtefact::getName);

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

    public static String getTreeNodeId(String repoId, String name) {
        return getTreeNodeId(repoId) + "_" + getTreeNodeId(name);
    }

    public static String getTreeNodeId(String name) {
        if (StringUtils.isNotBlank(name)) {
            // FIXME name.hashCode() can produce collisions. Not good for id.
            return String.valueOf(name.hashCode());
        }
        return null;
    }

    public static void archive(Repository folderRepository,
                               String rulesPath,
                               String projectName,
                               String version,
                               OutputStream out,
                               Manifest manifest) throws IOException {
        ZipOutputStream zipOutputStream = null;
        try {
            zipOutputStream = new DeploymentOutputStream(out, manifest);

            String projectPath = rulesPath + projectName + "/";
            folderRepository = getRepositoryForVersion(folderRepository, rulesPath, projectName, version);
            List<FileData> files = folderRepository.listFiles(projectPath, version);

            for (FileData file : files) {
                String internalPath = file.getName().substring(projectPath.length());
                if (JarFile.MANIFEST_NAME.equals(internalPath)) {
                    // skip old manifest
                    continue;
                }
                zipOutputStream.putNextEntry(new ZipEntry(internalPath));

                FileMappingData fileMappingData = file.getAdditionalData(FileMappingData.class);
                String name = file.getName();
                if (fileMappingData != null) {
                    name = fileMappingData.getInternalPath();
                }
                FileItem fileItem = folderRepository.readHistory(name, file.getVersion());
                try (InputStream content = fileItem.getStream()) {
                    content.transferTo(zipOutputStream);
                }

                zipOutputStream.closeEntry();
            }
            zipOutputStream.finish();
        } finally {
            IOUtils.closeQuietly(zipOutputStream);
        }
    }

    /**
     * Includes generated manifest to the first position of deployed archive. The old manifest file will be skipped
     *
     * @param in       project input stream
     * @param out      target output stream
     * @param manifest manifest file to include
     * @throws IOException
     */
    public static void includeManifestAndRepackArchive(InputStream in,
                                                       OutputStream out,
                                                       Manifest manifest) throws IOException {
        try (ZipInputStream zipIn = new ZipInputStream(in);
             ZipOutputStream zipOut = new DeploymentOutputStream(out, manifest)) {
            byte[] buffer = new byte[64 * 1024];
            ZipEntry entry = zipIn.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory() && !JarFile.MANIFEST_NAME.equals(entry.getName())) {
                    zipOut.putNextEntry(entry);
                    IOUtils.copy(zipIn, zipOut, buffer);
                    zipOut.closeEntry();
                }
                entry = zipIn.getNextEntry();
            }
            zipOut.finish();
        }
    }

    static Repository getRepositoryForVersion(Repository folderRepo,
                                              String rulesPath,
                                              String projectName,
                                              String version) throws IOException {
        String srcProjectPath = rulesPath + projectName;
        if (folderRepo.supports().mappedFolders()) {
            srcProjectPath = ((FolderMapper) folderRepo).getRealPath(srcProjectPath);
        }
        if (folderRepo.supports().branches()) {
            BranchRepository branchRepository = (BranchRepository) folderRepo;
            if (branchRepository.checkHistory(srcProjectPath + "/", version) != null) {
                // Use main branch
                return folderRepo;
            } else {
                // Use secondary branch
                List<String> branches = branchRepository.getBranches(srcProjectPath);
                for (String branch : branches) {
                    BranchRepository secondaryBranch = branchRepository.forBranch(branch);
                    if (secondaryBranch.checkHistory(srcProjectPath + "/", version) != null) {
                        return secondaryBranch;
                    }
                }

                return folderRepo;
            }
        } else {
            return folderRepo;
        }
    }

    /**
     * Build project version using the following pattern {@code %modifiedBy%-%modifiedAt:yyyy-MM-dd_HH-mm-ss%}
     *
     * @param fileData project file data
     * @return project version
     */
    public static String buildProjectVersion(FileData fileData) {
        if (fileData == null) {
            return null;
        }
        String modifiedOnStr = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(fileData.getModifiedAt());
        String name = Optional.ofNullable(fileData.getAuthor()).map(UserInfo::getName).orElse(null);
        return name + "-" + modifiedOnStr;
    }
}
