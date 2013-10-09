package org.openl.rules.workspace.uw.impl;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.rules.workspace.lw.impl.LocalWorkspaceImpl;

public final class FileExportHelper {
    private static FileFilter FILE_FILTER = new FileFilter() {
        public boolean accept(File pathname) {
            return !FolderHelper.PROPERTIES_FOLDER.equalsIgnoreCase(pathname.getName());
        }
    };

    public static File export(WorkspaceUser user, AProject oldRP, String fileName) throws ProjectException {
        File file = null;
        try {
            file = File.createTempFile("export-", "-file");
        } catch (IOException e) {
            throw new ProjectException("Failed to create temporary file!", e);
        }

        File tempWsLocation = new File(file.getParentFile(), "export-" + System.currentTimeMillis());
        tempWsLocation.mkdir();

        LocalWorkspaceImpl tempWS = new LocalWorkspaceImpl(user, tempWsLocation, FILE_FILTER, FILE_FILTER);
        AProject localProject = tempWS.addProject(oldRP);

        IOException copyException = null;
        try {
            copyFile(file, new File(tempWsLocation, localProject.getName()), fileName);
        } catch (IOException e) {
            copyException = e;
        }

        tempWS.release();
        FolderHelper.deleteFolder(tempWsLocation);

        if (copyException != null) {
            throw new ProjectException("Failed to export file due I/O error!", copyException);
        }

        return file;
    }

    protected static void copyFile(File file, File rootDir, String fileName) throws IOException {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            
            File[] files = rootDir.listFiles();
            if (files == null) {
                return;
            }

            for (File f : files) {
                if (!f.isDirectory() && f.getName().equals(fileName)) {
                    FileInputStream source = null;
                    try {
                        source = new FileInputStream(f);
                        IOUtils.copy(source, fileOutputStream);
                    } finally {
                        if (source != null) {
                            source.close();
                        }
                    }
                }
            }
        } finally {
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
        }
    }
}
