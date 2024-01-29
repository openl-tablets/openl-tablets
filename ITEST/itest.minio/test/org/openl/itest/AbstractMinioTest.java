package org.openl.itest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.minio.BucketExistsArgs;
import io.minio.GetBucketVersioningArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.messages.VersioningConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class AbstractMinioTest {

    private static final MinIOContainer MINIO_CONTAINER = new MinIOContainer("minio/minio") {
        @Override
        public void configure() {
            super.configure();
            waitingFor(Wait.defaultWaitStrategy());
        }
    };

    protected static MinioClient minioClient;

    protected Map<String, String> config;
    protected String bucketName;

    @BeforeAll
    public static void initialize() {
        MINIO_CONTAINER.start();

        minioClient = MinioClient.builder()
                .endpoint(MINIO_CONTAINER.getS3URL())
                .credentials(MINIO_CONTAINER.getUserName(), MINIO_CONTAINER.getPassword())
                .build();

    }

    @AfterAll
    public static void destroy() {
        MINIO_CONTAINER.stop();
    }

    @BeforeEach
    public void setUp() {
        bucketName = "openl-test-" + System.currentTimeMillis();
        config = new HashMap<>();
        config.put("production-repository.factory", "repo-aws-s3");
        config.put("production-repository.region-name", "us-east-1");
        config.put("production-repository.bucket-name", bucketName);
        config.put("production-repository.service-endpoint", MINIO_CONTAINER.getS3URL());
        config.put("production-repository.access-key", MINIO_CONTAINER.getUserName());
        config.put("production-repository.secret-key", MINIO_CONTAINER.getPassword());
        config.put("production-repository.base.path", "deploy/");
    }

    protected void verifyS3Repository() throws Exception {
        assertTrue(minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build()));
        assertEquals(VersioningConfiguration.Status.ENABLED,
                minioClient.getBucketVersioning(GetBucketVersioningArgs.builder().bucket(bucketName).build()).status());
    }

    protected void assertDeployedServices(String... services) throws Exception {
        var objects = minioClient
                .listObjects(ListObjectsArgs.builder().bucket(bucketName).prefix("deploy").recursive(true).build())
                .iterator();
        var actualObjects = new ArrayList<String>();
        while (objects.hasNext()) {
            var object = objects.next().get();
            actualObjects.add(object.objectName());
        }
        assertEquals(Set.of(services), new HashSet<>(actualObjects));
    }

}
