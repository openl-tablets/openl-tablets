package org.openl.rules.project.impl.local;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.file.FileRepository;
import org.openl.util.IOUtils;

public class LocalRepository extends FileRepository {
    private final PropertiesEngine propertiesEngine;

    public LocalRepository(File location) {
        super(location);
        propertiesEngine = new PropertiesEngine(location);
    }

    @Override
    public List<FileData> list(String path) throws IOException {
        List<FileData> list = super.list(path);

        for (Iterator<FileData> iterator = list.iterator(); iterator.hasNext(); ) {
            FileData fileData = iterator.next();
            if (propertiesEngine.isPropertyFile(fileData.getName())) {
                // Property files must be hidden
                iterator.remove();
            }
        }

        return list;
    }

    @Override
    public FileData save(FileData data, InputStream stream) throws IOException {
        FileData fileData = super.save(data, stream);
        notifyModified(data.getName());
        return fileData;
    }

    @Override
    public boolean delete(String name) {
        boolean deleted = super.delete(name);
        if (deleted) {
            notifyModified(name);
        }
        return deleted;
    }

    @Override
    public FileData copy(String srcPath, FileData destData) throws IOException {
        FileData fileData = super.copy(srcPath, destData);
        notifyModified(destData.getName());
        return fileData;
    }

    @Override
    public FileData rename(String path, FileData destData) throws IOException {
        FileData fileData = super.rename(path, destData);
        notifyModified(destData.getName());
        return fileData;
    }

    public ProjectState getProjectState(final String pathInProject) {
        return new ProjectState() {
            private static final String MODIFIED_FILE_NAME = ".modified";
            private static final String VERSION_FILE_NAME = ".version";
            private static final String VERSION_PROPERTY = "version";

            @Override
            public void notifyModified() {
                if (propertiesEngine.isEmptyProject(pathInProject)) {
                    propertiesEngine.deleteAllProperties(pathInProject);
                    invokeListener();
                    return;
                }

                propertiesEngine.createPropertiesFile(pathInProject, MODIFIED_FILE_NAME);
                invokeListener();
            }

            @Override
            public boolean isModified() {
                return propertiesEngine.getPropertiesFile(pathInProject, MODIFIED_FILE_NAME).exists();
            }

            @Override
            public void clearModifyStatus() {
                propertiesEngine.deletePropertiesFile(pathInProject, MODIFIED_FILE_NAME);
            }

            @Override
            public void setProjectVersion(String version) {
                if (version == null) {
                    propertiesEngine.deletePropertiesFile(pathInProject, VERSION_FILE_NAME);
                    return;
                }

                File file = propertiesEngine.createPropertiesFile(pathInProject, VERSION_FILE_NAME);

                Properties properties = new Properties();
                properties.setProperty(VERSION_PROPERTY, version);
                FileOutputStream os = null;
                try {
                    os = new FileOutputStream(file);
                    properties.store(os, "Project version");
                } catch (IOException e) {
                    throw new IllegalStateException(version);
                } finally {
                    IOUtils.closeQuietly(os);
                }
            }

            @Override
            public String getProjectVersion() {
                File file = propertiesEngine.getPropertiesFile(pathInProject, VERSION_FILE_NAME);
                if (!file.exists()) {
                    return null;
                }

                Properties properties = new Properties();
                FileInputStream is = null;
                try {
                    is = new FileInputStream(file);
                    properties.load(is);
                    return properties.getProperty(VERSION_PROPERTY);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
        };
    }

    private void notifyModified(String path) {
        getProjectState(path).notifyModified();
    }
}
