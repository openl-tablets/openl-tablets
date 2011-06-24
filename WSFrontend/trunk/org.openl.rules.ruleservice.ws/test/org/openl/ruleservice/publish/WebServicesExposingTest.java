package org.openl.ruleservice.publish;

import static junit.framework.Assert.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.annox.xml.bind.AnnoxAnnotationReader;
import org.openl.rules.common.CommonUser;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.Property;
import org.openl.rules.common.PropertyException;
import org.openl.rules.common.impl.CommonUserImpl;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.core.ServiceDescription.ModuleConfiguration;
import org.openl.rules.ruleservice.loader.IRulesLoader;
import org.openl.rules.ruleservice.management.IServiceConfigurer;
import org.openl.rules.ruleservice.management.ServiceManager;
import org.openl.rules.ruleservice.publish.RulesPublisher;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.deploy.impl.jcr.JcrProductionDeployer;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sun.xml.bind.api.JAXBRIContext;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-ruleservice-beans.xml" })
public class WebServicesExposingTest implements ApplicationContextAware {
    public static class TestConfigurer implements IServiceConfigurer {
        private static CommonVersion getLastVersion(IRulesLoader loader, String deploymentName) {
            CommonVersion lastVersion = new CommonVersionImpl(0, 0, 0);
            for (Deployment deployment : loader.getDeployments()) {
                if (deployment.getDeploymentName().equals(deploymentName)) {
                    if (lastVersion.compareTo(deployment.getCommonVersion()) < 0) {
                        lastVersion = deployment.getCommonVersion();
                    }
                }
            }
            return lastVersion;
        }

        private ServiceDescription resolveTutorial4Service(IRulesLoader loader) {
            final String deploymentName = "org.openl.tablets.tutorial4";
            List<ModuleConfiguration> modules = new ArrayList<ServiceDescription.ModuleConfiguration>(1);
            modules.add(new ModuleConfiguration(deploymentName, getLastVersion(loader, deploymentName), deploymentName,
                    "Tutorial 4 - UServ Product Derby"));
            return new ServiceDescription("tutorial4", TUTORIAL4_SERVICE_URL, null, false, modules);
        }

        private ServiceDescription resolveMultimoduleService(IRulesLoader loader) {
            final String multiModuleDeploymentName = "multimodule";
            final String domainDeploymentName = "domain";
            List<ModuleConfiguration> modules = new ArrayList<ServiceDescription.ModuleConfiguration>(1);
            modules.add(new ModuleConfiguration(domainDeploymentName, getLastVersion(loader, domainDeploymentName),
                    domainDeploymentName, "Domain"));
            modules.add(new ModuleConfiguration(multiModuleDeploymentName, getLastVersion(loader,
                    multiModuleDeploymentName), "project1", "Module1_1"));
            modules.add(new ModuleConfiguration(multiModuleDeploymentName, getLastVersion(loader,
                    multiModuleDeploymentName), "project2", "Module2_1"));
            modules.add(new ModuleConfiguration(multiModuleDeploymentName, getLastVersion(loader,
                    multiModuleDeploymentName), "project3", "Module3_1"));
            return new ServiceDescription("multimodule", MULTIMODULE_SERVICE_URL, null, false, modules);
        }

        public List<ServiceDescription> getServicesToBeDeployed(IRulesLoader loader) {
            List<ServiceDescription> services = new ArrayList<ServiceDescription>();
            services.add(resolveTutorial4Service(loader));
            services.add(resolveMultimoduleService(loader));
            return services;
        }
    }

    private static final String TUTORIAL4_SERVICE_URL = "org.openl.tablets.tutorial4";
    private static final String MULTIMODULE_SERVICE_URL = "multimodule";
    private static final String TEST_REPOSITORY_PATH = "./test-resources/production-repository/";

    private ApplicationContext applicationContext;

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @BeforeClass
    public static void createdRepository() throws Exception {
        unzipArchive(new File("./test-resources/production-repository.zip"), new File(TEST_REPOSITORY_PATH));
    }

    @Test
    public void testExposing() throws Exception {
        assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);
        serviceManager.start();
        WebServicesDeploymentAdmin deploymentAdmin = applicationContext.getBean("deploymentAdmin",
                WebServicesDeploymentAdmin.class);
        final String baseUrl = deploymentAdmin.getBaseAddress();
        assertEquals(2, applicationContext.getBean("rulesPublisher", RulesPublisher.class).getRunningServices().size());

        JaxWsDynamicClientFactory clientFactory = JaxWsDynamicClientFactory.newInstance();
        final Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(JAXBRIContext.ANNOTATION_READER, new AnnoxAnnotationReader());
        clientFactory.setJaxbContextProperties(properties);

        Client tutorial4Client = clientFactory.createClient(baseUrl + TUTORIAL4_SERVICE_URL + "?wsdl");
        Client multimoduleClient = clientFactory.createClient(baseUrl + MULTIMODULE_SERVICE_URL + "?wsdl");

        assertEquals("World, Good Morning!",
                multimoduleClient.invoke("worldHello", new Object[] { new Integer(10) })[0]);
        assertEquals(2, CollectionUtils.size(multimoduleClient.invoke("getData1")[0]));
        assertEquals(3, CollectionUtils.size(multimoduleClient.invoke("getData2")[0]));
        assertEquals(2, CollectionUtils.size(tutorial4Client.invoke("getCoverage")[0]));
    }

    @AfterClass
    public static void deleteCreatedRepository() throws Exception {
        ProductionRepositoryFactoryProxy.release();
        File tempRepoFolder = new File(TEST_REPOSITORY_PATH);
        if (tempRepoFolder.exists()) {
            FileUtils.deleteDirectory(tempRepoFolder);
        }
    }

    @Test
    public void testServerPrototypes() {
        assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);
        serviceManager.start();
        WebServicesDeploymentAdmin deploymentAdmin = applicationContext.getBean("deploymentAdmin",
                WebServicesDeploymentAdmin.class);
        ServerFactoryBean firstServer = deploymentAdmin.getServerFactoryBean();
        ServerFactoryBean secondServer = deploymentAdmin.getServerFactoryBean();
        assertTrue(firstServer != secondServer);
    }

    @Test
    public void testRedeployAfterChanges() throws Exception {
        assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);
        IRulesLoader rulesLoader = serviceManager.getRulesLoader();
        serviceManager.start();
        OpenLService multimoduleService = serviceManager.getRuleService().findServiceByName("multimodule");
        OpenLService tutorial4Service = serviceManager.getRuleService().findServiceByName("tutorial4");
        Deployment domainDeployment = rulesLoader.getDeployment("domain",
                TestConfigurer.getLastVersion(rulesLoader, "domain"));

        ADeploymentProject testDeploymentProject = new ADeploymentProject(domainDeployment.getAPI(), null);
        new JcrProductionDeployer(new WorkspaceUserImpl("test")).deploy(testDeploymentProject,
                domainDeployment.getProjects());
        for (int i = 0; i < 12; i++) {//waiting for redeploying of services during.
            Thread.sleep(5000); // notifications come asynchroniously
            if (multimoduleService != serviceManager.getRuleService().findServiceByName("multimodule")) {
                break;
            }
        }
        assertNotSame(multimoduleService, serviceManager.getRuleService().findServiceByName("multimodule"));
        //uncomment after the smart redeployment will be implemented
        //assertSame(tutorial4Service, serviceManager.getRuleService().findServiceByName("tutorial4"));
    }

    public static void unzipArchive(File archive, File outputDir) throws Exception {
        ZipFile zipfile = new ZipFile(archive);
        Enumeration<? extends ZipEntry> e = zipfile.entries();
        while (e.hasMoreElements()) {
            ZipEntry entry = e.nextElement();
            unzipEntry(zipfile, entry, outputDir);
        }
    }

    private static void unzipEntry(ZipFile zipfile, ZipEntry entry, File outputDir) throws IOException {
        if (entry.isDirectory()) {
            createDir(new File(outputDir, entry.getName()));
            return;
        }
        File outputFile = new File(outputDir, entry.getName());
        if (!outputFile.getParentFile().exists()) {
            createDir(outputFile.getParentFile());
        }

        BufferedInputStream inputStream = new BufferedInputStream(zipfile.getInputStream(entry));
        BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));

        try {
            IOUtils.copy(inputStream, outputStream);
        } finally {
            outputStream.close();
            inputStream.close();
        }
    }

    private static void createDir(File dir) {
        if (!dir.mkdirs())
            throw new RuntimeException("Can not create dir " + dir);
    }
}
