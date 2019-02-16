package org.openl.rules.project.impl.local;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.openl.rules.repository.api.FileChange;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.file.FileSystemRepository;
import org.openl.util.IOUtils;

public class LocalRepository extends FileSystemRepository {
    private final PropertiesEngine propertiesEngine;

    public LocalRepository(File location) {
        setRoot(location);
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
    public FileData save(FileData folderData, Iterable<FileChange> files) throws IOException {
        FileData fileData = super.save(folderData, files);
        notifyModified(folderData.getName());
        return fileData;
    }

    @Override
    public boolean delete(FileData data) {
        boolean deleted = super.delete(data);
        if (deleted) {
            notifyModified(data.getName());
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
            private static final String DATE_FORMAT = "yyyy-MM-dd";
            private static final String MODIFIED_FILE_NAME = ".modified";
            private static final String VERSION_FILE_NAME = ".version";
            private static final String VERSION_PROPERTY = "version";
            private static final String BRANCH_PROPERTY = "branch";
            private static final String AUTHOR_PROPERTY = "author";
            private static final String MODIFIED_AT_PROPERTY = "modified-at";
            private static final String SIZE_PROPERTY = "size";
            private static final String COMMENT_PROPERTY = "comment";

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

            @Override
            public void saveFileData(FileData fileData) {
                if (fileData.getVersion() == null || fileData.getAuthor() == null || fileData.getModifiedAt() == null) {
                    // No need to save empty fileData
                    return;
                }
                Properties properties = new Properties();
                properties.setProperty(VERSION_PROPERTY, fileData.getVersion());
                properties.setProperty(AUTHOR_PROPERTY, fileData.getAuthor());
                properties.setProperty(MODIFIED_AT_PROPERTY, new SimpleDateFormat(DATE_FORMAT).format(fileData.getModifiedAt()));
                properties.setProperty(SIZE_PROPERTY, "" + fileData.getSize());
                if (fileData.getComment() != null) {
                    properties.setProperty(COMMENT_PROPERTY, fileData.getComment());
                }
                String branch = fileData.getBranch();
                if (branch != null) {
                    properties.setProperty(BRANCH_PROPERTY, branch);
                }
                FileOutputStream os = null;
                try {
                    File file = propertiesEngine.createPropertiesFile(pathInProject, VERSION_FILE_NAME);
                    os = new FileOutputStream(file);
                    properties.store(os, "Project version");
                } catch (IOException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                } finally {
                    IOUtils.closeQuietly(os);
                }
            }

            @Override
            public FileData getFileData() {
                File file = propertiesEngine.getPropertiesFile(pathInProject, VERSION_FILE_NAME);
                if (!file.exists()) {
                    return null;
                }

                Properties properties = new Properties();
                FileInputStream is = null;
                try {
                    is = new FileInputStream(file);
                    properties.load(is);
                    FileData fileData = new FileData();
                    File propertiesFolder = propertiesEngine.getPropertiesFolder(pathInProject);
                    File projectFolder = propertiesFolder.getParentFile();

                    String name = projectFolder.getName();
                    String version = properties.getProperty(VERSION_PROPERTY);
                    String branch = properties.getProperty(BRANCH_PROPERTY);
                    String author = properties.getProperty(AUTHOR_PROPERTY);
                    String modifiedAt = properties.getProperty(MODIFIED_AT_PROPERTY);
                    String size = properties.getProperty(SIZE_PROPERTY);
                    String comment = properties.getProperty(COMMENT_PROPERTY);

                    if (version == null || author == null || modifiedAt == null) {
                        // Only partial information is available. Can't fill FileData. Must request from repository.
                        return null;
                    }

                    fileData.setName(name);
                    fileData.setVersion(version);
                    fileData.setBranch(branch);
                    fileData.setAuthor(author);
                    fileData.setModifiedAt(new SimpleDateFormat(DATE_FORMAT).parse(modifiedAt));
                    fileData.setSize(Long.parseLong(size));
                    fileData.setComment(comment);

                    return fileData;
                } catch (IOException | ParseException e) {
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
