package org.openl.rules.ruleservice.loader;

import static org.junit.Assert.*;
import static org.openl.rules.ruleservice.Constants.*;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.resolving.ProjectResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = {
        "production-repository.factory = org.openl.rules.repository.file.FileSystemRepository",
        "production-repository.uri = test-resources/openl-repository",
        "version-in-deployment-name = true" })
@ContextConfiguration({ "classpath:openl-ruleservice-property-placeholder.xml",
        "classpath:openl-ruleservice-datasource-beans.xml" })
public class RulesLoaderTest {

    @Autowired
    @Qualifier("productionRepositoryDataSource")
    private DataSource dataSource;
    private static RuleServiceLoader rulesLoader;

    @Before
    public void setDataSource() throws Exception {
        ProjectResolver projectResolver = ProjectResolver.instance();
        rulesLoader = new RuleServiceLoaderImpl(dataSource,
            new LocalTemporaryDeploymentsStorage("target/openl-deploy2"),
            projectResolver);
    }

    @Test
    public void testGetDeployments() {
        Collection<Deployment> deployments = rulesLoader.getDeployments();
        assertNotNull(deployments);
        assertTrue(deployments.size() > 0);
    }

    @Test
    public void testResolveModulesForProject() {
        CommonVersion commonVersion = new CommonVersionImpl(VERSION);
        Collection<Module> modules = rulesLoader.resolveModulesForProject(DEPLOYMENT_NAME, commonVersion, PROJECT_NAME);
        assertNotNull(modules);
        assertTrue(modules.size() > 0);
        Module module = modules.iterator().next();
        assertEquals(MODULE_NAME, module.getName());
    }
}