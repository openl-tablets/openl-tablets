package org.openl.rules.workspace.lw.impl;

import java.io.File;
import java.io.IOException;

import org.openl.rules.project.impl.local.ModificationHandler;

public class LocalProjectModificationHandler implements ModificationHandler {
    private static final String MARKER_FILE_NAME = ".modified";
    private final File root;

    public LocalProjectModificationHandler(File root) {
        this.root = root;
    }

    @Override
    public void notifyModified(String path) {
        File marker = getMarkerFile(path);
        File propertiesFolder = marker.getParentFile();
        if (propertiesFolder.getParentFile().exists()) {
            if (!propertiesFolder.exists()) {
                if (!propertiesFolder.mkdir() && !propertiesFolder.exists()) {
                    throw new IllegalStateException("Can't create the folder " + propertiesFolder);
                }
            }
            if (propertiesFolder.exists()) {
                try {
                    marker.createNewFile();
                } catch (IOException e) {
                    throw new IllegalStateException("Can't create the file " + marker.getAbsolutePath(), e);
                }
            }
        }
    }

    @Override
    public boolean isModified(String path) {
        return getMarkerFile(path).exists();
    }

    @Override
    public void clearModifyStatus(String path) {
        File marker = getMarkerFile(path);
        if (!marker.delete() && marker.exists()) {
            throw new IllegalStateException("Can't delete the file " + marker.getAbsolutePath());
        }
    }

    private File getMarkerFile(String path) {
        if (!new File(path).isAbsolute()) {
            path = path.replace(File.separatorChar, '/');
            File projectFolder = new File(root, path.split("/")[0]);
            return new File(new File(projectFolder, FolderHelper.PROPERTIES_FOLDER), MARKER_FILE_NAME);
        } else {
            String rootAbsolutePath = root.getAbsolutePath();
            return getMarkerFile(path.substring(rootAbsolutePath.length() + 1));
        }
    }
}
