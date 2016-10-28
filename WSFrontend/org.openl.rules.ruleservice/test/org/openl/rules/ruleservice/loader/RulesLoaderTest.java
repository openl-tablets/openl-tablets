package org.openl.rules.ruleservice.loader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.openl.rules.ruleservice.Constants.DEPLOYMENT_NAME;
import static org.openl.rules.ruleservice.Constants.MODULE_NAME;
import static org.openl.rules.ruleservice.Constants.PROJECT_NAME;
import static org.openl.rules.ruleservice.Constants.VERSION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.resolving.ResolvingStrategy;
import org.openl.rules.project.resolving.RulesProjectResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource({"classpath:openl-ruleservice-ref.properties", "classpath:rules-production.properties"})
@ContextConfiguration({ "classpath:properties.xml", "classpath:openl-ruleservice-datasource-jcr-beans.xml" })
public class RulesLoaderTest {

    @Autowired
    private DataSource dataSource;
    private static RuleServiceLoader rulesLoader;

    @Before
    public void setDataSource() throws Exception {
        RulesProjectResolver projectResolver = new RulesProjectResolver();
        List<ResolvingStrategy> resolvingStrategies = new ArrayList<ResolvingStrategy>();
        resolvingStrategies.add(new org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy());
        resolvingStrategies.add(new org.openl.rules.project.resolving.SimpleXlsResolvingStrategy());
        projectResolver.setResolvingStrategies(resolvingStrategies);
        rulesLoader = new RuleServiceLoaderImpl(dataSource, new LocalTemporaryDeploymentsStorage(), projectResolver);
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