package org.openl.itest;

import okio.Path;
import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;
import org.openl.rules.ruleservice.deployer.RulesDeployerService;

public class RunMinioTest extends AbstractMinioTest {

    @Test
    public void testSmoke() throws Exception {
        JettyServer server = null;
        try {
            server = JettyServer.start(config);
            var client = server.client();
            verifyS3Repository();
            assertDeployedServices("deploy/multiple-deployment-datasource/project1",
                "deploy/multiple-deployment-datasource/project2",
                "deploy/multiple-deployment-yaml-datasource/yaml-project1",
                "deploy/multiple-deployment-yaml-datasource/yaml-project2");
            client.test("test-resources-smoke/stage1");

            try (var deployer = new RulesDeployerService(config::get)) {
                deployer.deploy(Path.get("test-resources-smoke/stage2/rules-to-deploy-datasource.zip").toFile(), true);
            }

            assertDeployedServices("deploy/multiple-deployment-datasource/project1",
                "deploy/multiple-deployment-datasource/project2",
                "deploy/multiple-deployment-yaml-datasource/yaml-project1",
                "deploy/multiple-deployment-yaml-datasource/yaml-project2",
                "deploy/rules-to-deploy/rules-to-deploy");

            client.test("test-resources-smoke/stage3");
        } finally {
            if (server != null) {
                server.stop();
            }
        }
    }

}
