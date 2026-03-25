package org.openl.itest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.model.S3Object;

import org.openl.itest.core.JettyServer;
import org.openl.rules.ruleservice.deployer.RulesDeployerService;

public class RunS3Test extends AbstractS3Test {

    @Test
    public void testSmoke() throws Exception {
        try (var client = JettyServer.get().withInitParam(config).start()) {

            verifyS3Repository();
            assertDeployedServices("deploy/multiple-deployment-datasource/project1",
                    "deploy/multiple-deployment-datasource/project2",
                    "deploy/multiple-deployment-yaml-datasource/yaml-project1",
                    "deploy/multiple-deployment-yaml-datasource/yaml-project2");
            client.test("test-resources-smoke/stage1");

            try (var deployer = new RulesDeployerService(config::get)) {
                deployer.deploy(Paths.get("test-resources-smoke/stage2/rules-to-deploy-datasource.zip").toFile(), true);
            }

            assertDeployedServices("deploy/multiple-deployment-datasource/project1",
                    "deploy/multiple-deployment-datasource/project2",
                    "deploy/multiple-deployment-yaml-datasource/yaml-project1",
                    "deploy/multiple-deployment-yaml-datasource/yaml-project2",
                    "deploy/rules-to-deploy/rules-to-deploy");

            client.test("test-resources-smoke/stage3");
        }
    }

    private void assertDeployedServices(String... services) throws Exception {
        var response = s3Client.listObjectsV2(it -> it.bucket(bucketName).prefix("deploy"));
        var actualObjects = response.contents().stream().map(S3Object::key).collect(Collectors.toSet());
        assertEquals(Set.of(services), actualObjects);
    }

}
