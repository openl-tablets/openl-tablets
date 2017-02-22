package org.openl.rules.repository.aws;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import org.openl.rules.repository.RRepositoryFactory;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3Repository implements Repository, Closeable, RRepositoryFactory {
    private final Logger log = LoggerFactory.getLogger(S3Repository.class);

    private static final String MODIFICATION_FILE = ".openl-settings/.modification";

    private String bucketName;
    private String regionName;
    private String accessKey;
    private String secretKey;
    private Long listenerTimerPeriod = 10000L;

    private AmazonS3 s3;
    private Listener listener;
    private Timer timer;

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

    public void setListenerTimerPeriod(Long listenerTimerPeriod) {
        this.listenerTimerPeriod = listenerTimerPeriod;
    }

    @Override
    public void close() throws IOException {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    public void initialize() throws RRepositoryException {
        s3 = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
                .withRegion(Regions.fromName(regionName))
                .build();

        try {
            if (!s3.doesBucketExist(bucketName)) {
                s3.createBucket(bucketName);
                s3.setBucketVersioningConfiguration(new SetBucketVersioningConfigurationRequest(bucketName,
                        new BucketVersioningConfiguration(BucketVersioningConfiguration.ENABLED)));
            }

            // Check the connection
            s3.listObjectsV2(bucketName);
        } catch (SdkClientException e) {
            throw new RRepositoryException(e.getMessage(), e);
        }
    }

    @Override
    public List<FileData> list(String path) throws IOException {
        try {
            List<FileData> result = new ArrayList<FileData>();

            VersionListing versionListing = null;

            do {
                versionListing = versionListing == null ?
                                 s3.listVersions(bucketName, path) :
                                 s3.listNextBatchOfVersions(versionListing);
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
                versionListing = versionListing == null ?
                                 s3.listVersions(bucketName, name) :
                                 s3.listNextBatchOfVersions(versionListing);
                List<S3VersionSummary> versionSummaries = versionListing.getVersionSummaries();
                if (versionSummaries.isEmpty()) {
                    return null;
                }
                for (S3VersionSummary versionSummary : versionSummaries) {
                    if (versionSummary.isLatest()) {
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
            return new FileItem(fileData, objectContent);
        } catch (SdkClientException e) {
            throw new IOException(e);
        }
    }

    @Override
    public FileData save(FileData data, InputStream stream) throws IOException {
        try {
            if (!data.isDeleted()) {
                FileData existing = check(data.getName());

                if (existing != null && existing.isDeleted()) {
                    // This is undelete operation
                    deleteHistory(data.getName(), existing.getVersion());
                    return check(data.getName());
                }
            }

            ObjectMetadata metaData = new ObjectMetadata();
            metaData.setContentType("application/zip");
            if (data.getSize() != 0) {
                metaData.setContentLength(data.getSize());
            }
            if (!StringUtils.isBlank(data.getAuthor())) {
                metaData.addUserMetadata(LazyFileData.METADATA_AUTHOR, LazyFileData.encode(data.getAuthor()));
            }
            if (!StringUtils.isBlank(data.getComment())) {
                metaData.addUserMetadata(LazyFileData.METADATA_COMMENT, LazyFileData.encode(data.getComment()));
            }

            s3.putObject(bucketName, data.getName(), stream, metaData);

            onModified();

            return check(data.getName());
        } catch (SdkClientException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean delete(FileData data) {
        try {
            s3.deleteObject(bucketName, data.getName());
            onModified();
            return true;
        } catch (SdkClientException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public FileData copy(String srcName, FileData destData) throws IOException {
        try {
            s3.copyObject(bucketName, srcName, bucketName, destData.getName());

            onModified();

            return check(destData.getName());
        } catch (SdkClientException e) {
            throw new IOException(e);
        }
    }

    @Override
    public FileData rename(String srcName, FileData destData) throws IOException {
        copy(srcName, destData);
        deleteHistory(srcName, null);

        onModified();

        return check(destData.getName());
    }

    @Override
    public void setListener(final Listener callback) {
        this.listener = callback;

        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        if (callback != null) {
            timer = new Timer(true);

            timer.schedule(new TimerTask() {
                private String lastChange = getLastChange();

                @Override
                public void run() {
                    String currentChange = getLastChange();
                    if (currentChange == null) {
                        // Ignore unknown changes
                        return;
                    }
                    if (currentChange.equals(lastChange)) {
                        // Ignore no changes
                        return;
                    }
                    lastChange = currentChange;
                    callback.onChange();
                }
            }, listenerTimerPeriod, listenerTimerPeriod);
        }
    }

    private String getLastChange() {
        FileData fileData;
        try {
            fileData = check(MODIFICATION_FILE);
            return fileData == null ? null : fileData.getVersion();
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public List<FileData> listHistory(String name) throws IOException {
        try {
            List<FileData> result = new ArrayList<FileData>();
            VersionListing versionListing = null;

            do {
                versionListing = versionListing == null ?
                                 s3.listVersions(bucketName, name) :
                                 s3.listNextBatchOfVersions(versionListing);
                for (S3VersionSummary versionSummary : versionListing.getVersionSummaries()) {
                    result.add(createFileData(versionSummary));
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

            S3VersionSummary summary = null;
            do {
                versionListing = versionListing == null ?
                                 s3.listVersions(bucketName, name) :
                                 s3.listNextBatchOfVersions(versionListing);
                for (S3VersionSummary versionSummary : versionListing.getVersionSummaries()) {
                    if (versionSummary.getVersionId().equals(version)) {
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

            S3VersionSummary summary = null;
            do {
                versionListing = versionListing == null ?
                                 s3.listVersions(bucketName, name) :
                                 s3.listNextBatchOfVersions(versionListing);
                for (S3VersionSummary versionSummary : versionListing.getVersionSummaries()) {
                    if (versionSummary.getVersionId().equals(version)) {
                        InputStream content = null;
                        if (!versionSummary.isDeleteMarker()) {
                            S3Object object = s3.getObject(new GetObjectRequest(bucketName, name, version));
                            content = object.getObjectContent();
                        }
                        return new FileItem(checkHistory(name, version), content);
                    }
                }
            } while (versionListing.isTruncated());

            return null;
        } catch (SdkClientException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean deleteHistory(String name, String version) {
        try {
            if (version == null) {
                VersionListing versionListing = null;

                do {
                    versionListing = versionListing == null ?
                                     s3.listVersions(bucketName, name) :
                                     s3.listNextBatchOfVersions(versionListing);
                    for (S3VersionSummary versionSummary : versionListing.getVersionSummaries()) {
                        s3.deleteVersion(bucketName, name, versionSummary.getVersionId());
                    }
                } while (versionListing.isTruncated());

                onModified();
                return true;
            } else {
                s3.deleteVersion(bucketName, name, version);
                onModified();
                return true;
            }
        } catch (SdkClientException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public FileData copyHistory(String srcName, FileData destData, String version) throws IOException {
        try {
            CopyObjectRequest request = new CopyObjectRequest(bucketName, srcName, version, bucketName, destData.getName());
            CopyObjectResult result = s3.copyObject(request);

            onModified();

            return checkHistory(destData.getName(), result.getVersionId());
        } catch (SdkClientException e) {
            throw new IOException(e);
        }
    }

    private void onModified() {
        // Delete previous versions of modification marker file
        VersionListing versionListing = null;

        do {
            versionListing = versionListing == null ?
                             s3.listVersions(bucketName, MODIFICATION_FILE) :
                             s3.listNextBatchOfVersions(versionListing);
            for (S3VersionSummary versionSummary : versionListing.getVersionSummaries()) {
                s3.deleteVersion(bucketName, MODIFICATION_FILE, versionSummary.getVersionId());
            }
        } while (versionListing.isTruncated());

        // Create new version of modification marker file with new id
        ObjectMetadata metaData = new ObjectMetadata();
        metaData.setContentLength(0);
        s3.putObject(bucketName, MODIFICATION_FILE, new ByteArrayInputStream(new byte[0]), metaData);

        // Invoke listener if exist
        if (listener != null) {
            listener.onChange();
        }
    }
}
