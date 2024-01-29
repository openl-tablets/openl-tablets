package org.openl.rules.webstudio.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Map;

import net.ttddyy.dsproxy.QueryCount;
import net.ttddyy.dsproxy.QueryCountHolder;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.openl.rules.security.standalone.persistence.OpenLProject;
import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagType;
import org.openl.rules.webstudio.service.config.TagsConfiguration;

/**
 * Project Tags Service Test
 *
 * @author Vladyslav Pikus
 */
@SpringJUnitConfig(classes = {DBTestConfiguration.class, TagsConfiguration.class})
@TestPropertySource(properties = {"db.url = jdbc:h2:mem:temp;DB_CLOSE_DELAY=-1", "db.user =", "db.password ="})
public class OpenLProjectServiceTest {

    @Autowired
    private OpenLProjectService projectService;
    @Autowired
    private TagService tagService;
    @Autowired
    private TagTypeService tagTypeService;

    @Autowired
    @Qualifier("flywayDBReset")
    private Flyway flywayDBReset;

    @BeforeEach
    public void setUp() {
        // Reset all changes where done while testing
        flywayDBReset.clean();
        flywayDBReset.migrate();
        QueryCountHolder.clear();
    }

    @Test
    public void smokeTest() {
        var functionalityType = createTagType("Functionality");
        tagTypeService.save(functionalityType);
        var fooTag = createTag("Foo", functionalityType);
        tagService.save(fooTag);
        var barTag = createTag("Bar", functionalityType);
        tagService.save(barTag);

        var lobType = createTagType("LOB");
        tagTypeService.save(lobType);
        var autoTag = createTag("Auto", lobType);
        tagService.save(autoTag);

        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(5, queryCount.getInsert());
        assertEquals(5, queryCount.getTotal());

        QueryCountHolder.clear();
        var project1 = createProject("repo1", "path1", List.of(fooTag, autoTag));
        projectService.save(project1);
        queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(2, queryCount.getInsert());
        assertEquals(2, queryCount.getTotal());

        QueryCountHolder.clear();
        var project2 = createProject("repo2", "path2", List.of(barTag));
        projectService.save(project2);
        queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(2, queryCount.getInsert());
        assertEquals(2, queryCount.getTotal());

        QueryCountHolder.clear();
        var tagTypes = tagTypeService.getAll();
        assertEquals(2, tagTypes.size());
        queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(3, queryCount.getSelect());
        assertEquals(3, queryCount.getTotal());

        QueryCountHolder.clear();
        var project1Tags = projectService.getTagsForProject(project1.getRepositoryId(), project1.getProjectPath());
        assertEquals(2, project1Tags.size());
        for (var tag : project1Tags) {
            if (tag.getName().equals("Foo")) {
                assertEquals("Functionality", tag.getType().getName());
            } else if (tag.getName().equals("Auto")) {
                assertEquals("LOB", tag.getType().getName());
            } else {
                fail("Unexpected tag");
            }
        }
        assertEquals(3, queryCount.getSelect());
        assertEquals(3, queryCount.getTotal());

        QueryCountHolder.clear();
        assertTrue(projectService.isProjectHasTags(project1.getRepositoryId(), project1.getProjectPath(), Map.of("Functionality", "Foo", "LOB", "Auto")));
        assertFalse(projectService.isProjectHasTags(project1.getRepositoryId(), project1.getProjectPath(), Map.of("Functionality", "Foo", "LOB", "Bar")));

        assertTrue(projectService.isProjectHasTags(project2.getRepositoryId(), project2.getProjectPath(), Map.of("Functionality", "Bar")));
        assertFalse(projectService.isProjectHasTags(project2.getRepositoryId(), project2.getProjectPath(), Map.of("Functionality", "Foo")));

        queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(4, queryCount.getSelect());
        assertEquals(4, queryCount.getTotal());
    }

    private OpenLProject createProject(String repositoryId, String path, List<Tag> tags) {
        var project = new OpenLProject();
        project.setRepositoryId(repositoryId);
        project.setProjectPath(path);
        project.setTags(tags);
        return project;
    }

    private TagType createTagType(String name) {
        var tagType = new TagType();
        tagType.setName(name);
        return tagType;
    }

    private Tag createTag(String name, TagType type) {
        var tag = new Tag();
        tag.setName(name);
        tag.setType(type);
        return tag;
    }

}
