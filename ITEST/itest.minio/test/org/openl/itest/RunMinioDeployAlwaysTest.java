package org.openl.itest;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;
import org.openl.rules.ruleservice.deployer.RulesDeployerService;

public class RunMinioDeployAlwaysTest extends AbstractMinioTest {

    @Test
    public void testWhenDeployJarsAlways() throws Exception {
        try (var deployer = new RulesDeployerService(config::get)) {
            deployer.deploy(Paths.get("test-resources/openl/multiple-deployment-datasource.zip").toFile(), false);
        }
        verifyS3Repository();
        final var beforeStartProject1 = s3Client.headObject(it -> it.bucket(bucketName).key("deploy/multiple-deployment-datasource/project1"));
        final var beforeStartProject2 = s3Client.headObject(it -> it.bucket(bucketName).key("deploy/multiple-deployment-datasource/project2"));

        try (var client = JettyServer.get()
                .withInitParam(config)
                .withInitParam("ruleservice.datasource.deploy.classpath.jars", "ALWAYS")
                .start()) {
            client.test("test-resources-smoke/stage1");

            // verify that projects are redeployed
            final var actualProject1 = s3Client.headObject(it -> it.bucket(bucketName).key("deploy/multiple-deployment-datasource/project1"));
            assertTrue(beforeStartProject1.lastModified().isBefore(actualProject1.lastModified()));
            assertNotEquals(beforeStartProject1.versionId(), actualProject1.versionId());

            final var actualProject2 = s3Client.headObject(it -> it.bucket(bucketName).key("deploy/multiple-deployment-datasource/project2"));
            assertTrue(beforeStartProject2.lastModified().isBefore(actualProject2.lastModified()));
            assertNotEquals(beforeStartProject2.versionId(), actualProject2.versionId());
        }
    }

}
