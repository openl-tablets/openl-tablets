package org.openl.rules.webstudio.web.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.webstudio.web.repository.deployment.DeploymentOutputStream;
import org.openl.rules.workspace.dtr.FolderMapper;
import org.openl.rules.workspace.dtr.impl.FileMappingData;
import org.openl.util.IOUtils;

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

    public static void archive(Repository folderRepository,
                               String rulesPath,
                               String projectName,
                               String version,
                               OutputStream out,
                               Manifest manifest) throws IOException {
        ZipOutputStream zipOutputStream = null;
        try {
            zipOutputStream = new DeploymentOutputStream(out, manifest);

            var projectPath = rulesPath + projectName + "/";
            folderRepository = getRepositoryForVersion(folderRepository, rulesPath, projectName, version);
            var files = folderRepository.listFiles(projectPath, version);

            for (FileData file : files) {
                var internalPath = file.getName().substring(projectPath.length());
                if (JarFile.MANIFEST_NAME.equals(internalPath)) {
                    // skip old manifest
                    continue;
                }
                zipOutputStream.putNextEntry(new ZipEntry(internalPath));

                var fileMappingData = file.getAdditionalData(FileMappingData.class);
                var name = file.getName();
                if (fileMappingData != null) {
                    name = fileMappingData.getInternalPath();
                }
                var fileItem = folderRepository.readHistory(name, file.getVersion());
                try (var content = fileItem.getStream()) {
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
        try (var zipIn = new ZipInputStream(in);
             var zipOut = new DeploymentOutputStream(out, manifest)) {
            byte[] buffer = new byte[64 * 1024];
            var entry = zipIn.getNextEntry();
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
        var srcProjectPath = rulesPath + projectName;
        if (folderRepo.supports().mappedFolders()) {
            srcProjectPath = ((FolderMapper) folderRepo).getRealPath(srcProjectPath);
        }
        if (folderRepo.supports().branches()) {
            var branchRepository = (BranchRepository) folderRepo;
            if (branchRepository.checkHistory(srcProjectPath + "/", version) != null) {
                // Use main branch
                return folderRepo;
            } else {
                // Use secondary branch
                var branches = branchRepository.getBranches(srcProjectPath);
                for (String branch : branches) {
                    var secondaryBranch = branchRepository.forBranch(branch);
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
        var modifiedOnStr = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(fileData.getModifiedAt());
        var name = Optional.ofNullable(fileData.getAuthor()).map(UserInfo::getName).orElse(null);
        return name + "-" + modifiedOnStr;
    }
}
