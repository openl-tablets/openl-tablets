package org.openl.rules.repository.aws;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openl.rules.repository.RRepositoryFactory;
import org.openl.rules.repository.api.*;
import org.openl.rules.repository.common.ChangesMonitor;
import org.openl.rules.repository.common.RevisionGetter;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;

public class S3Repository implements Repository, Closeable, RRepositoryFactory {
    private final Logger log = LoggerFactory.getLogger(S3Repository.class);

    private static final String MODIFICATION_FILE = ".openl-settings/.modification";

    private String bucketName;
    private String regionName;
    private String accessKey;
    private String secretKey;
    private int listenerTimerPeriod = 10;

    private AmazonS3 s3;
    private ChangesMonitor monitor;

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
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
    }

    @Override
    public void initialize() throws RRepositoryException {
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
        if (!StringUtils.isBlank(accessKey) && !StringUtils.isBlank(secretKey)) {
            builder.withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)));
        } else {
            builder.withCredentials(new DefaultAWSCredentialsProviderChain());
        }
        s3 = builder.withRegion(Regions.fromName(regionName)).build();

        try {
            if (!s3.doesBucketExistV2(bucketName)) {
                s3.createBucket(bucketName);
            }
            try {
                String status = s3.getBucketVersioningConfiguration(bucketName).getStatus();
                if (!BucketVersioningConfiguration.ENABLED.equals(status)) {
                    try {
                        s3.setBucketVersioningConfiguration(new SetBucketVersioningConfigurationRequest(bucketName,
                            new BucketVersioningConfiguration(BucketVersioningConfiguration.ENABLED)));
                    } catch (SdkClientException e) {
                        // Possibly don't have permission
                        String message = "Bucket versioning status: " + status + ". Cannot enable versioning. Error message: " + e
                            .getMessage();
                        log.warn(message);
                    }
                }
            } catch (SdkClientException e) {
                // Possibly don't have permission
                log.warn("Cannot detect bucket versioning configuration.");
            }

            // Check the connection
            s3.listObjectsV2(bucketName);
            list("");
            monitor = new ChangesMonitor(new S3RevisionGetter(), listenerTimerPeriod);
        } catch (SdkClientException | IOException e) {
            throw new RRepositoryException(e.getMessage(), e);
        }
    }

    @Override
    public List<FileData> list(String path) throws IOException {
        try {
            List<FileData> result = new ArrayList<>();

            VersionListing versionListing = null;

            do {
                versionListing = versionListing == null ? s3.listVersions(bucketName, path)
                                                        : s3.listNextBatchOfVersions(versionListing);
                List<S3VersionSummary> versionSummaries = versionListing.getVersionSummaries();
                if (versionSummaries.isEmpty()) {
                    return result;
                }
                for (S3VersionSummary versionSummary : versionSummaries) {
                    if (versionSummary.isLatest()) {
                        result.add(createFileData(versionSummary));
                    }
                }
            } while (versionListing.isTruncated());

            return result;
        } catch (SdkClientException e) {
            throw new IOException(e);
        }
    }

    private FileData createFileData(S3VersionSummary latest) {
        FileData data = latest.isDeleteMarker() ? new FileData() : new LazyFileData(s3, bucketName);
        data.setName(latest.getKey());
        data.setSize(latest.getSize());
        data.setModifiedAt(latest.getLastModified());
        data.setVersion(latest.getVersionId());
        data.setDeleted(latest.isDeleteMarker());
        return data;
    }

    @Override
    public FileData check(String name) throws IOException {
        try {
            VersionListing versionListing = null;

            S3VersionSummary latest = null;

            do {
                versionListing = versionListing == null ? s3.listVersions(bucketName, name)
                                                        : s3.listNextBatchOfVersions(versionListing);
                List<S3VersionSummary> versionSummaries = versionListing.getVersionSummaries();
                if (versionSummaries.isEmpty()) {
                    return null;
                }
                for (S3VersionSummary versionSummary : versionSummaries) {
                    if (versionSummary.getKey().equals(name) && versionSummary.isLatest()) {
                        latest = versionSummary;
                        break;
                    }
                }
            } while (versionListing.isTruncated());

            if (latest == null) {
                // Should never occur. But if occurred, get last version in the list
                List<S3VersionSummary> summaries = versionListing.getVersionSummaries();
                if (summaries.isEmpty()) {
                    return null;
                }
                latest = summaries.get(0);
            }

            return createFileData(latest);
        } catch (SdkClientException e) {
            throw new IOException(e);
        }
    }

    @Override
    public FileItem read(String name) throws IOException {
        try {
            FileData fileData = check(name);
            if (fileData == null) {
                return null;
            }
            InputStream objectContent = null;
            if (!fileData.isDeleted()) {
                S3Object object = s3.getObject(bucketName, name);
                objectContent = object.getObjectContent();
            }
            return objectContent == null ? null : new FileItem(fileData, objectContent);
        } catch (SdkClientException e) {
            throw new IOException(e);
        }
    }

    @Override
    public FileData save(FileData data, InputStream stream) throws IOException {
        try {
            ObjectMetadata metaData = createInsertFileMetadata(data);

            s3.putObject(bucketName, data.getName(), stream, metaData);

            onModified();

            return check(data.getName());
        } catch (SdkClientException e) {
            throw new IOException(e);
        }
    }

    private ObjectMetadata createInsertFileMetadata(FileData data) {
        ObjectMetadata metaData = new ObjectMetadata();
        metaData.setContentType("application/zip");
        if (data.getSize() != FileData.UNDEFINED_SIZE) {
            metaData.setContentLength(data.getSize());
        }
        if (!StringUtils.isBlank(data.getAuthor())) {
            metaData.addUserMetadata(LazyFileData.METADATA_AUTHOR, LazyFileData.encode(data.getAuthor()));
        }
        if (!StringUtils.isBlank(data.getComment())) {
            metaData.addUserMetadata(LazyFileData.METADATA_COMMENT, LazyFileData.encode(data.getComment()));
        }
        return metaData;
    }

    @Override
    public List<FileData> save(List<FileItem> fileItems) throws IOException {
        try {
            for (FileItem fileItem : fileItems) {
                FileData data = fileItem.getData();
                ObjectMetadata metaData = createInsertFileMetadata(data);
                s3.putObject(bucketName, data.getName(), fileItem.getStream(), metaData);
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
            s3.deleteObject(bucketName, data.getName());
            onModified();
            return true;
        } catch (SdkClientException e) {
            log.error(e.getMessage(), e);
            throw new IOException(e.getMessage(), e);
        }
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
            FileData fileData;
            try {
                fileData = check(MODIFICATION_FILE);
                return fileData == null ? null : fileData.getVersion();
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
            VersionListing versionListing = null;

            do {
                versionListing = versionListing == null ? s3.listVersions(bucketName, name)
                                                        : s3.listNextBatchOfVersions(versionListing);
                for (S3VersionSummary versionSummary : versionListing.getVersionSummaries()) {
                    if (versionSummary.getKey().equals(name)) {
                        result.add(createFileData(versionSummary));
                    }
                }
            } while (versionListing.isTruncated());

            // OpenL expects that the last element in the list is last version.
            // AWS S3 returns last version first and older versions later.
            // So we reverse result to convert AWS S3 order to OpenL.
            Collections.reverse(result);

            return result;
        } catch (SdkClientException e) {
            throw new IOException(e);
        }
    }

    @Override
    public FileData checkHistory(String name, String version) throws IOException {
        try {
            VersionListing versionListing = null;

            do {
                versionListing = versionListing == null ? s3.listVersions(bucketName, name)
                                                        : s3.listNextBatchOfVersions(versionListing);
                for (S3VersionSummary versionSummary : versionListing.getVersionSummaries()) {
                    if (versionSummary.getKey().equals(name) && versionSummary.getVersionId().equals(version)) {
                        return createFileData(versionSummary);
                    }
                }
            } while (versionListing.isTruncated());

            return null;
        } catch (SdkClientException e) {
            throw new IOException(e);
        }
    }

    @Override
    public FileItem readHistory(String name, String version) throws IOException {
        try {
            VersionListing versionListing = null;

            do {
                versionListing = versionListing == null ? s3.listVersions(bucketName, name)
                                                        : s3.listNextBatchOfVersions(versionListing);
                for (S3VersionSummary versionSummary : versionListing.getVersionSummaries()) {
                    if (versionSummary.getKey().equals(name) && versionSummary.getVersionId().equals(version)) {
                        InputStream content = null;
                        if (!versionSummary.isDeleteMarker()) {
                            S3Object object = s3.getObject(new GetObjectRequest(bucketName, name, version));
                            content = object.getObjectContent();
                        }
                        return content == null ? null : new FileItem(checkHistory(name, version), content);
                    }
                }
            } while (versionListing.isTruncated());

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

                onModified();
                return true;
            } else {
                s3.deleteVersion(bucketName, name, version);
                onModified();
                return true;
            }
        } catch (SdkClientException e) {
            log.error(e.getMessage(), e);
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public FileData copyHistory(String srcName, FileData destData, String version) throws IOException {
        try {
            CopyObjectRequest request = new CopyObjectRequest(bucketName,
                srcName,
                version,
                bucketName,
                destData.getName());
            CopyObjectResult result = s3.copyObject(request);

            onModified();

            return checkHistory(destData.getName(), result.getVersionId());
        } catch (SdkClientException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Features supports() {
        return new FeaturesBuilder(this).build();
    }

    private void onModified() {
        // Delete previous versions of modification marker file
        deleteAllVersions(MODIFICATION_FILE);

        // Create new version of modification marker file with new id
        ObjectMetadata metaData = new ObjectMetadata();
        metaData.setContentLength(0);
        s3.putObject(bucketName, MODIFICATION_FILE, new ByteArrayInputStream(new byte[0]), metaData);

        // Invoke listener if exist
        if (monitor != null) {
            monitor.fireOnChange();
        }
    }

    private void deleteAllVersions(String name) {
        VersionListing versionListing = null;

        do {
            versionListing = versionListing == null ? s3.listVersions(bucketName, name)
                                                    : s3.listNextBatchOfVersions(versionListing);
            for (S3VersionSummary versionSummary : versionListing.getVersionSummaries()) {
                if (versionSummary.getKey().equals(name)) {
                    s3.deleteVersion(bucketName, name, versionSummary.getVersionId());
                }
            }
        } while (versionListing.isTruncated());
    }
}
