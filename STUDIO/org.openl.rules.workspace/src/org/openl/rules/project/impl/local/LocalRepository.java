package org.openl.rules.project.impl.local;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String VERSION_PROPERTY = "version";
    private static final String BRANCH_PROPERTY = "branch";
    private static final String AUTHOR_PROPERTY = "author";
    private static final String MODIFIED_AT_PROPERTY = "modified-at";
    private static final String MODIFIED_AT_LONG_PROPERTY = "modified-at-long";
    private static final String SIZE_PROPERTY = "size";
    private static final String COMMENT_PROPERTY = "comment";
    private static final String UNIQUE_ID_PROPERTY = "unique-id";
    private static final String FILE_MODIFIED_PROPERTY = "modified";
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

        for (Iterator<FileData> iterator = list.iterator(); iterator.hasNext();) {
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
    public List<FileData> save(List<FileItem> fileItems) throws IOException {
        List<FileData> result = super.save(fileItems);
        for (FileData data : result) {
            notifyModified(data.getName());
        }
        return result;
    }

    @Override
    public FileData save(FileData folderData, final Iterable<FileChange> files, ChangesetType changesetType) throws IOException {
        FileData fileData = super.save(folderData, files, changesetType);
        notifyModified(folderData.getName());
        return fileData;
    }

    @Override
    public List<FileData> save(List<FolderItem> folderItems, ChangesetType changesetType) throws IOException {
        List<FileData> result = super.save(folderItems, changesetType);
        for (FileData data : result) {
            notifyModified(data.getName());
        }
        return result;
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
        String uniqueId = properties.getProperty(UNIQUE_ID_PROPERTY);
        if (uniqueId != null) {
            // If the file is modified, set unique id to null to mark that it's id is unknown
            if (isFileModified(fileData, properties)) {
                uniqueId = null;
            }
            fileData.setUniqueId(uniqueId);
        }

        return fileData;
    }

    /**
     * The file is modified if any of these is true:
     * a) it's marked as modified in properties file
     * b) size is changed
     * c) last modified time is changed
     *
     * @param fileData   file data for checking file
     * @param properties properties of original file
     * @return true if file is modified
     */
    private boolean isFileModified(FileData fileData, Properties properties) {
        boolean modified = Boolean.parseBoolean(properties.getProperty(FILE_MODIFIED_PROPERTY));
        if (modified) {
            return true;
        }

        try {
            long size = Long.parseLong(properties.getProperty(SIZE_PROPERTY));
            if (fileData.getSize() != size) {
                return true;
            }
        } catch (NumberFormatException ignored) {
            // Can't determine saved size. So treat it as modified file
            return true;
        }

        try {
            Date modifiedAt = new Date(Long.parseLong(properties.getProperty(MODIFIED_AT_LONG_PROPERTY)));
            return !modifiedAt.equals(fileData.getModifiedAt());
        } catch (NumberFormatException ignored) {
            // Can't determine saved date. So treat it as modified file
            return true;
        }
    }

    @Override
    protected boolean isSkip(File file) {
        return FolderHelper.PROPERTIES_FOLDER.equals(file.getName());
    }

    public ProjectState getProjectState(final String pathInProject) {
        return new ProjectState() {
            private static final String MODIFIED_FILE_NAME = ".modified";
            private static final String VERSION_FILE_NAME = ".version";

            @Override
            public void notifyModified() {
                if (propertiesEngine.isEmptyProject(pathInProject)) {
                    propertiesEngine.deleteAllProperties(pathInProject);
                    invokeListener();
                    return;
                }

                propertiesEngine.createPropertiesFile(pathInProject, MODIFIED_FILE_NAME);
                setFileModified(pathInProject);
                invokeListener();
            }

            @Override
            public boolean isModified() {
                return propertiesEngine.getPropertiesFile(pathInProject, MODIFIED_FILE_NAME).exists();
            }

            @Override
            public void clearModifyStatus() {
                propertiesEngine.deletePropertiesFile(pathInProject, MODIFIED_FILE_NAME);
                File propertiesFolder = propertiesEngine.getPropertiesFolder(pathInProject);
                File[] files = new File(propertiesFolder, FILE_PROPERTIES_FOLDER).listFiles();
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

                            properties.remove(FILE_MODIFIED_PROPERTY);

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
                properties.setProperty(MODIFIED_AT_PROPERTY,
                    new SimpleDateFormat(DATE_FORMAT).format(fileData.getModifiedAt()));
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

    private void setFileModified(String path) {
        Properties properties = readFileProperties(path);
        properties.setProperty(FILE_MODIFIED_PROPERTY, "true");

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

    public void updateFileProperties(FileData fileData) {
        String path = fileData.getName();
        String filePropertiesPath = getFilePropertiesPath(path);

        if (StringUtils.isNotEmpty(filePropertiesPath)) {
            Properties properties = readFileProperties(path);

            if (fileData.getUniqueId() != null) {
                properties.setProperty(UNIQUE_ID_PROPERTY, fileData.getUniqueId());
            } else {
                properties.remove(UNIQUE_ID_PROPERTY);
            }
            properties.setProperty(MODIFIED_AT_LONG_PROPERTY, "" + fileData.getModifiedAt().getTime());
            properties.setProperty(SIZE_PROPERTY, "" + fileData.getSize());

            File file = propertiesEngine.createPropertiesFile(path, filePropertiesPath);
            try (FileOutputStream os = new FileOutputStream(file)) {
                properties.store(os, FILE_PROPERTIES_COMMENT);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                FileUtils.deleteQuietly(file);
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

    public void deleteAllFileProperties(String path) {
        File propertiesFolder = propertiesEngine.getPropertiesFolder(path);
        File fileProps = new File(propertiesFolder, FILE_PROPERTIES_FOLDER);
        FileUtils.deleteQuietly(fileProps);
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

}
