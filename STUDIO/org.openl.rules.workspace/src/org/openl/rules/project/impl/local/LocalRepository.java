package org.openl.rules.project.impl.local;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.openl.rules.repository.api.*;
import org.openl.rules.repository.file.FileSystemRepository;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalRepository extends FileSystemRepository {
    private static final String PROPERTY_UNIQUE_ID = "unique-id";
    private static final String PROPERTY_FILE_MODIFIED = "modified";
    private static final String FILE_PROPERTIES_FOLDER = "file-properties";
    private static final String FILE_PROPERTIES_COMMENT = "File properties";

    private final Logger log = LoggerFactory.getLogger(LocalRepository.class);
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
        String uniqueId = fileData.getUniqueId();
        if (uniqueId != null) {
            updateFileProperties(data.getName(), PROPERTY_UNIQUE_ID, uniqueId);
        }
        notifyModified(data.getName());
        return fileData;
    }

    @Override
    public FileData save(FileData folderData, final Iterable<FileChange> files, ChangesetType changesetType) throws IOException {
        Iterable<FileChange> changes = new UniqueIdSaverIterable(files);
        FileData fileData = super.save(folderData, changes, changesetType);
        notifyModified(folderData.getName());
        return fileData;
    }

    @Override
    public boolean delete(FileData data) {
        boolean deleted = super.delete(data);
        deleteFileProperties(data.getName());

        if (deleted) {
            notifyModified(data.getName());
        }
        return deleted;
    }

    @Override
    public Features supports() {
        return new FeaturesBuilder(this).setSupportsUniqueFileId(true).setVersions(false).build();
    }

    @Override
    protected FileData getFileData(File file) throws IOException {
        FileData fileData = super.getFileData(file);
        Properties properties = readFileProperties(fileData.getName());
        String uniqueId = properties.getProperty(PROPERTY_UNIQUE_ID);
        if (uniqueId != null) {
            String modified = properties.getProperty(PROPERTY_FILE_MODIFIED);
            // If the file is modified, set unique id to null to mark that it's id is unknown
            if (Boolean.parseBoolean(modified)) {
                uniqueId = null;
            }
            fileData.setUniqueId(uniqueId);
        }

        return fileData;
    }

    @Override
    protected boolean isSkip(File file) {
        return FolderHelper.PROPERTIES_FOLDER.equals(file.getName());
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
                updateFileProperties(pathInProject, PROPERTY_FILE_MODIFIED, "true");
                invokeListener();
            }

            @Override
            public boolean isModified() {
                return propertiesEngine.getPropertiesFile(pathInProject, MODIFIED_FILE_NAME).exists();
            }

            @Override
            public void clearModifyStatus() {
                propertiesEngine.deletePropertiesFile(pathInProject, MODIFIED_FILE_NAME);
                File projectFolder = propertiesEngine.getProjectFolder(pathInProject);
                File[] files = new File(projectFolder, FILE_PROPERTIES_FOLDER).listFiles();
                clearFileModifyStatus(files);
            }

            private void clearFileModifyStatus(File[] files) {
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            Properties properties = new Properties();
                            try (FileInputStream is = new FileInputStream(file)) {
                                properties.load(is);
                            } catch (IOException e) {
                                log.error(e.getMessage(), e);
                            }

                            properties.remove(PROPERTY_FILE_MODIFIED);

                            try (FileOutputStream os = new FileOutputStream(file)) {
                                properties.store(os, FILE_PROPERTIES_COMMENT);
                            } catch (IOException e) {
                                log.error(e.getMessage(), e);
                            }
                        } else if (file.isDirectory()) {
                            clearFileModifyStatus(file.listFiles());
                        }
                    }
                }
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
                    File projectFolder = propertiesEngine.getProjectFolder(pathInProject);

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

    private String getFilePropertiesPath(String path) {
        String relativePath = path;
        if (new File(relativePath).isAbsolute()) {
            relativePath = propertiesEngine.getRelativePath(path).replace(File.separatorChar, '/');
        }
        if (!relativePath.contains("/")) {
            // Not a file. Just project name
            return "";
        }
        String pathInProject = relativePath.substring(relativePath.indexOf('/'));
        return FILE_PROPERTIES_FOLDER + pathInProject;
    }

    private void updateFileProperties(String path, String propertyName, String propertyValue) {
        Properties properties = readFileProperties(path);
        properties.setProperty(propertyName, propertyValue);

        String filePropertiesPath = getFilePropertiesPath(path);
        if (StringUtils.isNotEmpty(filePropertiesPath)) {
            File file = propertiesEngine.createPropertiesFile(path, filePropertiesPath);
            try (FileOutputStream os = new FileOutputStream(file)) {
                properties.store(os, FILE_PROPERTIES_COMMENT);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private Properties readFileProperties(String path) {
        String filePropertiesPath = getFilePropertiesPath(path);
        if (filePropertiesPath.isEmpty()) {
            return new Properties();
        }

        File fileProperties = propertiesEngine.getPropertiesFile(path, filePropertiesPath);

        Properties properties = new Properties();
        if (fileProperties.exists()) {
            try (FileInputStream is = new FileInputStream(fileProperties)) {
                properties.load(is);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return properties;
    }

    private void deleteFileProperties(String path) {
        String filePropertiesPath = getFilePropertiesPath(path);
        if (StringUtils.isNotEmpty(filePropertiesPath)) {
            File fileProperties = propertiesEngine.getPropertiesFile(path, filePropertiesPath);
            if (fileProperties.isFile()) {
                FileUtils.deleteQuietly(fileProperties);
            }
        }
    }

    private class UniqueIdSaverIterable implements Iterable<FileChange> {
        private final Iterable<FileChange> files;

        UniqueIdSaverIterable(Iterable<FileChange> files) {
            this.files = files;
        }

        @Override
        public Iterator<FileChange> iterator() {
            return new Iterator<FileChange>() {
                private Iterator<FileChange> delegate = files.iterator();

                @Override
                public boolean hasNext() {
                    return delegate.hasNext();
                }

                @Override
                public FileChange next() {
                    FileChange change = delegate.next();
                    String uniqueId = change.getUniqueId();
                    String path = change.getName();
                    if (uniqueId != null) {
                        updateFileProperties(path, PROPERTY_UNIQUE_ID, uniqueId);
                    }
                    if (change.getStream() == null) {
                        deleteFileProperties(path);
                    }
                    return change;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("remove");
                }
            };
        }
    }

}
