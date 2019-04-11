package org.openl.rules.project.impl.local;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PropertiesEngine {
    private final Logger log = LoggerFactory.getLogger(PropertiesEngine.class);
    private final File root;

    PropertiesEngine(File root) {
        this.root = root;
    }

    public File createPropertiesFile(String pathInProject, String propertiesFileName) {
        File propertiesFolder = getPropertiesFolder(pathInProject);

        File properties = new File(propertiesFolder, propertiesFileName);
        try {
            File parent = properties.getParentFile();
            if (!parent.mkdirs() && !parent.exists()) {
                throw new IllegalStateException("Can't create the folder " + parent);
            }

            properties.createNewFile();
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException("Can't create the file " + properties.getAbsolutePath(), e);
        }
    }

    public boolean deletePropertiesFile(String pathInProject, String propertiesFileName) {
        File file = new File(getPropertiesFolder(pathInProject), propertiesFileName);
        boolean deleted = file.delete();

        File folder = file.getParentFile();
        while (!folder.equals(root) && folder.delete()) {
            folder = folder.getParentFile();
        }

        return deleted;
    }

    public File getPropertiesFile(String pathInProject, String propertiesFileName) {
        return new File(getPropertiesFolder(pathInProject), propertiesFileName);
    }

    public boolean isEmptyProject(String pathInProject) {
        File propertiesFolder = getPropertiesFolder(pathInProject);
        File projectFolder = propertiesFolder.getParentFile();
        File[] files = projectFolder.listFiles();
        return files == null || files.length == 0 || files.length == 1 && files[0].equals(propertiesFolder);
    }

    public void deleteAllProperties(String pathInProject) {
        File propertiesFolder = getPropertiesFolder(pathInProject);
        FileUtils.deleteQuietly(propertiesFolder);

        File folder = propertiesFolder.getParentFile();
        while (!folder.equals(root) && folder.delete()) {
            folder = folder.getParentFile();
        }
    }

    public boolean isPropertyFile(String path) {
        return path.replace(File.separatorChar, '/').contains("/" + FolderHelper.PROPERTIES_FOLDER + "/");
    }

    File getProjectFolder(String path) {
        return getPropertiesFolder(path).getParentFile();
    }

    private File getPropertiesFolder(String path) {
        if (!new File(path).isAbsolute()) {
            path = path.replace(File.separatorChar, '/');
            File projectFolder = new File(root, path.split("/")[0]);
            return new File(projectFolder, FolderHelper.PROPERTIES_FOLDER);
        } else {
            return getPropertiesFolder(getRelativePath(path));
        }
    }

    String getRelativePath(String path) {
        log.debug("Base: {}", root.getAbsolutePath());
        log.debug("File path: {}", path);

        Path base;
        Path pathAbsolute;
        try {
            base = Paths.get(root.getAbsolutePath()).toRealPath();
            pathAbsolute = Paths.get(path).toRealPath();
        } catch (IOException e) {
            throw new IllegalStateException("Can't determine properties folder: " + e.getMessage(), e);
        }
        String relativePath = base.relativize(pathAbsolute).toString();
        log.debug("Relative: {}", relativePath);
        return relativePath;
    }

}
