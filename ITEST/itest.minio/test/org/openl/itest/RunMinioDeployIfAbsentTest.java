package org.openl.itest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.minio.MakeBucketArgs;
import io.minio.StatObjectArgs;
import okio.Path;
import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;
import org.openl.rules.ruleservice.deployer.RulesDeployerService;

public class RunMinioDeployIfAbsentTest extends AbstractMinioTest {

    @Test
    public void testWhenDeployJarsIfAbsent() throws Exception {
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        try (var deployer = new RulesDeployerService(config::get)) {
            deployer.deploy(Path.get("test-resources/openl/multiple-deployment-datasource.zip").toFile(), false);
        }
        verifyS3Repository();
        final var beforeStartProject1 = minioClient.statObject(StatObjectArgs.builder()
                .bucket(bucketName)
                .object("deploy/multiple-deployment-datasource/project1")
                .build());
        final var beforeStartProject2 = minioClient.statObject(StatObjectArgs.builder()
                .bucket(bucketName)
                .object("deploy/multiple-deployment-datasource/project2")
                .build());

        try (var client = JettyServer.get()
                .withInitParam(config)
                .withInitParam("ruleservice.datasource.deploy.classpath.jars", "IF_ABSENT")
                .start()) {
            client.test("test-resources-smoke/stage1");

            // verify that projects are not redeployed
            var actualProject1 = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object("deploy/multiple-deployment-datasource/project1")
                    .build());
            assertTrue(beforeStartProject1.lastModified().isEqual(actualProject1.lastModified()));
            assertEquals(beforeStartProject1.versionId(), actualProject1.versionId());

            var actualProject2 = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object("deploy/multiple-deployment-datasource/project2")
                    .build());
            assertTrue(beforeStartProject2.lastModified().isEqual(actualProject2.lastModified()));
            assertEquals(beforeStartProject2.versionId(), actualProject2.versionId());
        }
    }

}
