package org.openl.rules.ruleservice.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.project.abstraction.IProjectFolder;
import org.openl.rules.project.model.Module;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;

@TestPropertySource(properties = {"production-repository.base.path=",
    "production-repository.factory = repo-jdbc",
    "production-repository.uri = jdbc:h2:mem:temp;DB_CLOSE_DELAY=-1"})
@SpringJUnitConfig(locations = {"classpath:openl-ruleservice-property-placeholder.xml",
    "classpath:openl-ruleservice-datasource-beans.xml"})
public class RulesLoaderTest {

    @Autowired
    private Repository repository;
    @Autowired
    private RuleServiceLoader ruleServiceLoader;

    @Test
    public void testSkipDeletedProjects() throws Exception {
        List<String> d0 = ruleServiceLoader.getDeployments()
            .stream()
            .flatMap(d -> d.getProjects().stream())
            .map(IProjectFolder::getName)
            .sorted()
            .collect(Collectors.toList());
        assertEquals(Collections.emptyList(), d0);

        // First version deploy
        updateProject(repository, "deployment1", "project1", false);
        List<String> d1 = ruleServiceLoader.getDeployments()
            .stream()
            .flatMap(d -> d.getProjects().stream())
            .map(IProjectFolder::getName)
            .sorted()
            .collect(Collectors.toList());
        assertEquals(Collections.singletonList("project1"), d1);

        updateProject(repository, "deployment1", "project2", false);
        List<String> d2 = ruleServiceLoader.getDeployments()
            .stream()
            .flatMap(d -> d.getProjects().stream())
            .map(IProjectFolder::getName)
            .sorted()
            .collect(Collectors.toList());
        assertEquals(Arrays.asList("project1", "project2"), d2);

        // Second version deploy
        updateProject(repository, "deployment1", "project1", true); // Delete
        List<String> d3 = ruleServiceLoader.getDeployments()
            .stream()
            .flatMap(d -> d.getProjects().stream())
            .map(IProjectFolder::getName)
            .sorted()
            .collect(Collectors.toList());
        assertEquals(Collections.singletonList("project2"), d3);

        updateProject(repository, "deployment1", "project2", false);
        List<String> d4 = ruleServiceLoader.getDeployments()
            .stream()
            .flatMap(d -> d.getProjects().stream())
            .map(IProjectFolder::getName)
            .sorted()
            .collect(Collectors.toList());
        assertEquals(Collections.singletonList("project2"), d4);

        updateProject(repository, "org.openl.tablets.tutorial4", "org.openl.tablets.tutorial4", false);
        List<String> d5 = ruleServiceLoader.getDeployments()
            .stream()
            .flatMap(d -> d.getProjects().stream())
            .map(IProjectFolder::getName)
            .sorted()
            .collect(Collectors.toList());
        assertEquals(Arrays.asList("org.openl.tablets.tutorial4", "project2"), d5);

        CommonVersion commonVersion = new CommonVersionImpl("1");
        Collection<Module> modules = ruleServiceLoader
            .resolveProject("org.openl.tablets.tutorial4", commonVersion, "org.openl.tablets.tutorial4").getModules();
        assertNotNull(modules);
        assertFalse(modules.isEmpty());
        Module module = modules.iterator().next();
        assertEquals("Tutorial 4 - UServ Product Derby", module.getName());
    }

    private void updateProject(Repository repository,
            String deploymentName,
            String projectName,
            boolean delete) throws IOException {
        FileData fileData = new FileData();
        String resource = deploymentName + "/" + projectName;
        fileData.setName(resource);
        fileData.setAuthor(new UserInfo("user", "user@email", "User"));

        if (delete) {
            repository.delete(fileData);
        } else {
            InputStream str = RulesLoaderTest.class.getClassLoader()
                .getResourceAsStream("openl-repository/deploy/" + resource);

            repository.save(fileData, str != null ? str : new ByteArrayInputStream(new byte[0]));
        }
    }
}
