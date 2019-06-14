package org.openl.rules.workspace.dtr.impl;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.openl.rules.repository.RRepositoryFactory;
import org.openl.rules.repository.RepositoryMode;
import org.openl.rules.repository.api.*;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

public class MappedRepository implements FolderRepository, BranchRepository, RRepositoryFactory, Closeable {
    private static final Pattern PROJECT_PROPERTY_PATTERN = Pattern.compile("(project\\.\\d+\\.)\\w+");
    private final Logger log = LoggerFactory.getLogger(MappedRepository.class);

    private FolderRepository delegate;

    private volatile Map<String, String> externalToInternal = Collections.emptyMap();

    private ReadWriteLock mappingLock = new ReentrantReadWriteLock();
    private RepositoryMode repositoryMode;
    private String configFile;
    private String baseFolder;

    public void setDelegate(FolderRepository delegate) {
        this.delegate = delegate;
    }

    public void setRepositoryMode(RepositoryMode repositoryMode) {
        this.repositoryMode = repositoryMode;
    }

    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    public void setBaseFolder(String baseFolder) {
        this.baseFolder = baseFolder;
    }

    private void setExternalToInternal(Map<String, String> externalToInternal) {
        Lock lock = mappingLock.writeLock();
        try {
            lock.lock();
            this.externalToInternal = Collections.unmodifiableMap(externalToInternal);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        Lock lock = mappingLock.writeLock();
        try {
            lock.lock();
            externalToInternal = Collections.emptyMap();

            if (delegate instanceof Closeable) {
                ((Closeable) delegate).close();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<FileData> list(String path) throws IOException {
        Map<String, String> mapping = getMappingForRead();

        List<FileData> internal = new ArrayList<>();
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            String external = entry.getKey();
            if (external.startsWith(path)) {
                internal.addAll(delegate.list(entry.getValue() + "/"));
            } else if (path.startsWith(external + "/")) {
                internal.addAll(delegate.list(toInternal(mapping, path)));
            }
        }

        return toExternal(mapping, internal);
    }

    @Override
    public FileData check(String name) throws IOException {
        Map<String, String> mapping = getMappingForRead();
        return toExternal(mapping, delegate.check(toInternal(mapping, name)));
    }

    @Override
    public FileItem read(String name) throws IOException {
        Map<String, String> mapping = getMappingForRead();
        return toExternal(mapping, delegate.read(toInternal(mapping, name)));
    }

    @Override
    public FileData save(FileData data, InputStream stream) throws IOException {
        Map<String, String> mapping = getMappingForRead();
        return toExternal(mapping, delegate.save(toInternal(mapping, data), stream));
    }

    @Override
    public List<FileData> save(List<FileItem> fileItems) throws IOException {
        Map<String, String> mapping = getMappingForRead();
        List<FileItem> fileItemsInternal = fileItems.stream()
                .map(fi -> new FileItem(toInternal(mapping, fi.getData()), fi.getStream()))
                .collect(Collectors.toList());
        List<FileData> result = delegate.save(fileItemsInternal);

        return result.stream()
                .map(dt -> toExternal(mapping, dt))
                .collect(Collectors.toList());
    }
    @Override
    public boolean delete(FileData data) {
        Map<String, String> mapping = getMappingForRead();
        return delegate.delete(toInternal(mapping, data));
    }

    @Override
    public void setListener(final Listener callback) {
        delegate.setListener(new Listener() {
            @Override
            public void onChange() {
                try {
                    initialize();
                } catch (Exception e) {
                    log.warn(e.getMessage(), e);
                }

                callback.onChange();
            }
        });
    }

    @Override
    public List<FileData> listHistory(String name) throws IOException {
        Map<String, String> mapping = getMappingForRead();
        return toExternal(mapping, delegate.listHistory(toInternal(mapping, name)));
    }

    @Override
    public FileData checkHistory(String name, String version) throws IOException {
        Map<String, String> mapping = getMappingForRead();
        return toExternal(mapping, delegate.checkHistory(toInternal(mapping, name), version));
    }

    @Override
    public FileItem readHistory(String name, String version) throws IOException {
        Map<String, String> mapping = getMappingForRead();
        return toExternal(mapping, delegate.readHistory(toInternal(mapping, name), version));
    }

    @Override
    public boolean deleteHistory(FileData data) {
        if (data.getVersion() == null) {
            // Store mapping before modification to use it later
            Map<String, String> mapping = getMappingForRead();

            Lock lock = mappingLock.writeLock();
            try {
                lock.lock();

                Map<String, String> newMap = new HashMap<>(externalToInternal);
                newMap.remove(data.getName());
                ByteArrayInputStream inputStream = getStreamFromProperties(newMap);

                FileData configData = new FileData();
                configData.setName(configFile);
                configData.setAuthor(data.getAuthor());
                configData.setComment(data.getComment());
                delegate.save(configData, inputStream);

                // Use mapping before modification
                boolean result = delegate.deleteHistory(toInternal(mapping, data));
                externalToInternal = newMap;
                return result;
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                refreshMapping();
                return false;
            } finally {
                lock.unlock();
            }
        } else {
            Map<String, String> mapping = getMappingForRead();
            return delegate.deleteHistory(toInternal(mapping, data));
        }
    }

    @Override
    public FileData copyHistory(String srcName, FileData destData, String version) throws IOException {
        if (destData instanceof MappedFileData) {
            try {
                ByteArrayInputStream configStream = updateConfigFile((MappedFileData) destData);
                FileData configData = new FileData();
                configData.setName(configFile);
                configData.setAuthor(destData.getAuthor());
                configData.setComment(destData.getComment());
                delegate.save(configData, configStream);

                Map<String, String> mapping = getMappingForRead();
                return toExternal(mapping, delegate.copyHistory(toInternal(mapping, srcName), toInternal(mapping, destData), version));
            } catch (IOException | RuntimeException e) {
                // Failed to update mapping. Restore current saved version.
                refreshMapping();
                throw e;
            }
        } else {
            Map<String, String> mapping = getMappingForRead();
            return toExternal(mapping, delegate.copyHistory(toInternal(mapping, srcName), toInternal(mapping, destData), version));
        }
    }

    @Override
    public List<FileData> listFolders(String path) throws IOException {
        Map<String, String> mapping = getMappingForRead();

        List<FileData> internal = new ArrayList<>();
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            String external = entry.getKey();
            if (external.startsWith(path) && !external.substring(path.length()).contains("/")) {
                // "external" is direct child of "path"
                FileData data = delegate.check(entry.getValue());
                if (data == null) {
                    log.error("Project {} is not found", entry.getValue());
                } else {
                    internal.add(data);
                }
            }
        }

        return toExternal(mapping, internal);
    }

    @Override
    public List<FileData> listFiles(String path, String version) throws IOException {
        Map<String, String> mapping = getMappingForRead();
        return toExternal(mapping, delegate.listFiles(toInternal(mapping, path), version));
    }

    @Override
    public FileData save(FileData folderData, Iterable<FileChange> files, ChangesetType changesetType) throws IOException {
        if (folderData instanceof MappedFileData) {
            try {
                FileChange configChange = new FileChange(configFile, updateConfigFile((MappedFileData) folderData));
                Iterable<FileChange> filesWithMapping = new CompositeFileChanges(files, configChange);

                // Mapping was updated on previous step.
                Map<String, String> mapping = getMappingForRead();
                FileData result = delegate.save(toInternal(mapping, folderData), toInternal(mapping, filesWithMapping),
                        changesetType);
                return toExternal(mapping, result);
            } catch (IOException | RuntimeException e) {
                // Failed to update mapping. Restore current saved version.
                refreshMapping();
                throw e;
            }
        } else {
            Map<String, String> mapping = getMappingForRead();
            return toExternal(mapping, delegate.save(toInternal(mapping, folderData), toInternal(mapping, files),
                    changesetType));
        }
    }

    @Override
    public List<FileData> save(List<FolderItem> folderItems, ChangesetType changesetType) throws IOException {
        if (folderItems.isEmpty()) {
            return Collections.emptyList();
        }

        if (folderItems.get(0).getData() instanceof MappedFileData) {
            throw new UnsupportedEncodingException();
        }
        Map<String, String> mapping = getMappingForRead();

        List<FolderItem> folderItemsInternal = folderItems.stream()
                .map(fi -> new FolderItem(toInternal(mapping, fi.getData()), toInternal(mapping, fi.getFiles())))
                .collect(Collectors.toList());
        List<FileData> result = delegate.save(folderItemsInternal, changesetType);

        return result.stream()
                .map(dt -> toExternal(mapping, dt))
                .collect(Collectors.toList());
    }

    @Override
    public Features supports() {
        return new FeaturesBuilder(delegate)
                .setVersions(delegate.supports().versions())
                .setMappedFolders(true)
                .setSupportsUniqueFileId(delegate.supports().uniqueFileId())
                .build();
    }

    @Override
    public String getBranch() {
        return ((BranchRepository) delegate).getBranch();
    }

    @Override
    public void createBranch(String projectName, String branch) throws IOException {
        ((BranchRepository) delegate).createBranch(projectName, branch);
    }

    @Override
    public void deleteBranch(String projectName, String branch) throws IOException {
        ((BranchRepository) delegate).deleteBranch(projectName, branch);
    }

    @Override
    public List<String> getBranches(String projectName) throws IOException {
        return ((BranchRepository) delegate).getBranches(projectName);
    }

    @Override
    public BranchRepository forBranch(String branch) throws IOException {
        BranchRepository delegateForBranch = ((BranchRepository) delegate).forBranch(branch);

        MappedRepository mappedRepository = new MappedRepository();
        mappedRepository.setDelegate((FolderRepository) delegateForBranch);
        mappedRepository.setRepositoryMode(repositoryMode);
        mappedRepository.setConfigFile(configFile);
        mappedRepository.setBaseFolder(baseFolder);
        try {
            mappedRepository.initialize();
        } catch (RRepositoryException e) {
            throw new IOException(e.getMessage(), e);
        }

        return mappedRepository;
    }

    private Map<String, String> getMappingForRead() {
        Lock lock = mappingLock.readLock();
        Map<String, String> mapping;
        try {
            lock.lock();
            mapping = externalToInternal;
        } finally {
            lock.unlock();
        }
        return mapping;
    }

    private Iterable<FileChange> toInternal(final Map<String, String> mapping, final Iterable<FileChange> files) {
        return new Iterable<FileChange>() {
            @SuppressWarnings("NullableProblems")
            @Override
            public Iterator<FileChange> iterator() {
                return new Iterator<FileChange>() {
                    private final Iterator<FileChange> delegate = files.iterator();

                    @Override
                    public boolean hasNext() {
                        return delegate.hasNext();
                    }

                    @Override
                    public FileChange next() {
                        FileChange external = delegate.next();
                        FileData data = external.getData();
                        String name = toInternal(mapping, external.getData().getName());
                        data.setName(name);
                        return new FileChange(data, external.getStream());
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("Remove is not supported");
                    }
                };
            }
        };
    }

    private FileData toInternal(Map<String, String> externalToInternal, FileData data) {
        FileData copy = new FileData();
        copy.setVersion(data.getVersion());
        copy.setAuthor(data.getAuthor());
        copy.setComment(data.getComment());
        copy.setSize(data.getSize());
        copy.setDeleted(data.isDeleted());
        copy.setName(toInternal(externalToInternal, data.getName()));
        return copy;
    }

    private String toInternal(Map<String, String> externalToInternal, String externalPath) {
        for (Map.Entry<String, String> entry : externalToInternal.entrySet()) {
            String externalBase = entry.getKey();
            if (externalPath.equals(externalBase) || externalPath.startsWith(externalBase + "/")) {
                return entry.getValue() + externalPath.substring(externalBase.length());
            }
        }

        log.debug("Mapping for external folder '{}' is not found. Use it as is.", externalPath);
        return externalPath;
    }

    private List<FileData> toExternal(Map<String, String> externalToInternal, List<FileData> internal) {
        List<FileData> external = new ArrayList<>(internal.size());

        for (FileData data : internal) {
            external.add(toExternal(externalToInternal, data));
        }

        return external;
    }

    private FileItem toExternal(Map<String, String> externalToInternal, FileItem internal) {
        return new FileItem(toExternal(externalToInternal, internal.getData()), internal.getStream());
    }

    private FileData toExternal(Map<String, String> externalToInternal, FileData data) {
        if (data == null) {
            return null;
        }

        data.setName(toExternal(externalToInternal, data.getName()));
        return data;
    }

    private String toExternal(Map<String, String> externalToInternal, String internalPath) {
        for (Map.Entry<String, String> entry : externalToInternal.entrySet()) {
            String internalBase = entry.getValue();
            if (internalPath.equals(internalBase) || internalPath.startsWith(internalBase + "/")) {
                return entry.getKey() + internalPath.substring(internalBase.length());
            }
        }

        // Shouldn't occur. If occurred, it's a bug.
        log.warn("Mapping for internal folder '{}' is not found. Use it as is.", internalPath);
        return internalPath;
    }

    @Override
    public void initialize() throws RRepositoryException {
        try {
            refreshMapping();
        } catch (Exception e) {
            throw new RRepositoryException(e.getMessage(), e);
        }
    }

    /**
     * Load mapping from properties file.
     *
     * @param delegate       original repository
     * @param repositoryMode Repository mode: design or deploy config.
     * @param configFile     properties file
     * @param baseFolder     virtual base folder. WebStudio will think that projects can be found in this folder.
     * @return loaded mapping
     * @throws IOException if it was any error during operation
     */
    private Map<String, String> readExternalToInternalMap(FolderRepository delegate,
            RepositoryMode repositoryMode,
            String configFile,
            String baseFolder) throws IOException {
        baseFolder = StringUtils.isBlank(baseFolder) ? "" : baseFolder.endsWith("/") ? baseFolder : baseFolder + "/";
        Map<String, String> externalToInternal = new HashMap<>();
        FileItem fileItem = delegate.read(configFile);
        if (fileItem == null) {
            log.debug("Repository configuration file {} is not found", configFile);
            return generateExternalToInternalMap(delegate, repositoryMode, baseFolder);
        }

        Properties prop;
        try (InputStreamReader in = new InputStreamReader(fileItem.getStream(), StandardCharsets.UTF_8)) {
            prop = new Properties();
            prop.load(in);
        }

        Set<String> processed = new HashSet<>();
        for (Object key : prop.keySet()) {
            String propertyName = ((String) key);

            Matcher matcher = PROJECT_PROPERTY_PATTERN.matcher(propertyName);
            if (matcher.matches()) {
                String suffix = matcher.group(1);
                if (processed.add(suffix)) {
                    String name = prop.getProperty(suffix + "name");
                    String path = prop.getProperty(suffix + "path");

                    if (name != null && path != null) {
                        if (path.endsWith("/")) {
                            path = path.substring(0, path.length() - 1);
                        }
                        String externalPath = createUniquePath(externalToInternal, baseFolder + name);

                        externalToInternal.put(externalPath, path);
                    }
                }
            }
        }

        return externalToInternal;
    }

    private String createUniquePath(Map<String, String> externalToInternal, String externalPath) {
        // If occasionally such project name exists already, add some suffix to it.
        if (externalToInternal.containsKey(externalPath)) {
            int i = 1;
            String copy = externalPath + "." + i;
            while (externalToInternal.containsKey(copy)) {
                copy = externalPath + "." + (++i);
            }
            externalPath = copy;
        }

        return externalPath;
    }

    /**
     * Detect existing projects and Deploy Configurations based on rules.xml and
     * {@link ArtefactProperties#DESCRIPTORS_FILE}. If there are several projects with same name, suffix will be added
     * to them
     *
     * @param delegate repository to detect projects
     * @param repositoryMode repository mode. If design repository, rules.xml will be searched, otherwise
     *            {@link ArtefactProperties#DESCRIPTORS_FILE}
     * @param baseFolder virtual base folder. WebStudio will think that projects can be found in this folder.
     * @return generated mapping
     */
    private Map<String, String> generateExternalToInternalMap(FolderRepository delegate,
            RepositoryMode repositoryMode,
            String baseFolder) throws IOException {
        Map<String, String> externalToInternal = new HashMap<>();
        List<FileData> allFiles = delegate.list("");
        for (FileData fileData : allFiles) {
            String fullName = fileData.getName();
            String[] nameParts = fullName.split("/");
            if (nameParts.length == 0) {
                continue;
            }
            String fileName = nameParts[nameParts.length - 1];
            if (repositoryMode == RepositoryMode.DESIGN) {
                if ("rules.xml".equals(fileName)) {
                    FileItem fileItem = delegate.read(fullName);
                    try (InputStream stream = fileItem.getStream()) {
                        String projectName = getProjectName(stream);
                        String externalPath = createUniquePath(externalToInternal, baseFolder + projectName);

                        int cutSize = "rules.xml".length() + (nameParts.length > 1 ? 1 : 0); // Exclude "/" if exist
                        String path = fullName.substring(0, fullName.length() - cutSize);
                        externalToInternal.put(externalPath, path);
                    }
                }
            } else if (repositoryMode == RepositoryMode.DEPLOY_CONFIG) {
                if (ArtefactProperties.DESCRIPTORS_FILE.equals(fileName)) {
                    if (nameParts.length < 2) {
                        continue;
                    }

                    String deployConfigName = nameParts[nameParts.length - 2];
                    String externalPath = createUniquePath(externalToInternal, baseFolder + deployConfigName);
                    int cutSize = ArtefactProperties.DESCRIPTORS_FILE.length() + 1; // Exclude "/"
                    String path = fullName.substring(0, fullName.length() - cutSize);
                    externalToInternal.put(externalPath, path);
                }
            }
        }

        return externalToInternal;
    }


    private void refreshMapping() {
        try {
            Map<String, String> currentMapping = readExternalToInternalMap(delegate,
                    repositoryMode,
                    configFile,
                    baseFolder);

            setExternalToInternal(currentMapping);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            setExternalToInternal(Collections.<String, String>emptyMap());
        }
    }

    private ByteArrayInputStream updateConfigFile(MappedFileData folderData) throws IOException {
        Lock lock = mappingLock.writeLock();
        try {
            lock.lock();
            Map<String, String> newMap = new HashMap<>(externalToInternal);
            newMap.put(folderData.getName(), folderData.getInternalPath());

            ByteArrayInputStream configInputStream = getStreamFromProperties(newMap);
            externalToInternal = newMap;
            return configInputStream;
        } finally {
            lock.unlock();
        }
    }

    private ByteArrayInputStream getStreamFromProperties(Map<String, String> newMap) throws IOException {
        String parent = StringUtils.isBlank(baseFolder) ?
                        "" :
                        baseFolder.endsWith("/") ? baseFolder : baseFolder + "/";

        Properties prop = new Properties();
        int i = 1;
        for (Map.Entry<String, String> entry : newMap.entrySet()) {
            if (entry.getKey().length() <= parent.length()) {
                log.warn("Skip mapping for {} to {}", entry.getKey(), entry.getValue());
                continue;
            }
            String name = entry.getKey().substring(parent.length());
            String path = entry.getValue();

            prop.setProperty("project." + i + ".name", name);
            prop.setProperty("project." + i + ".path", path);
            i++;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            prop.store(writer, null);
        }

        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    private String getProjectName(InputStream inputStream) {
        try {
            InputSource inputSource = new InputSource(inputStream);
            XPathFactory factory = XPathFactory.newInstance();
            XPath xPath = factory.newXPath();
            XPathExpression xPathExpression = xPath.compile("/project/name");
            return xPathExpression.evaluate(inputSource);
        } catch (XPathExpressionException e) {
            return null;
        }
    }

    @Override
    public boolean isValidBranchName(String branch) {
        if (delegate instanceof BranchRepository) {
            return ((BranchRepository) delegate).isValidBranchName(branch);
        }
        return true;
    }

    @Override
    public boolean branchExists(String branch) throws IOException {
        return delegate instanceof BranchRepository && ((BranchRepository) delegate).branchExists(branch);
    }
}
