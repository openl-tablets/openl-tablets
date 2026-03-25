package org.openl.itest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import com.adobe.testing.s3mock.testcontainers.S3MockContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketVersioningStatus;

public class AbstractMinioTest {

    private static final S3MockContainer S3_CONTAINER = new S3MockContainer("latest");

    protected static S3Client s3Client;

    protected Map<String, String> config;
    protected String bucketName;

    @BeforeAll
    public static void initialize() {
        S3_CONTAINER.start();

        s3Client = S3Client.builder()
                .endpointOverride(URI.create(S3_CONTAINER.getHttpEndpoint()))
                .region(Region.US_EAST_1)
                .forcePathStyle(true)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("access key", "secret key")
                ))
                .build();

    }

    @AfterAll
    public static void destroy() {
        S3_CONTAINER.stop();
    }

    @BeforeEach
    public void setUp() {
        bucketName = "openl-test-" + System.currentTimeMillis();
        config = new HashMap<>();
        config.put("production-repository.factory", "repo-aws-s3");
        config.put("production-repository.region-name", "us-east-1");
        config.put("production-repository.bucket-name", bucketName);
        config.put("production-repository.service-endpoint", S3_CONTAINER.getHttpEndpoint());
        config.put("production-repository.access-key", "access key");
        config.put("production-repository.secret-key", "secret key");
        config.put("production-repository.base.path", "deploy/");
    }

    protected void verifyS3Repository() throws Exception {
        assertNotNull(s3Client.headBucket(it -> it.bucket(bucketName))); // Check that bucket exists
        assertEquals(BucketVersioningStatus.ENABLED, s3Client.getBucketVersioning(it -> it.bucket(bucketName)).status());
    }

}
