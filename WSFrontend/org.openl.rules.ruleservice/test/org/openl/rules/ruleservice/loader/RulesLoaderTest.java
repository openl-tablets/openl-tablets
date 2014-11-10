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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.resolving.EclipseBasedResolvingStrategy;
import org.openl.rules.project.resolving.ResolvingStrategy;
import org.openl.rules.project.resolving.RulesProjectResolver;

public class RulesLoaderTest {

    private static DataSource dataSource;
    private static RuleServiceLoader rulesLoader;

    @BeforeClass
    public static void setDataSource() throws Exception {
        dataSource = new JcrDataSource();
        RulesProjectResolver projectResolver = new RulesProjectResolver();
        List<ResolvingStrategy> resolvingStrategies = new ArrayList<ResolvingStrategy>();
        resolvingStrategies.add(new org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy());
        EclipseBasedResolvingStrategy eclipseBasedResolvingStrategy = new org.openl.rules.project.resolving.EclipseBasedResolvingStrategy();
        eclipseBasedResolvingStrategy.setTreeAdaptor(new org.openl.util.tree.FileTreeIterator.FileTreeAdaptor());
        resolvingStrategies.add(eclipseBasedResolvingStrategy);
        resolvingStrategies.add(new org.openl.rules.project.resolving.SimpleXlsResolvingStrategy());
        projectResolver.setResolvingStrategies(resolvingStrategies);
        rulesLoader = new RuleServiceLoaderImpl(dataSource, new LocalTemporaryDeploymentsStorage(), projectResolver);
    }

    @AfterClass
    public static void releaseDataSource() throws Exception {
        ((JcrDataSource) dataSource).destroy();
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