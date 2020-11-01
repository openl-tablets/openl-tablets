package org.openl.rules.ruleservice.loader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.openl.rules.ruleservice.Constants.DEPLOYMENT_NAME;
import static org.openl.rules.ruleservice.Constants.MODULE_NAME;
import static org.openl.rules.ruleservice.Constants.PROJECT_NAME;
import static org.openl.rules.ruleservice.Constants.VERSION;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.model.Module;
import org.openl.rules.repository.RepositoryInstatiator;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.db.DBRepository;
import org.openl.rules.repository.db.JdbcDBRepositoryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = {
        "production-repository.factory = org.openl.rules.repository.file.FileSystemRepository",
        "production-repository.uri = test-resources/openl-repository" })
@ContextConfiguration({ "classpath:openl-ruleservice-property-placeholder.xml",
        "classpath:openl-ruleservice-datasource-beans.xml" })
public class RulesLoaderTest {
    private static final String DEPLOY_PATH = "deploy/";

    @Autowired
    @Qualifier("repository")
    private Repository repository;
    private static RuleServiceLoader rulesLoader;
    private Deployment deployment;

    @Before
    public void setDataSource() throws Exception {
        rulesLoader = new RuleServiceLoaderImpl(repository);
        ((RuleServiceLoaderImpl) rulesLoader).setDeployPath(DEPLOY_PATH);
        Collection<Deployment> deployments = rulesLoader.getDeployments();
        assertTrue(!deployments.isEmpty());
        deployment = deployments.iterator().next();
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

    @Test
    public void testContainsDeloymentAndLoadDeployment() throws Exception {
        RuleServiceLoaderImpl storage = new RuleServiceLoaderImpl(repository);
        assertFalse(storage.containsDeployment(deployment.getDeploymentName()));
        storage.loadDeployment(deployment);
        assertTrue(storage.containsDeployment(deployment.getDeploymentName()));
    }

    @Test
    public void testLoadDeployment() throws Exception {
        RuleServiceLoaderImpl storage = new RuleServiceLoaderImpl(repository);
        assertNull(storage.getDeploymentFromCache(deployment.getDeploymentName(), deployment.getCommonVersion()));
        assertFalse(storage.containsDeployment(deployment.getDeploymentName()));
        storage.loadDeployment(deployment);
        assertTrue(storage.containsDeployment(deployment.getDeploymentName()));
    }

    @Test
    public void testGetDeployment() throws Exception {
        RuleServiceLoaderImpl storage = new RuleServiceLoaderImpl(repository);
        assertNull(storage.getDeploymentFromCache(deployment.getDeploymentName(), deployment.getCommonVersion()));
        assertFalse(storage.containsDeployment(deployment.getDeploymentName()));
        storage.loadDeployment(deployment);
        assertTrue(storage.containsDeployment(deployment.getDeploymentName()));
        Deployment deployment1 = storage.getDeploymentFromCache(deployment.getDeploymentName(),
            deployment.getCommonVersion());
        assertNotNull(deployment1);
        Deployment deployment2 = storage.getDeploymentFromCache(deployment.getDeploymentName(),
            deployment.getCommonVersion());
        assertSame(deployment1, deployment2);
    }

    @Test
    public void testSkipDeletedProjects() throws Exception {
        String deploymentName = "deployment1";
        CommonVersionImpl version = new CommonVersionImpl("3");

        RuleServiceLoaderImpl storage = new RuleServiceLoaderImpl(repository);
        assertFalse(storage.containsDeployment(deploymentName));

        Map<String, String> params = new HashMap<>();
        params.put("uri", "jdbc:h2:mem:temp;DB_CLOSE_DELAY=-1");
        try (DBRepository repository = (DBRepository) RepositoryInstatiator
            .newRepository(JdbcDBRepositoryFactory.class.getName(), params)) {
            // First version deploy
            updateProject(repository, deploymentName, "project1", false);
            updateProject(repository, deploymentName, "project2", false);
            // Second version deploy
            updateProject(repository, deploymentName, "project1", true); // Delete
            updateProject(repository, deploymentName, "project2", false);

            Deployment remoteDeployment = new Deployment(repository,
                DEPLOY_PATH + deploymentName,
                deploymentName,
                version,
                false);
            assertTrue(containsProject(remoteDeployment, "project2"));
            assertFalse(containsProject(remoteDeployment, "project1"));

            storage.loadDeployment(remoteDeployment);

            Deployment localDeployment = storage.getDeploymentFromCache(deploymentName, version);
            assertTrue(containsProject(localDeployment, "project2"));
            assertFalse(containsProject(localDeployment, "project1"));
        }
    }

    private boolean containsProject(Deployment deployment, String projectName) {
        Collection<AProject> projects = deployment.getProjects();
        for (AProject project : projects) {
            if (projectName.equals(project.getName())) {
                return true;
            }
        }

        return false;
    }

    private void updateProject(DBRepository repository,
            String deploymentName,
            String projectName,
            boolean delete) throws IOException {
        String deploymentPath = DEPLOY_PATH + deploymentName;
        byte[] zip = createZip();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(zip);
        FileData fileData = new FileData();
        fileData.setName(deploymentPath + "/" + projectName);
        fileData.setAuthor("user");
        fileData.setSize(zip.length);

        if (delete) {
            repository.delete(fileData);
        } else {
            repository.save(fileData, inputStream);
        }
    }

    private byte[] createZip() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry("test.xls");
            zos.putNextEntry(entry);
            zos.write(new byte[0]);
            zos.closeEntry();
            return baos.toByteArray();
        }
    }
}