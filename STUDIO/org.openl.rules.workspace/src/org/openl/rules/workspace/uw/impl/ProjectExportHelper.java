package org.openl.rules.workspace.uw.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;

public final class ProjectExportHelper {
    private ProjectExportHelper() {
    }

    public static File export(WorkspaceUser user, AProject project) throws ProjectException {
        File zipFile = null;
        try {
            String zipComment = "Project '" + project.getName() + "' version " + project.getFileData()
                .getVersion() + "\nExported by " + user.getUserName();

            zipFile = File.createTempFile("export-", "-zip");
            packIntoZip(zipFile, project, zipComment);
            return zipFile;
        } catch (ProjectException e) {
            FileUtils.deleteQuietly(zipFile);
            throw e;
        } catch (Exception e) {
            FileUtils.deleteQuietly(zipFile);
            throw new ProjectException("Failed to export project due I/O error.", e);
        }
    }

    private static void packDir(ZipOutputStream zipOutputStream, AProjectFolder dir) throws IOException,
                                                                                     ProjectException {
        Collection<AProjectArtefact> artefacts = dir.getArtefacts();
        if (artefacts.isEmpty()) {
            return;
        }

        for (AProjectArtefact artefact : artefacts) {
            if (artefact.isFolder()) {
                // Create zip entry for the folder even if it's empty, but don't add root folder to the zip
                ZipEntry entry = new ZipEntry(artefact.getInternalPath() + "/");
                zipOutputStream.putNextEntry(entry);
                packDir(zipOutputStream, (AProjectFolder) artefact);
            } else {
                packFile(zipOutputStream, (AProjectResource) artefact);
            }
        }
    }

    private static void packFile(ZipOutputStream zipOutputStream, AProjectResource file) throws IOException,
                                                                                         ProjectException {
        ZipEntry entry = new ZipEntry(file.getInternalPath());
        zipOutputStream.putNextEntry(entry);

        InputStream source = null;
        try {
            source = file.getContent();
            IOUtils.copy(source, zipOutputStream);
        } finally {
            if (source != null) {
                source.close();
            }
        }

        zipOutputStream.closeEntry();
    }

    private static void packIntoZip(File zipFile, AProject project, String zipComment) throws IOException,
                                                                                       ProjectException {
        FileOutputStream fileOutputStream = null;
        ZipOutputStream zipOutputStream = null;

        try {
            fileOutputStream = new FileOutputStream(zipFile);

            if (project.isFolder()) {
                zipOutputStream = new ZipOutputStream(fileOutputStream);
                zipOutputStream.setLevel(9);
                zipOutputStream.setComment(zipComment);
                packDir(zipOutputStream, project);
            } else {
                FileItem fileItem;
                if (project.isHistoric()) {
                    fileItem = project.getRepository()
                        .readHistory(project.getFolderPath(), project.getFileData().getVersion());
                } else {
                    fileItem = project.getRepository().read(project.getFolderPath());
                }

                IOUtils.copyAndClose(fileItem.getStream(), fileOutputStream);
            }
        } finally {
            if (zipOutputStream != null) {
                zipOutputStream.close();
            }
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
        }
    }
}
