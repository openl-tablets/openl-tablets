package org.openl.rules.workspace.uw.impl;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.rules.workspace.lw.impl.LocalWorkspaceImpl;

public class ProjectExportHelper {
    private static FileFilter FILE_FILTER = new FileFilter() {
        public boolean accept(File pathname) {
            return !FolderHelper.PROPERTIES_FOLDER.equalsIgnoreCase(pathname.getName());
        }
    };

    private byte[] buffer = new byte[1024 * 4];

    public File export(WorkspaceUser user, AProject oldRP) throws ProjectException {
        File zipFile = null;
        try {
            zipFile = File.createTempFile("export-", "-zip");
        } catch (IOException e) {
            throw new ProjectException("Failed to create temporary file!", e);
        }

        File tempWsLocation = new File(zipFile.getParentFile(), "export-" + System.currentTimeMillis());
        tempWsLocation.mkdir();

        LocalWorkspaceImpl tempWS = new LocalWorkspaceImpl(user, tempWsLocation, FILE_FILTER, FILE_FILTER);
        AProject localProject = tempWS.addProject(oldRP);

        String zipComment = "Project '" + oldRP.getName() + "' version " + oldRP.getVersion().getVersionName()
                + "\nExported by " + user.getUserName();

        IOException packException = null;
        try {
            packIntoZip(zipFile, new File(tempWsLocation, localProject.getName()), zipComment);
        } catch (IOException e) {
            packException = e;
        }

        tempWS.release();
        FolderHelper.deleteFolder(tempWsLocation);

        if (packException != null) {
            throw new ProjectException("Failed to export project due I/O error!", packException);
        }

        return zipFile;
    }

    protected void packDir(ZipOutputStream zipOutputStream, File dir, String path) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File f : files) {
            if (f.isDirectory()) {
                packDir(zipOutputStream, f, path + f.getName() + "/");
            } else {
                packFile(zipOutputStream, f, path);
            }
        }
    }

    protected void packFile(ZipOutputStream zipOutputStream, File file, String path) throws IOException {
        ZipEntry entry = new ZipEntry(path + file.getName());
        zipOutputStream.putNextEntry(entry);

        FileInputStream source = null;
        try {
            source = new FileInputStream(file);
            IOUtils.copy(source, zipOutputStream);
        } finally {
            if (source != null) {
                source.close();
            }
        }

        zipOutputStream.closeEntry();
    }

    protected void packIntoZip(File zipFile, File rootDir, String zipComment) throws IOException {
        FileOutputStream fileOutputStream = null;
        ZipOutputStream zipOutputStream = null;

        try {
            fileOutputStream = new FileOutputStream(zipFile);
            zipOutputStream = new ZipOutputStream(fileOutputStream);
            zipOutputStream.setLevel(9);
            zipOutputStream.setComment(zipComment);

            packDir(zipOutputStream, rootDir, "");
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
