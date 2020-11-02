package org.openl.rules.ruleservice.loader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.project.abstraction.AProjectFolder;
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
        "production-repository.uri = test-resources/openl-repository/deploy" })
@ContextConfiguration({ "classpath:openl-ruleservice-property-placeholder.xml",
        "classpath:openl-ruleservice-datasource-beans.xml" })
public class RulesLoaderTest {

    @Autowired
    @Qualifier("repository")
    private Repository repository;
    private static RuleServiceLoader rulesLoader;
    private Deployment deployment;

    @Before
    public void setDataSource() throws Exception {
        rulesLoader = new RuleServiceLoaderImpl(repository);
        Collection<Deployment> deployments = rulesLoader.getDeployments();
        assertTrue(!deployments.isEmpty());
        deployment = deployments.iterator().next();
    }

    @Test
    public void testResolveModulesForProject() {
        CommonVersion commonVersion = new CommonVersionImpl("1");
        Collection<Module> modules = rulesLoader.resolveModulesForProject("org.openl.tablets.tutorial4", commonVersion, "org.openl.tablets.tutorial4");
        assertNotNull(modules);
        assertTrue(modules.size() > 0);
        Module module = modules.iterator().next();
        assertEquals("Tutorial 4 - UServ Product Derby", module.getName());
    }

    @Test
    public void testLoadDeployment() throws Exception {
        RuleServiceLoaderImpl storage = new RuleServiceLoaderImpl(repository);
        Deployment deployment1 = storage.getDeployment(this.deployment.getDeploymentName(),
            this.deployment.getCommonVersion());
        Deployment deployment2 = storage.getDeployment(this.deployment.getDeploymentName(),
            this.deployment.getCommonVersion());
        assertNotNull(deployment1);
        assertSame(deployment1, deployment2);
    }

    @Test
    public void testSkipDeletedProjects() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("uri", "jdbc:h2:mem:temp;DB_CLOSE_DELAY=-1");
        try (DBRepository repository = (DBRepository) RepositoryInstatiator
            .newRepository(JdbcDBRepositoryFactory.class.getName(), params)) {
            RuleServiceLoaderImpl storage = new RuleServiceLoaderImpl(repository);

            List<String> d0 = storage.getDeployments()
                .stream()
                .flatMap(d -> d.getProjects().stream())
                .map(AProjectFolder::getName)
                .sorted()
                .collect(Collectors.toList());
            assertEquals(Collections.emptyList(), d0);

            // First version deploy
            updateProject(repository, "deployment1", "project1", false);
            List<String> d1 = storage.getDeployments()
                    .stream()
                    .flatMap(d -> d.getProjects().stream())
                    .map(AProjectFolder::getName)
                    .sorted()
                    .collect(Collectors.toList());
            assertEquals(Arrays.asList("project1"), d1);

            updateProject(repository, "deployment1", "project2", false);
            List<String> d2 = storage.getDeployments()
                .stream()
                .flatMap(d -> d.getProjects().stream())
                .map(AProjectFolder::getName)
                .sorted()
                .collect(Collectors.toList());
            assertEquals(Arrays.asList("project1", "project2"), d2);

            // Second version deploy
            updateProject(repository, "deployment1", "project1", true); // Delete
            List<String> d3 = storage.getDeployments()
                    .stream()
                    .flatMap(d -> d.getProjects().stream())
                    .map(AProjectFolder::getName)
                    .sorted()
                    .collect(Collectors.toList());
            assertEquals(Collections.singletonList("project2"), d3);

            updateProject(repository, "deployment1", "project2", false);
            List<String> d4 = storage.getDeployments()
                .stream()
                .flatMap(d -> d.getProjects().stream())
                .map(AProjectFolder::getName)
                .sorted()
                .collect(Collectors.toList());
            assertEquals(Collections.singletonList("project2"), d4);

            storage.destroy();
        }
    }

    private void updateProject(DBRepository repository,
            String deploymentName,
            String projectName,
            boolean delete) throws IOException {
        FileData fileData = new FileData();
        fileData.setName(deploymentName + "/" + projectName);
        fileData.setAuthor("user");

        if (delete) {
            repository.delete(fileData);
        } else {
            fileData.setSize(0);
            repository.save(fileData, new ByteArrayInputStream(new byte[0]));
        }
    }
}