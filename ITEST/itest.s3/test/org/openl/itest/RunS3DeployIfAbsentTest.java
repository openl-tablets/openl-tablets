package org.openl.itest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;
import org.openl.rules.ruleservice.deployer.RulesDeployerService;

public class RunS3DeployIfAbsentTest extends AbstractS3Test {

    @Test
    public void testWhenDeployJarsIfAbsent() throws Exception {
        try (var deployer = new RulesDeployerService(config::get)) {
            deployer.deploy(Paths.get("test-resources/openl/multiple-deployment-datasource.zip").toFile(), false);
        }
        verifyS3Repository();
        final var beforeStartProject1 = s3Client.headObject(it -> it.bucket(bucketName).key("deploy/multiple-deployment-datasource/project1"));
        final var beforeStartProject2 = s3Client.headObject(it -> it.bucket(bucketName).key("deploy/multiple-deployment-datasource/project2"));

        try (var client = JettyServer.get()
                .withInitParam(config)
                .withInitParam("ruleservice.datasource.deploy.classpath.jars", "IF_ABSENT")
                .start()) {
            client.test("test-resources-smoke/stage1");

            // verify that projects are not redeployed
            final var actualProject1 = s3Client.headObject(it -> it.bucket(bucketName).key("deploy/multiple-deployment-datasource/project1"));
            assertEquals(beforeStartProject1.lastModified(), actualProject1.lastModified());
            assertEquals(beforeStartProject1.versionId(), actualProject1.versionId());

            final var actualProject2 = s3Client.headObject(it -> it.bucket(bucketName).key("deploy/multiple-deployment-datasource/project2"));
            assertEquals(beforeStartProject2.lastModified(), actualProject2.lastModified());
            assertEquals(beforeStartProject2.versionId(), actualProject2.versionId());
        }
    }

}
