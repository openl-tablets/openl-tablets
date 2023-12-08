package org.openl.rules.repository.aws;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.repository.common.ChangesMonitor;
import org.openl.rules.repository.common.RevisionGetter;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketVersioningStatus;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteMarkerEntry;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.ObjectVersion;
import software.amazon.awssdk.services.s3.model.PutBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.VersioningConfiguration;

public class S3Repository implements Repository, Closeable {
    private final Logger log = LoggerFactory.getLogger(S3Repository.class);

    private static final String MODIFICATION_FILE = ".openl-settings/.modification";

    private static final Comparator<FileData> FILE_DATA_COMPARATOR = Comparator.comparing(FileData::getModifiedAt);

    private String serviceEndpoint;
    private String bucketName;
    private String regionName;
    private String accessKey;
    private String secretKey;
    private String sseAlgorithm;
    private int listenerTimerPeriod = 10;

    private S3Client s3;
    private ChangesMonitor monitor;
    private String id;
    private String name;

    public void setServiceEndpoint(String serviceEndpoint) {
        this.serviceEndpoint = serviceEndpoint;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getSseAlgorithm() {
        return sseAlgorithm;
    }

    public void setSseAlgorithm(String sseAlgorithm) {
        this.sseAlgorithm = sseAlgorithm;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void setListenerTimerPeriod(int listenerTimerPeriod) {
        this.listenerTimerPeriod = listenerTimerPeriod;
    }

    @Override
    public void close() {
        if (monitor != null) {
            monitor.release();
            monitor = null;
        }
        if (s3 != null) {
            s3.close();
            s3 = null;
        }
    }

    public void initialize() {
        var builder = S3Client.builder();
        if (!StringUtils.isBlank(serviceEndpoint)) {
            builder.endpointOverride(URI.create(serviceEndpoint)).region(Region.of(regionName));
        } else {
            builder.region(Region.of(regionName));
        }
        if (!StringUtils.isBlank(accessKey) && !StringUtils.isBlank(secretKey)) {
            builder.credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        s3 = builder.build();

        try {
            try {
                s3.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            } catch (NoSuchBucketException e) {
                log.debug(e.getMessage(), e);
                // If the bucket does not exist, create it
                s3.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
            }
            try {
                var verResp = s3.getBucketVersioning(GetBucketVersioningRequest.builder().bucket(bucketName).build());
                if (BucketVersioningStatus.ENABLED != verResp.status()) {
                    try {
                        s3.putBucketVersioning(PutBucketVersioningRequest.builder()
                            .bucket(bucketName)
                            .versioningConfiguration(
                                VersioningConfiguration.builder().status(BucketVersioningStatus.ENABLED).build())
                            .build());
                    } catch (S3Exception | SdkClientException e) {
                        // Possibly don't have permission
                        log.warn("Bucket versioning status: {}. Cannot enable versioning. Error message: {}",
                            verResp.status(),
                            e.getMessage());
                    }
                }
            } catch (S3Exception | SdkClientException e) {
                // Possibly don't have permission
                log.warn("Cannot detect bucket versioning configuration: {}.", e.getMessage());
            }
        } catch (SdkClientException e) {
            log.warn("Failed to initialize a repository", e);
        }

        monitor = new ChangesMonitor(new S3RevisionGetter(), listenerTimerPeriod);
    }

    @Override
    public void validateConnection() throws IOException {
        try {
            // Check the connection
            s3.listObjectsV2(ListObjectsV2Request.builder().bucket(bucketName).build());
        } catch (Exception e) {
            throw new IOException(e);
        }
        try {
            var verResp = s3.getBucketVersioning(GetBucketVersioningRequest.builder().bucket(bucketName).build());
            if (!BucketVersioningStatus.ENABLED.equals(verResp.status())) {
                throw new IOException("Versioning for the bucket is not enabled");
            }
        } catch (S3Exception | SdkClientException e) {
            // Possibly don't have permission
            log.warn("Cannot detect bucket versioning configuration.", e);
        }
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<FileData> list(String path) throws IOException {
        try {
            List<FileData> result = new ArrayList<>();
            var request = ListObjectVersionsRequest.builder().bucket(bucketName).prefix(path);
            do {
                var response = s3.listObjectVersions(request.build());
                var versionSummaries = response.versions();
                for (var versionSummary : versionSummaries) {
                    if (versionSummary.isLatest()) {
                        result.add(createFileData(versionSummary));
                    }
                }
                for (var deleteMarkers : response.deleteMarkers()) {
                    if (deleteMarkers.isLatest()) {
                        result.add(createFileData(deleteMarkers));
                    }
                }

                if (response.isTruncated()) {
                    request.keyMarker(response.nextKeyMarker());
                    request.versionIdMarker(response.nextVersionIdMarker());
                } else {
                    request = null;
                }
            } while (request != null);

            return result;
        } catch (SdkClientException e) {
            throw new IOException(e);
        }
    }

    private FileData createFileData(ObjectVersion latest) {
        var data = new LazyFileData(s3, bucketName);
        data.setName(latest.key());
        data.setSize(latest.size());
        data.setModifiedAt(Date.from(latest.lastModified()));
        data.setVersion(latest.versionId());
        return data;
    }

    private FileData createFileData(DeleteMarkerEntry latest) {
        var data = new FileData();
        data.setName(latest.key());
        data.setModifiedAt(Date.from(latest.lastModified()));
        data.setVersion(latest.versionId());
        data.setDeleted(true);
        return data;
    }

    @Override
    public FileData check(String name) throws IOException {
        try {
            var request = ListObjectVersionsRequest.builder().bucket(bucketName).prefix(name);
            do {
                var response = s3.listObjectVersions(request.build());
                var versionSummaries = response.versions();
                for (var versionSummary : versionSummaries) {
                    if (versionSummary.isLatest() && Objects.equals(versionSummary.key(), name)) {
                        return createFileData(versionSummary);
                    }
                }
                for (var deleteMarker : response.deleteMarkers()) {
                    if (deleteMarker.isLatest() && Objects.equals(deleteMarker.key(), name)) {
                        return createFileData(deleteMarker);
                    }
                }
                if (response.isTruncated()) {
                    request.keyMarker(response.nextKeyMarker());
                    request.versionIdMarker(response.nextVersionIdMarker());
                } else {
                    request = null;
                }
            } while (request != null);

            return null;
        } catch (SdkClientException e) {
            throw new IOException(e);
        }
    }

    @Override
    public FileItem read(String name) throws IOException {
        try {
            var fileData = check(name);
            if (fileData == null || fileData.isDeleted()) {
                return null;
            }
            return new FileItem(fileData, new DrainableInputStream(doRead(name, null)));
        } catch (SdkClientException e) {
            throw new IOException(e);
        }
    }

    private InputStream doRead(String name, String versionId) {
        var request = GetObjectRequest.builder().bucket(bucketName).key(name).versionId(versionId).build();
        return s3.getObject(request);
    }

    @Override
    public FileData save(FileData data, InputStream stream) throws IOException {
        try {
            doSave(data, stream);
            onModified();
            return check(data.getName());
        } catch (SdkClientException e) {
            throw new IOException(e);
        }
    }

    private void doSave(FileData data, InputStream stream) {
        var request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(data.getName())
            .metadata(createInsertFileMetadata(data))
            .build();

        s3.putObject(request, RequestBody.fromInputStream(stream, data.getSize()));
    }

    private Map<String, String> createInsertFileMetadata(FileData data) {
        Map<String, String> userMetadata = new HashMap<>();

        userMetadata.put("Content-Type", "application/zip");

        if (!StringUtils.isBlank(sseAlgorithm)) {
            userMetadata.put("sseAlgorithm", sseAlgorithm);
        }

        String username = Optional.ofNullable(data.getAuthor()).map(UserInfo::getUsername).orElse(null);
        if (!StringUtils.isBlank(username)) {
            userMetadata.put(LazyFileData.METADATA_AUTHOR, LazyFileData.encode(username));
        }

        if (!StringUtils.isBlank(data.getComment())) {
            userMetadata.put(LazyFileData.METADATA_COMMENT, LazyFileData.encode(data.getComment()));
        }

        return userMetadata;
    }

    @Override
    public List<FileData> save(List<FileItem> fileItems) throws IOException {
        try {
            for (FileItem fileItem : fileItems) {
                doSave(fileItem.getData(), fileItem.getStream());
            }
        } catch (SdkClientException e) {
            throw new IOException(e);
        }
        onModified();
        List<FileData> result = new ArrayList<>();
        for (FileItem fileItem : fileItems) {
            result.add(check(fileItem.getData().getName()));
        }
        return result;
    }

    @Override
    public boolean delete(FileData data) throws IOException {
        try {
            var request = DeleteObjectRequest.builder().bucket(bucketName).key(data.getName()).build();
            s3.deleteObject(request);
            onModified();
            return true;
        } catch (SdkClientException e) {
            log.error(e.getMessage(), e);
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(List<FileData> data) throws IOException {
        if (data.isEmpty()) {
            return false;
        }
        for (FileData f : data) {
            try {
                var request = DeleteObjectRequest.builder().bucket(bucketName).key(f.getName()).build();
                s3.deleteObject(request);
            } catch (SdkClientException e) {
                log.error(e.getMessage(), e);
                throw new IOException(e.getMessage(), e);
            }
        }
        onModified();
        return true;
    }

    @Override
    public void setListener(final Listener callback) {
        if (monitor != null) {
            monitor.setListener(callback);
        }
    }

    private class S3RevisionGetter implements RevisionGetter {

        @Override
        public Object getRevision() {
            try {
                var fileData = check(MODIFICATION_FILE);
                if (fileData == null) {
                    log.warn("Failed to detect changes. '{}' is not found.", MODIFICATION_FILE);
                    return null;
                }
                String version = fileData.getVersion();
                Object revision = version;
                if (StringUtils.isBlank(version) || "null".equalsIgnoreCase(version)) {
                    revision = fileData.getModifiedAt();
                }
                return revision;
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
                return null;
            }
        }
    }

    @Override
    public List<FileData> listHistory(String name) throws IOException {
        try {
            List<FileData> result = new ArrayList<>();

            var request = ListObjectVersionsRequest.builder().bucket(bucketName).prefix(name);
            do {
                var response = s3.listObjectVersions(request.build());
                for (ObjectVersion version : response.versions()) {
                    if (version.key().equals(name)) {
                        result.add(createFileData(version));
                    }
                }
                for (DeleteMarkerEntry deleteMarkers : response.deleteMarkers()) {
                    if (deleteMarkers.key().equals(name)) {
                        result.add(createFileData(deleteMarkers));
                    }
                }
                if (response.isTruncated()) {
                    request.keyMarker(response.nextKeyMarker());
                    request.versionIdMarker(response.nextVersionIdMarker());
                } else {
                    request = null;
                }
            } while (request != null);

            result.sort(FILE_DATA_COMPARATOR);
            return result;
        } catch (SdkClientException e) {
            throw new IOException(e);
        }
    }

    @Override
    public FileData checkHistory(String name, String version) throws IOException {
        try {
            var request = ListObjectVersionsRequest.builder().bucket(bucketName).prefix(name);
            do {
                var response = s3.listObjectVersions(request.build());
                for (ObjectVersion versionSummary : response.versions()) {
                    if (versionSummary.key().equals(name) && versionSummary.versionId().equals(version)) {
                        return createFileData(versionSummary);
                    }
                }
                for (DeleteMarkerEntry deleteMarkers : response.deleteMarkers()) {
                    if (deleteMarkers.key().equals(name) && deleteMarkers.versionId().equals(version)) {
                        return createFileData(deleteMarkers);
                    }
                }
                if (response.isTruncated()) {
                    request.keyMarker(response.nextKeyMarker());
                    request.versionIdMarker(response.nextVersionIdMarker());
                } else {
                    request = null;
                }
            } while (request != null);

            return null;
        } catch (SdkClientException e) {
            throw new IOException(e);
        }
    }

    @Override
    public FileItem readHistory(String name, String version) throws IOException {
        try {
            var request = ListObjectVersionsRequest.builder().bucket(bucketName).prefix(name);
            do {
                var response = s3.listObjectVersions(request.build());
                for (var versionSummary : response.versions()) {
                    if (versionSummary.key().equals(name) && versionSummary.versionId().equals(version)) {
                        return new FileItem(checkHistory(name, version),
                            new DrainableInputStream(doRead(name, version)));
                    }
                }
                if (response.isTruncated()) {
                    request.keyMarker(response.nextKeyMarker());
                    request.versionIdMarker(response.nextVersionIdMarker());
                } else {
                    request = null;
                }
            } while (request != null);

            return null;
        } catch (SdkClientException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean deleteHistory(FileData data) throws IOException {
        String name = data.getName();
        String version = data.getVersion();

        try {
            if (version == null) {
                deleteAllVersions(name);
            } else {
                var request = DeleteObjectRequest.builder().bucket(bucketName).key(name).versionId(version).build();
                s3.deleteObject(request);
            }
            onModified();
            return true;
        } catch (SdkClientException e) {
            log.error(e.getMessage(), e);
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public FileData copyHistory(String srcName, FileData destData, String version) throws IOException {
        try {
            var request = CopyObjectRequest.builder()
                .sourceBucket(bucketName)
                .sourceKey(srcName)
                .sourceVersionId(version)
                .destinationBucket(bucketName)
                .destinationKey(destData.getName())
                .build();

            var response = s3.copyObject(request);
            onModified();
            return checkHistory(destData.getName(), response.versionId());
        } catch (SdkClientException e) {
            throw new IOException(e);
        }
    }

    @Override
    public List<FileData> listFolders(String path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<FileData> listFiles(String path, String version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileData save(FileData folderData, Iterable<FileItem> files, ChangesetType changesetType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Features supports() {
        return new FeaturesBuilder(this).build();
    }

    private void onModified() {
        // Delete previous versions of modification marker file
        deleteAllVersions(MODIFICATION_FILE);

        // Create new version of modification marker file with new id
        var metadataMap = new HashMap<String, String>();
        if (StringUtils.isNotBlank(sseAlgorithm)) {
            metadataMap.put("sseAlgorithm", sseAlgorithm);
        }

        var request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(MODIFICATION_FILE)
            .metadata(metadataMap)
            .build();
        s3.putObject(request, RequestBody.empty());

        // Invoke listener if exist
        if (monitor != null) {
            monitor.fireOnChange();
        }
    }

    private void deleteAllVersions(String name) {
        var listVersionsRequest = ListObjectVersionsRequest.builder().bucket(bucketName).prefix(name);
        do {
            var response = s3.listObjectVersions(listVersionsRequest.build());

            var versions = new ArrayList<ObjectIdentifier>();
            for (var version : response.versions()) {
                if (version.key().equals(name)) {
                    versions.add(ObjectIdentifier.builder().key(name).versionId(version.versionId()).build());
                }
            }
            for (var deleteMarker : response.deleteMarkers()) {
                if (deleteMarker.key().equals(name)) {
                    versions.add(ObjectIdentifier.builder().key(name).versionId(deleteMarker.versionId()).build());
                }
            }
            if (!versions.isEmpty()) {
                var batchDeleteRequest = DeleteObjectsRequest.builder()
                    .bucket(bucketName)
                    .delete(Delete.builder().objects(versions).build())
                    .build();
                s3.deleteObjects(batchDeleteRequest);
            }

            if (response.isTruncated()) {
                listVersionsRequest.keyMarker(response.nextKeyMarker());
                listVersionsRequest.versionIdMarker(response.nextVersionIdMarker());
            } else {
                listVersionsRequest = null;
            }
        } while (listVersionsRequest != null);
    }
}
