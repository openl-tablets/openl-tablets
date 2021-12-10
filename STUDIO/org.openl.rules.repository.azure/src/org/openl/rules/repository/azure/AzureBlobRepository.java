package org.openl.rules.repository.azure;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.http.rest.Response;
import com.azure.core.util.BinaryData;
import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.*;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.azure.storage.common.StorageSharedKeyCredential;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.openl.rules.repository.api.*;
import org.openl.rules.repository.common.ChangesMonitor;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

/**
 * Azure Blob Storage repository.<br/>
 * Emulates storing projects as folders. Projects are versioned.<br/>
 * Each project version description is called Azure Commit and contains an author,
 * commit message and file names with their versions for that particular commit.<br/>
 * Also, it is possible to read and save only modified files when opening old project version or saving a project
 * in WebStudio (see supports().uniqueFileId() feature).
 * 
 * @author Nail Samatov
 */
public class AzureBlobRepository implements FolderRepository {
    private static final String UNSUPPORTED_IN_FOLDER_REPOSITORY = "Unsupported in folder repository";
    static final String VERSION_FILE = "versions.yaml";

    private static final String MODIFICATION_FILE = "[openl]/.modification";
    static final String CONTENT_PREFIX = "[content]/";
    static final String VERSIONS_PREFIX = "[openl]/versions/";

    private final Logger log = LoggerFactory.getLogger(AzureBlobRepository.class);

    private String id;
    private String name;
    private ChangesMonitor monitor;
    private int listenerTimerPeriod = 10;
    private String uri;
    private String accountName;
    private String accountKey;

    private BlobContainerClient blobContainerClient;
    private PassiveExpiringMap<CacheKey, AzureCommit> commitsCache;

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMonitor(ChangesMonitor monitor) {
        this.monitor = monitor;
    }

    public void setListenerTimerPeriod(int listenerTimerPeriod) {
        this.listenerTimerPeriod = listenerTimerPeriod;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setAccountKey(String accountKey) {
        this.accountKey = accountKey;
    }

    public void initialize() {
        if (blobContainerClient == null) {
            final BlobContainerClientBuilder builder = new BlobContainerClientBuilder().endpoint(uri);
            if (StringUtils.isNotEmpty(accountName)) {
                builder.credential(new StorageSharedKeyCredential(accountName, accountKey));
            }

            blobContainerClient = builder.buildClient();
        }

        commitsCache = new PassiveExpiringMap<>(10, TimeUnit.SECONDS);

        monitor = new ChangesMonitor(this::getLatestRevision, listenerTimerPeriod);
    }

    @Override
    public List<FileData> listFolders(String path) throws IOException {
        try {
            final String pathPrefix = VERSIONS_PREFIX + path;

            List<FileData> folders = new ArrayList<>();

            AzureCommit commit = findCommit(path, null);
            if (commit != null) {
                // Get sub-folders inside the project.
                final int start = path.length();
                Set<String> subFolders = new HashSet<>();
                List<FileInfo> files = commit.getFiles();
                if (files == null) {
                    return folders;
                }
                for (FileInfo file : files) {
                    String name = file.getPath();
                    if (name.startsWith(path)) {
                        String subFolder = name.substring(start, name.indexOf('/', start));
                        if (!subFolders.contains(subFolder)) {
                            folders.add(createFileData(path + subFolder, commit));
                            subFolders.add(subFolder);
                        }
                    }
                }
            } else {
                // Get folders outside of projects (folders containing projects).
                ListBlobsOptions options = new ListBlobsOptions();
                options.setPrefix(pathPrefix);
                final PagedIterable<BlobItem> items = blobContainerClient.listBlobs(options, null);
                for (BlobItem item : items) {
                    final String name = item.getName();
                    if (name.startsWith(pathPrefix)) {
                        final String filePath = name.substring(VERSIONS_PREFIX.length());
                        final String folderPath = filePath.substring(0, filePath.indexOf('/', path.length()));
                        folders.add(createFileData(folderPath, getCommit(item)));
                    }
                }
            }

            return folders;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public List<FileData> listFiles(String path, String version) throws IOException {
        try {
            AzureCommit commit = findCommit(path, version);
            return getFilesForCommit(commit, path);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public FileData save(FileData folderData, Iterable<FileItem> files, ChangesetType changesetType) throws IOException {
        String path = folderData.getName();

        final FileData fileData;
        try {
            AzureCommit commit = new AzureCommit();
            commit.setAuthor(folderData.getAuthor().getUsername());
            commit.setComment(folderData.getComment());

            final ArrayList<FileInfo> commitFiles = new ArrayList<>();
            commit.setFiles(commitFiles);

            if (changesetType == ChangesetType.FULL) {
                for (FileItem file : files) {
                    Response<BlockBlobItem> response = saveFile(file);

                    FileInfo fileInfo = new FileInfo();
                    fileInfo.setPath(file.getData().getName());
                    fileInfo.setRevision(response.getValue().getVersionId());
                    commitFiles.add(fileInfo);
                }
            } else {
                String baseVersion = folderData.getVersion();
                AzureCommit baseCommit = getCommit(folderData.getName(), baseVersion);
                if (baseCommit != null && baseCommit.getFiles() != null) {
                    commitFiles.addAll(baseCommit.getFiles());
                }

                for (FileItem file : files) {
                    final InputStream stream = file.getStream();
                    final String filePath = file.getData().getName();
                    if (stream == null) {
                        commitFiles.removeIf(f -> f.getPath().equals(filePath));
                    } else {
                        Response<BlockBlobItem> response = saveFile(file);
                        String revision = response.getValue().getVersionId();

                        final Optional<FileInfo> existingFile = commitFiles.stream().filter(f -> f.getPath().equals(filePath)).findAny();
                        if (existingFile.isPresent()) {
                            existingFile.get().setRevision(revision);
                        } else {
                            final FileInfo fileInfo = new FileInfo();
                            fileInfo.setPath(filePath);
                            fileInfo.setRevision(revision);
                            commitFiles.add(fileInfo);
                        }
                    }
                }
            }
            commit.setModifiedAt(new Date());

            saveCommit(commit, path);
            fileData = createFileData(path, commit);

            onModified();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }

        return fileData;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<FileData> list(String path) throws IOException {
        try {
            BlobClient client = findCommitBlob(path, null);
            if (client != null) {
                return getFilesForCommit(getCommit(client), path);
            } else {
                // Get files inside a folder outside of projects (folders containing projects).
                ListBlobsOptions options = new ListBlobsOptions();
                options.setPrefix(VERSIONS_PREFIX + path);
                PagedIterable<BlobItem> items = blobContainerClient.listBlobs(options, null);
                List<FileData> result = new ArrayList<>();
                for (BlobItem item : items) {
                    result.addAll(getFilesForCommit(getCommit(item), path));
                }

                return result;
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public FileData check(String name) throws IOException {
        try {
            final BlobClient client = blobContainerClient.getBlobClient(CONTENT_PREFIX + name);
            if (client.exists()) {
                // File
                return createFileDataForFile(name, null, client);
            } else {
                // Project folder or sub-folder.
                return createFileDataForFolder(name, null);
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public FileItem read(String name) throws IOException {
        try {
            final BlobClient client = blobContainerClient.getBlobClient(CONTENT_PREFIX + name);
            if (client.exists()) {
                FileData fileData = createFileDataForFile(name, null, client);
                return  new FileItem(fileData, client.openInputStream());
            }

            return null;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public FileData save(FileData data, InputStream stream) throws IOException {
        throw new UnsupportedOperationException(UNSUPPORTED_IN_FOLDER_REPOSITORY);
    }

    @Override
    public List<FileData> save(List<FileItem> fileItems) throws IOException {
        throw new UnsupportedOperationException(UNSUPPORTED_IN_FOLDER_REPOSITORY);
    }

    @Override
    public boolean delete(FileData data) throws IOException {
        try {
            String path = data.getName();

            AzureCommit commit = new AzureCommit();
            commit.setAuthor(data.getAuthor().getUsername());
            commit.setComment(data.getComment());
            commit.setDeleted(true);
            commit.setModifiedAt(new Date());

            saveCommit(commit, path);

            onModified();

            return true;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean delete(List<FileData> data) throws IOException {
        throw new UnsupportedOperationException(UNSUPPORTED_IN_FOLDER_REPOSITORY);
    }

    @Override
    public void setListener(Listener callback) {
        if (monitor != null) {
            monitor.setListener(callback);
        }
    }

    @Override
    public List<FileData> listHistory(String name) throws IOException {
        try {
            // TODO: What if name is a file name?
            // Current implementation works only if name is a project name.
            // If will invoke this method for files and project sub-folders, we should improve this method.
            String pathPrefix = VERSIONS_PREFIX + name + "/" + VERSION_FILE;

            ListBlobsOptions options = new ListBlobsOptions();
            options.setPrefix(pathPrefix);
            options.setDetails(new BlobListDetails().setRetrieveVersions(true));

            List<AzureCommit> commits = new ArrayList<>();
            final PagedIterable<BlobItem> items = blobContainerClient.listBlobs(options, null);
            for (BlobItem item : items) {
                commits.add(getCommit(item));
            }

            List<FileData> fileDataList = new ArrayList<>(commits.size());
            for (AzureCommit commit : commits) {
                fileDataList.add(createFileData(name, commit));
            }

            return fileDataList;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public FileData checkHistory(String name, String version) throws IOException {
        try {
            BlobClient client = findFile(name, version);
            if (client != null) {
                // File
                return createFileDataForFile(name, version, client);
            } else {
                // Project folder or sub-folder.
                return createFileDataForFolder(name, version);
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public FileItem readHistory(String name, String version) throws IOException {
        try {
            BlobClient client = findFile(name, version);
            if (client != null) {
                FileData fileData = createFileDataForFile(name, version, client);
                final BinaryData binaryData = client.downloadContent();

                return new FileItem(fileData, binaryData.toStream());
            }

            return null;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean deleteHistory(FileData data) throws IOException {
        try {
            final String path = data.getName();
            final String version = data.getVersion();

            if (version == null) {
                // Delete all versions.
                deleteAllByPrefix(VERSIONS_PREFIX + path);
                deleteAllByPrefix(CONTENT_PREFIX + path);
                synchronized (this) {
                    commitsCache.entrySet().removeIf(e -> e.getKey().name.equals(path));
                }
            } else {
                // Undelete
                final List<FileData> history = listHistory(path);
                if (history.isEmpty()) {
                    return false;
                }

                AzureCommit commit = new AzureCommit();
                commit.setAuthor(data.getAuthor().getUsername());
                commit.setComment(data.getComment());
                commit.setModifiedAt(new Date());

                boolean found = false;
                final ListIterator<FileData> listIterator = history.listIterator(history.size());
                while (listIterator.hasPrevious()) {
                    final FileData fileData = listIterator.previous();
                    if (!fileData.isDeleted()) {
                        final AzureCommit oldCommit = getCommit(path, fileData.getVersion());
                        if (oldCommit == null) {
                            continue;
                        }
                        commit.setFiles(oldCommit.getFiles());
                        found = true;
                        break;
                    }
                }

                if (found) {
                    saveCommit(commit, path);
                } else {
                    return false;
                }
            }

            onModified();
            return true;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }


    @Override
    public FileData copyHistory(String srcName, FileData destData, String version) throws IOException {
        try {
            final List<FileData> fileDataList = listFiles(srcName + "/", version);

            String path = destData.getName();

            final FileData fileData;
            AzureCommit commit = new AzureCommit();
            commit.setAuthor(destData.getAuthor().getUsername());
            commit.setComment(destData.getComment());
            final ArrayList<FileInfo> commitFiles = new ArrayList<>();
            commit.setFiles(commitFiles);
            commit.setModifiedAt(new Date());

            for (FileData data : fileDataList) {
                final FileItem file = readHistory(data.getName(), data.getVersion());
                final Response<BlockBlobItem> response;
                String newFile = path + data.getName().substring(srcName.length());
                try (final InputStream stream = file.getStream()) {
                    BlobClient blobClient = blobContainerClient.getBlobClient(CONTENT_PREFIX + newFile);
                    response = blobClient.uploadWithResponse(new BlobParallelUploadOptions(BinaryData.fromStream(stream)).setRequestConditions(
                            new BlobRequestConditions()),
                            null, Context.NONE);
                }

                FileInfo fileInfo = new FileInfo();
                fileInfo.setPath(newFile);
                fileInfo.setRevision(response.getValue().getVersionId());
                commitFiles.add(fileInfo);
            }

            saveCommit(commit, path);
            fileData = createFileData(destData.getName(), commit);

            onModified();

            return fileData;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public Features supports() {
        return new FeaturesBuilder(this).setSupportsUniqueFileId(true).build();
    }

    @Override
    public void close() {
        if (monitor != null) {
            monitor.release();
            monitor = null;
        }

        if (blobContainerClient != null) {
            blobContainerClient = null;
        }
    }

    @Override
    public void validateConnection() throws IOException {
        try {
            ListBlobsOptions options = new ListBlobsOptions();
            options.setPrefix(VERSIONS_PREFIX);
            blobContainerClient.listBlobs(options, null).iterator();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private List<FileData> getFilesForCommit(AzureCommit commit, String filterPath) {
        if (commit == null) {
            return Collections.emptyList();
        }
        final List<FileInfo> files = commit.getFiles();
        return files == null ? Collections.emptyList()
                             : files.stream()
                                 .filter(fileInfo -> fileInfo.getPath().startsWith(filterPath))
                                 .map(fileInfo -> {
                                     FileData fileData = new FileData();
                                     fileData.setName(fileInfo.getPath());
                                     fileData.setVersion(commit.getVersion());
                                     fileData.setUniqueId(fileInfo.getRevision());

                                     return fileData;
                                 })
                                 .collect(Collectors.toList());
    }

    private Response<BlockBlobItem> saveFile(FileItem file) throws IOException {
        FileData data = file.getData();
        BlobClient blobClient = blobContainerClient.getBlobClient(CONTENT_PREFIX + data.getName());

        BlobRequestConditions blobRequestConditions = new BlobRequestConditions();
        final Response<BlockBlobItem> response;
        try (final InputStream stream = file.getStream()) {
            response = blobClient.uploadWithResponse(new BlobParallelUploadOptions(BinaryData.fromStream(stream)).setRequestConditions(blobRequestConditions),
                null, Context.NONE);
        }
        return response;
    }

    private Object getLatestRevision() {
        BlobClient client = blobContainerClient.getBlobClient(MODIFICATION_FILE);

        try {
            // Avoid making extra request. If file is absent, getProperties() will throw exception.
            return client.getProperties().getCreationTime().toInstant().toEpochMilli();
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
        }

        return null;
    }

    private void onModified() {
        BlobClient client = blobContainerClient.getBlobClient(MODIFICATION_FILE);
        if (client.exists()) {
            client.delete();
        }
        client.upload(BinaryData.fromBytes(new byte[0]));

        // Invoke listener if exist
        if (monitor != null) {
            monitor.fireOnChange();
        }
    }

    private AzureCommit getCommit(String path, String version) {
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return getCommit(blobContainerClient.getBlobVersionClient(VERSIONS_PREFIX + path + "/" + VERSION_FILE, version));
    }

    private AzureCommit getCommit(BlobItem item) {
        return getCommit(blobContainerClient.getBlobVersionClient(item.getName(), item.getVersionId()));
    }

    private AzureCommit getCommit(BlobClient client) {
        if (client == null) {
            return null;
        }
        String versionId = client.getVersionId();
        if (versionId == null) {
            try {
                versionId = client.getProperties().getVersionId();
            } catch (Exception e) {
                // Blob is absent
                log.debug(e.getMessage(), e);
                return null;
            }
        }
        synchronized (this) {
            if (versionId != null) {
                final AzureCommit cached = commitsCache.get(new CacheKey(client.getBlobName(), versionId));
                if (cached != null) {
                    return cached;
                }
            }
        }

        try (InputStreamReader in = new InputStreamReader(client.openInputStream(), StandardCharsets.UTF_8)) {
            Yaml yaml = createYamlForCommit();
            final AzureCommit commit = yaml.loadAs(in, AzureCommit.class);
            commit.setVersion(versionId);
            String blobName = client.getBlobName();
            if (!blobName.startsWith(VERSIONS_PREFIX) || !blobName.endsWith("/" + VERSION_FILE)) {
                throw new IllegalStateException("Unexpected blob name: " + blobName);
            }
            commit.setPath(blobName.substring(VERSIONS_PREFIX.length(), blobName.length() - VERSION_FILE.length() - 1));
            synchronized (this) {
                commitsCache.put(new CacheKey(blobName, versionId), commit);
            }
            return commit;
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            return null;
        }
    }

    private void saveCommit(AzureCommit commit, String path) throws IOException {
        final Yaml yaml = createYamlForCommit();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (OutputStreamWriter out = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            yaml.dump(commit, out);
        }

        final BlobClient blobClient = blobContainerClient.getBlobClient(VERSIONS_PREFIX + path + "/" + VERSION_FILE);

        final Response<BlockBlobItem> response = blobClient
            .uploadWithResponse(new BlobParallelUploadOptions(BinaryData.fromBytes(outputStream.toByteArray()))
                .setRequestConditions(new BlobRequestConditions()), null, Context.NONE);
        commit.setVersion(response.getValue().getVersionId());
    }

    static Yaml createYamlForCommit() {
        TypeDescription projectsDescription = new TypeDescription(AzureCommit.class);
        projectsDescription.addPropertyParameters("files", FileInfo.class);
        projectsDescription.setExcludes("version");
        projectsDescription.setExcludes("path");
        Constructor constructor = new Constructor(projectsDescription);
        Representer representer = new Representer();
        representer.addTypeDescription(projectsDescription);
        representer.getPropertyUtils().setSkipMissingProperties(true);
        return new Yaml(constructor, representer);
    }

    private void deleteAllByPrefix(String pathPrefix) {
        ListBlobsOptions options = new ListBlobsOptions();
        options.setPrefix(pathPrefix);
        final PagedIterable<BlobItem> items = blobContainerClient.listBlobs(options, null);
        for (BlobItem item : items) {
            blobContainerClient.getBlobClient(item.getName()).delete();
        }
    }

    private FileData createFileData(String name, AzureCommit commit) {
        FileData fileData = new FileData();
        fileData.setName(name);
        fileData.setVersion(commit.getVersion());
        fileData.setComment(commit.getComment());
        fileData.setAuthor(new UserInfo(commit.getAuthor()));
        fileData.setModifiedAt(commit.getModifiedAt());
        fileData.setDeleted(commit.isDeleted());
        return fileData;
    }

    private FileData createFileDataForFile(String name, String version, BlobClient client) {
        BlobProperties properties = client.getProperties();

        FileData fileData = new FileData();
        fileData.setName(name);
        fileData.setVersion(version == null ? properties.getVersionId() : version);
        fileData.setUniqueId(properties.getVersionId());
        fileData.setModifiedAt(new Date(properties.getCreationTime().toInstant().toEpochMilli()));
        fileData.setSize(properties.getBlobSize());
        return fileData;
    }

    private FileData createFileDataForFolder(String name, String version) {
        final String folderPath = name + "/";
        AzureCommit commit = findCommit(folderPath, version);
        if (commit != null) {
            if (commit.getPath().equals(name)) {
                // It's a project name.
                return createFileData(name, commit);
            }
            if (commit.getFiles() == null) {
                // Commit doesn't contain files and thus doesn't contain any folders.
                return null;
            }
            // Detecting if commit contains sub-folder with path "folderPath".
            for (FileInfo file : commit.getFiles()) {
                if (file.getPath().startsWith(folderPath)) {
                    // We found a file inside searched folder. We can create File Data for it.
                    return createFileData(name, commit);
                }
            }
        }

        return null;
    }

    private AzureCommit findCommit(String path, String version) {
        String name = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        final String commitName = VERSIONS_PREFIX + name + "/" + VERSION_FILE;
        if (version != null) {
            synchronized (this) {
                final AzureCommit cached = commitsCache.get(new CacheKey(commitName, version));
                if (cached != null) {
                    return cached;
                }
            }
        } else {
            boolean isCommitName;
            synchronized (this) {
                isCommitName = commitsCache.keySet().stream().anyMatch(k -> k.name.equals(commitName));
            }
            if (isCommitName) {
                return getCommit(blobContainerClient.getBlobClient(VERSIONS_PREFIX + name + "/" + VERSION_FILE));
            }
        }
        return getCommit(findCommitBlob(path, version));
    }

    private BlobClient findCommitBlob(String name, String version) {
        String commitName = name;
        if (!commitName.contains("/")) {
            return null;
        }

        do {
            commitName = commitName.substring(0, commitName.lastIndexOf("/"));
            BlobClient client = blobContainerClient.getBlobVersionClient(VERSIONS_PREFIX + commitName + "/" + VERSION_FILE, version);
            if (client.exists()) {
                return client;
            }
        } while (commitName.contains("/"));

        return null;
    }

    private BlobClient findFile(String name, String version) {
        final AzureCommit commit = findCommit(name, version);
        if (commit != null && commit.getFiles() != null) {
            Optional<FileInfo> fileInfo = commit.getFiles().stream().filter(f -> f.getPath().equals(name)).findFirst();
            if (fileInfo.isPresent()) {
                BlobClient client = blobContainerClient.getBlobVersionClient(CONTENT_PREFIX + fileInfo.get().getPath(), fileInfo.get().getRevision());

                if (client.exists()) {
                    return client;
                }
            }
        }

        return null;
    }

    /**
     * Is used in tests only
     */
    void setBlobContainerClient(BlobContainerClient client) {
        this.blobContainerClient = client;
    }

    private static final class CacheKey {
        final String name;
        final String version;

        private CacheKey(String name, String version) {
            this.name = name;
            this.version = version;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheKey cacheKey = (CacheKey) o;
            return name.equals(cacheKey.name) && version.equals(cacheKey.version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, version);
        }
    }
}