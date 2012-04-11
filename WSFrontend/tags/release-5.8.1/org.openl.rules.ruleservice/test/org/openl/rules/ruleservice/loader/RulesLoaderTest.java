package org.openl.rules.ruleservice.loader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.openl.rules.ruleservice.Constants.DEPLOYMENT_NAME;
import static org.openl.rules.ruleservice.Constants.MODULE_NAME;
import static org.openl.rules.ruleservice.Constants.PROJECT_NAME;
import static org.openl.rules.ruleservice.Constants.VERSION;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.impl.local.LocalFolderAPI;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.resolving.EclipseBasedResolvingStrategy;
import org.openl.rules.project.resolving.ResolvingStrategy;
import org.openl.rules.project.resolving.RulesProjectResolver;
import org.openl.rules.repository.jcr.JcrFolderAPI;
import org.openl.rules.ruleservice.core.ModuleConfiguration;
import org.openl.rules.ruleservice.core.ServiceDescription;

public class RulesLoaderTest {

    private static IDataSource dataSource;
    private static IRulesLoader rulesLoader;

    @BeforeClass
    public static void setDataSource() throws Exception{
        dataSource = new JcrDataSource();
        RulesProjectResolver projectResolver = new RulesProjectResolver();
        List<ResolvingStrategy> resolvingStrategies = new ArrayList<ResolvingStrategy>();
        resolvingStrategies.add(new org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy());
        EclipseBasedResolvingStrategy eclipseBasedResolvingStrategy = new org.openl.rules.project.resolving.EclipseBasedResolvingStrategy();
        eclipseBasedResolvingStrategy.setTreeAdaptor(new org.openl.util.tree.FileTreeIterator.FileTreeAdaptor());
        resolvingStrategies.add(eclipseBasedResolvingStrategy);
        resolvingStrategies.add(new org.openl.rules.project.resolving.SimpleXlsResolvingStrategy());
        projectResolver.setResolvingStrategies(resolvingStrategies);
        rulesLoader = new RulesLoader(dataSource, new LocalTemporaryDeploymentsStorage(), projectResolver);
    }

    
    @Test
    public void testGetDeployment() {
        LocalTemporaryDeploymentsStorage storage = new LocalTemporaryDeploymentsStorage();
        CommonVersion commonVersion = new CommonVersionImpl(VERSION);
        Deployment deployment = rulesLoader.getDeployment(DEPLOYMENT_NAME, commonVersion);
        assertNotNull(deployment);
        assertEquals(DEPLOYMENT_NAME, deployment.getDeploymentName());
        assertEquals(VERSION, deployment.getCommonVersion().getVersionName());
        assertTrue(deployment.getAPI() instanceof JcrFolderAPI);
        storage.loadDeployment(deployment);
        deployment = rulesLoader.getDeployment(DEPLOYMENT_NAME, commonVersion);
        assertTrue(deployment.getAPI() instanceof LocalFolderAPI);
    }

    @Test
    public void testGetDeployments() {
        List<Deployment> deployments = rulesLoader.getDeployments();
        assertNotNull(deployments);
        assertTrue(deployments.size() > 0);
    }

    @Test
    public void testResolveModulesForProject() {
        CommonVersion commonVersion = new CommonVersionImpl(VERSION);
        List<Module> modules = rulesLoader.resolveModulesForProject(DEPLOYMENT_NAME, commonVersion, PROJECT_NAME);
        assertNotNull(modules);
        assertTrue(modules.size() > 0);
        Module module = modules.get(0);
        assertEquals(MODULE_NAME, module.getName());
    }

    @Test
    public void testGetModulesForService() {
        ServiceDescription serviceDescription = new ServiceDescription();
        List<ModuleConfiguration> configurationModules = new ArrayList<ModuleConfiguration>();
        CommonVersion commonVersion = new CommonVersionImpl(VERSION);
        ModuleConfiguration moduleConfiguration = new ModuleConfiguration(DEPLOYMENT_NAME, commonVersion, PROJECT_NAME,
                MODULE_NAME);
        configurationModules.add(moduleConfiguration);
        serviceDescription.setModulesToLoad(configurationModules);
        serviceDescription.setName("myService");
        serviceDescription.setServiceClassName(null);
        serviceDescription.setUrl(null);
        List<Module> modules = rulesLoader.getModulesForService(serviceDescription);
        assertTrue(modules.size() > 0);
    }
}