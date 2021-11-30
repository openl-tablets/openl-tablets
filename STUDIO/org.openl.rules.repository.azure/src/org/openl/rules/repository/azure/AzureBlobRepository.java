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
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobRequestConditions;
import com.azure.storage.blob.models.BlockBlobItem;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.azure.storage.blob.specialized.BlobInputStream;
import com.azure.storage.common.implementation.Constants;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.openl.rules.repository.api.*;
import org.openl.rules.repository.common.ChangesMonitor;
import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

// TODO: Check that all streams are closed properly
public class AzureBlobRepository implements FolderRepository {
    private static final String UNSUPPORTED_IN_FOLDER_REPOSITORY = "Unsupported in folder repository";
    private static final String VERSION_EXTENSION = ".yaml";
    private final Logger log = LoggerFactory.getLogger(AzureBlobRepository.class);

    private static final String MODIFICATION_FILE = "[openl]/.modification";
    private static final String CONTENT_PREFIX = "[content]/";
    private static final String VERSIONS_PREFIX = "[openl]/versions/";
    private static final String LOCKS_PREFIX = "[openl]/locks/";
    private static final String VERSIONS_SEPARATOR = "/[versions]/";

    private String id;
    private String name;
    private ChangesMonitor monitor;
    private int listenerTimerPeriod = 10;
    private String uri;

    private BlobContainerClient blobContainerClient;
    private PassiveExpiringMap<String, AzureCommit> commitsCache;

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

    public void initialize() {
        blobContainerClient = new BlobContainerClientBuilder()
                .endpoint(uri)
                .buildClient();

        commitsCache = new PassiveExpiringMap<>(10, TimeUnit.SECONDS);

        monitor = new ChangesMonitor(this::getLatestRevision, listenerTimerPeriod);
    }

    @Override
    public List<FileData> listFolders(String path) {
        final String pathPrefix = VERSIONS_PREFIX + path;

        Map<String, BlobItem> folderPaths = new LinkedHashMap<>();
        ListBlobsOptions options = new ListBlobsOptions();
        options.setPrefix(pathPrefix);
        // TODO: Set timeout
        // TODO: Set max results per response
        final PagedIterable<BlobItem> items = blobContainerClient.listBlobs(options, null);
        for (BlobItem item : items) {
            final String name = item.getName();
            if (name.startsWith(pathPrefix)) {
                final String filePath = name.substring(VERSIONS_PREFIX.length());
                final String folderPath = filePath.substring(0, filePath.indexOf('/', path.length()));
                final BlobItem existingItem = folderPaths.get(folderPath);
                if (existingItem == null) {
                    folderPaths.put(folderPath, item);
                } else {
                    Long existingVersion = getVersion(existingItem);
                    Long newVersion = getVersion(item);
                    if (existingVersion == null || newVersion != null && newVersion > existingVersion) {
                        folderPaths.put(folderPath, item);
                    }
                }
            }
        }

        List<FileData> folders = new ArrayList<>();
        for (Map.Entry<String, BlobItem> entry : folderPaths.entrySet()) {
            AzureCommit commit = getCommit(entry.getValue());
            folders.add(createFileData(entry.getKey(), commit));
        }
        return folders;
    }

    @Override
    public List<FileData> listFiles(String path, String version) {
        AzureCommit commit = getCommit(path, version);
        return getFilesForCommit(commit);
    }

    private List<FileData> getFilesForCommit(AzureCommit commit) {
        if (commit == null) {
            return Collections.emptyList();
        }
        final List<FileInfo> files = commit.getFiles();
        return files == null ? Collections.emptyList() : files.stream().map(fileInfo -> {
            FileData fileData = new FileData();
            fileData.setName(fileInfo.getPath());
            fileData.setVersion(String.valueOf(commit.getVersion()));
            fileData.setUniqueId(fileInfo.getRevision());
            // TODO: Use base64 to decode and encode metadata to support non-ascii symbols
//            fileData.setAuthor(client.getProperties().getMetadata().get("author")); // TODO: Implement
//            fileData.setComment(client.getProperties().getMetadata().get("comment")); // TODO: Implement

            return fileData;
        }).collect(Collectors.toList());
    }

    @Override
    public FileData save(FileData folderData, Iterable<FileItem> files, ChangesetType changesetType) throws IOException {
        String path = folderData.getName();
        final long version = lock(path);

        final FileData fileData;
        try {
            AzureCommit commit = new AzureCommit();
            commit.setVersion(version);
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
                if (baseVersion == null) {
                    baseVersion = String.valueOf(getLatestVersion(folderData.getName()));
                }
                AzureCommit baseCommit = getCommit(folderData.getName(), baseVersion);
                if (baseCommit != null) {
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

            saveCommit(commit, path, version);
            fileData = createFileData(path, commit);
        } catch (Exception e) {
            // TODO: Rollback saved revisions if possible and saved version info
            throw e;
        } finally {
            unlock(path, version);
        }

        onModified();

        return fileData;
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
        if (path.endsWith("/")) {
            // TODO: Remove such lines
            path = path.substring(0, path.length() - 1);
        }
        ListBlobsOptions options = new ListBlobsOptions().setPrefix(VERSIONS_PREFIX + path + VERSIONS_SEPARATOR);
        final PagedIterable<BlobItem> items = blobContainerClient.listBlobs(options, null);
        final Iterator<BlobItem> iterator = items.iterator();
        if (iterator.hasNext()) {
            return getFilesForCommit(getCommit(getLatestBlob(iterator)));
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public FileData check(String name) throws IOException {

        ListBlobsOptions options = new ListBlobsOptions().setPrefix(VERSIONS_PREFIX + name + VERSIONS_SEPARATOR);
        final PagedIterable<BlobItem> items = blobContainerClient.listBlobs(options, null);
        final Iterator<BlobItem> iterator = items.iterator();
        if (iterator.hasNext()) {
            return createFileData(name, getCommit(getLatestBlob(iterator)));
        } else {
            final BlobClient client = blobContainerClient.getBlobClient(CONTENT_PREFIX + name);
            if (client.exists()) {
                final FileData fileData = new FileData();
                fileData.setName(name);
                fileData.setSize(client.getProperties().getBlobSize());
                fileData.setModifiedAt(new Date(client.getProperties().getCreationTime().toInstant().toEpochMilli()));
                fileData.setVersion(client.getVersionId()); // TODO: Set commit version instead
                fileData.setUniqueId(client.getVersionId());
//            fileData.setDeleted(client.isDeleted()); // TODO: Do we need it for files?

                // TODO: Use base64 to decode and encode metadata to support non-ascii symbols
                fileData.setAuthor(new UserInfo(client.getProperties().getMetadata().get("author"))); // TODO: Implement
                fileData.setComment(client.getProperties().getMetadata().get("comment")); // TODO: Implement
                return fileData;
            }
        }

        return null;
    }

    @Override
    public FileItem read(String name) throws IOException {
        final BlobClient client = blobContainerClient.getBlobClient(CONTENT_PREFIX + name);
        if (client.exists()) {
            final FileData fileData = new FileData();
            fileData.setName(name);
            fileData.setSize(client.getProperties().getBlobSize());
            fileData.setModifiedAt(new Date(client.getProperties().getCreationTime().toInstant().toEpochMilli()));
            fileData.setVersion(client.getVersionId()); // TODO: Set commit version instead
            fileData.setUniqueId(client.getVersionId());
//            fileData.setDeleted(client.isDeleted()); // TODO: Do we need it for files?

            // TODO: Use base64 to decode and encode metadata to support non-ascii symbols
            fileData.setAuthor(new UserInfo(client.getProperties().getMetadata().get("author"))); // TODO: Implement
            fileData.setComment(client.getProperties().getMetadata().get("comment")); // TODO: Implement
            return  new FileItem(fileData, client.openInputStream());
        }

        return null;
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
        String path = data.getName();
        final long version = lock(path);

        try {
            AzureCommit commit = new AzureCommit();
            commit.setVersion(version);
            commit.setAuthor(data.getAuthor().getUsername());
            commit.setComment(data.getComment());
            commit.setDeleted(true);
            commit.setModifiedAt(new Date());

            saveCommit(commit, path, version);

            onModified();

            return true;
        } finally {
            unlock(path, version);
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
    public List<FileData> listHistory(String name) {
        // TODO: What if name is a file name?
        String pathPrefix = VERSIONS_PREFIX + name;

        ListBlobsOptions options = new ListBlobsOptions();
        options.setPrefix(pathPrefix);
        // TODO: Set timeout
        // TODO: Set max results per response

        List<AzureCommit> commits = new ArrayList<>();
        final PagedIterable<BlobItem> items = blobContainerClient.listBlobs(options, null);
        for (BlobItem item : items) {
            commits.add(getCommit(item));
        }

        commits.sort(Comparator.comparingLong(AzureCommit::getVersion));

        List<FileData> fileDataList = new ArrayList<>(commits.size());
        for (AzureCommit commit : commits) {
            fileDataList.add(createFileData(name, commit));
        }

        return fileDataList;
    }

    @Override
    public FileData checkHistory(String name, String version) {
        BlobClient client = blobContainerClient.getBlobClient(VERSIONS_PREFIX + name + VERSIONS_SEPARATOR + version + VERSION_EXTENSION);
        if (client.exists()) {
            // folder
            AzureCommit commit = getCommit(client);
            return commit == null ? null : createFileData(name, commit);
        } else {
            // file
            client = findFile(name, version);
            if (client != null) {
                FileData fileData = new FileData();
                fileData.setName(name);
                fileData.setVersion(version);
                fileData.setUniqueId(client.getVersionId());
                // TODO: Add modified at field
                //            fileData.setModifiedAt();
                return fileData;
            }

            return null;
        }

    }

    @Override
    public FileItem readHistory(String name, String version) {
        BlobClient client = findFile(name, version);
        if (client != null) {
            FileData fileData = new FileData();
            fileData.setName(name);
            fileData.setVersion(version);
            fileData.setUniqueId(client.getVersionId());
            // TODO: Add modified at field
            //            fileData.setModifiedAt();
            final BinaryData binaryData = client.downloadContent();
            final Long length = binaryData.getLength();
            if (length != null) {
                fileData.setSize(length);
            }

            return new FileItem(fileData, binaryData.toStream());
        }

        return null;
    }

    @Override
    public boolean deleteHistory(FileData data) throws IOException {
        final String path = data.getName();
        final String version = data.getVersion();

        if (version == null) {
            // Delete all versions.
            deleteAllByPrefix(VERSIONS_PREFIX + path);
            deleteAllByPrefix(CONTENT_PREFIX + path);
        } else {
            // Undelete
            final long newVersion = lock(path);
            try {
                final List<FileData> history = listHistory(path);
                if (history.isEmpty()) {
                    return false;
                }

                AzureCommit commit = new AzureCommit();
                commit.setVersion(newVersion);
                commit.setAuthor(data.getAuthor().getUsername());
                commit.setComment(data.getComment());
                commit.setModifiedAt(new Date());

                boolean found = false;
                final ListIterator<FileData> listIterator = history.listIterator(history.size());
                while (listIterator.hasPrevious()) {
                    final FileData fileData = listIterator.previous();
                    if (!fileData.isDeleted()) {
                        final AzureCommit oldCommit = getCommit(path, fileData.getVersion());
                        commit.setFiles(oldCommit.getFiles());
                        found = true;
                        break;
                    }
                }

                if (found) {
                    saveCommit(commit, path, newVersion);
                } else {
                    return false;
                }
            } finally {
                unlock(path, newVersion);
            }
        }

        onModified();
        return true;
    }


    @Override
    public FileData copyHistory(String srcName, FileData destData, String version) throws IOException {
        // TODO: Probably can be optimized
        final List<FileData> fileDataList = listFiles(srcName, version);

        String path = destData.getName();
        final long newVersion = lock(path);

        final FileData fileData;
        try {
            AzureCommit commit = new AzureCommit();
            commit.setVersion(newVersion);
            commit.setAuthor(destData.getAuthor().getUsername());
            commit.setComment(destData.getComment());
            final ArrayList<FileInfo> commitFiles = new ArrayList<>();
            commit.setFiles(commitFiles);
            commit.setModifiedAt(new Date());

            for (FileData data : fileDataList) {
                final BlobClient blobClient = blobContainerClient.getBlobClient(CONTENT_PREFIX + data.getName());

                BlobRequestConditions blobRequestConditions = new BlobRequestConditions();
                blobRequestConditions.setIfNoneMatch(Constants.HeaderConstants.ETAG_WILDCARD);
                final FileItem file = readHistory(data.getName(), data.getVersion());
                final Response<BlockBlobItem> response;
                try (final InputStream stream = file.getStream()) {
                    response = blobClient.uploadWithResponse(new BlobParallelUploadOptions(BinaryData.fromStream(stream)).setRequestConditions(blobRequestConditions),
                            null, Context.NONE);
                }

                FileInfo fileInfo = new FileInfo();
                fileInfo.setPath(data.getName());
                fileInfo.setRevision(response.getValue().getVersionId());
                commitFiles.add(fileInfo);
            }

            saveCommit(commit, path, newVersion);
            fileData = createFileData(destData.getName(), commit);
        } finally {
            unlock(path, newVersion);
        }

        onModified();

        return fileData;
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

    private Object getLatestRevision() {
        try {
            BlobClient client = blobContainerClient.getBlobClient(MODIFICATION_FILE);

            if (client.exists()) {
                return client.getProperties().getCreationTime().toInstant().toEpochMilli();
            }

            return null;
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return null;
        }
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

    private long lock(String path) throws IOException {
        do {
            long max = getLatestVersion(path);

            final long newVersion = max + 1;
            final BlobClient blobClient = blobContainerClient.getBlobClient(LOCKS_PREFIX + path + "/" + newVersion + "/lock");
            // TODO: save timestamp instead
            final String uuidStr = UUID.randomUUID().toString();
            final byte[] bytes = uuidStr.getBytes(StandardCharsets.UTF_8);
            ByteArrayInputStream is = new ByteArrayInputStream(bytes);
            // TODO: What if the lock exists already?
            blobClient.upload(is, bytes.length);
            try (final BlobInputStream inputStream = blobClient.openInputStream()) {
                final String lockContent = IOUtils.toStringAndClose(inputStream);
                if (lockContent.equals(uuidStr)) {
                    return newVersion;
                } else {
                    // TODO: Add check for locks that must be deleted
                }
            }
            // TODO: Add max iterations restriction
        } while (true);
    }

    private void unlock(String path, long version) {
        final BlobClient blobClient = blobContainerClient.getBlobClient(LOCKS_PREFIX + path + "/" +version + "/lock");
        if (blobClient.exists()) {
            blobClient.delete();
        } else {
            log.warn("Lock for path {} and version {} isn't found.", version, path);
        }
    }

    private long getLatestVersion(String path) {
        ListBlobsOptions options = new ListBlobsOptions().setPrefix(VERSIONS_PREFIX + path + VERSIONS_SEPARATOR);
        final PagedIterable<BlobItem> items = blobContainerClient.listBlobs(options, null);
        long max = 0L;
        for (BlobItem item : items) {
            Long ver = getVersion(item);
            if (ver == null) {
                continue;
            }
            if (max < ver) {
                max = ver;
            }
        }
        return max;
    }

    private Long getVersion(BlobItem item) {
        final String name = item.getName();
        if (!name.endsWith(VERSION_EXTENSION)) {
            return null;
        }
        final String verStr = name.substring(name.lastIndexOf('/') + 1, name.length() - VERSION_EXTENSION.length());
        return Long.parseLong(verStr);
    }

    private BlobItem getLatestBlob(Iterator<BlobItem> iterator) throws IOException {
        BlobItem latestBlob = null;
        long max = 0L;
        while (iterator.hasNext()) {
            BlobItem item = iterator.next();
            Long ver = getVersion(item);
            if (ver == null) {
                continue;
            }
            if (max < ver) {
                max = ver;
                latestBlob = item;
            }
        }
        if (latestBlob == null) {
            throw new IOException("Can't find file with version. Probably incorrect folder structure.");
        }
        return latestBlob;
    }

    private AzureCommit getCommit(String path, String version) {
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return getCommit(blobContainerClient.getBlobClient(VERSIONS_PREFIX + path + VERSIONS_SEPARATOR + version + VERSION_EXTENSION));
    }

    private AzureCommit getCommit(BlobItem item) {
        return getCommit(blobContainerClient.getBlobClient(item.getName()));
    }

    private AzureCommit getCommit(BlobClient client) {
        if (client == null) {
            return null;
        }
        synchronized (this) {
            final AzureCommit cached = commitsCache.get(client.getBlobName());
            if (cached != null) {
                return cached;
            }
        }

        try (InputStreamReader in = new InputStreamReader(client.openInputStream(), StandardCharsets.UTF_8)) {
            Yaml yaml = createYamlForCommit();
            final AzureCommit commit = yaml.loadAs(in, AzureCommit.class);
            synchronized (this) {
                commitsCache.put(client.getBlobName(), commit);
            }
            return commit;
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            return null;
        }
    }

    private void saveCommit(AzureCommit commit, String path, long version) throws IOException {
        final Yaml yaml = createYamlForCommit();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (OutputStreamWriter out = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            yaml.dump(commit, out);
        }

        final BlobClient blobClient = blobContainerClient.getBlobClient(VERSIONS_PREFIX + path + VERSIONS_SEPARATOR + version + VERSION_EXTENSION);
        blobClient.upload(BinaryData.fromBytes(outputStream.toByteArray()));
    }

    private Yaml createYamlForCommit() {
        TypeDescription projectsDescription = new TypeDescription(AzureCommit.class);
        projectsDescription.addPropertyParameters("files", FileInfo.class);
        Constructor constructor = new Constructor(AzureCommit.class);
        constructor.addTypeDescription(projectsDescription);
        Representer representer = new Representer();
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
        fileData.setVersion(String.valueOf(commit.getVersion()));
        fileData.setComment(commit.getComment());
        fileData.setAuthor(new UserInfo(commit.getAuthor()));
        fileData.setModifiedAt(commit.getModifiedAt());
        fileData.setDeleted(commit.isDeleted());
        return fileData;
    }

    private BlobClient findCommit(String name, String version) {
        // TODO: What if version is null?
        String commitName = name;
        if (!commitName.contains("/")) {
            return null;
        }

        String versionToSearch = version == null ? "1" : version;
        do {
            commitName = commitName.substring(0, commitName.lastIndexOf("/"));
            BlobClient client = blobContainerClient.getBlobClient(VERSIONS_PREFIX + commitName + VERSIONS_SEPARATOR + versionToSearch + VERSION_EXTENSION);
            if (client.exists()) {
                return client;
            }
        } while (commitName.contains("/"));

        return null;
    }

    private BlobClient findFile(String name, String version) {
        final AzureCommit commit = getCommit(findCommit(name, version));
        if (commit != null) {
            Optional<FileInfo> fileInfo = commit.getFiles().stream().filter(f -> f.getPath().equals(name)).findFirst();
            if (fileInfo.isPresent()) {
                BlobClient client = blobContainerClient.getBlobClient(CONTENT_PREFIX + fileInfo.get().getPath());
                if (client.exists()) {
                    client = client.getVersionClient(fileInfo.get().getRevision());
                } else {
                    return null;
                }

                if (client.exists()) {
                    return client;
                }
            }
        }

        return null;
    }
}