package org.openl.studio.tags.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.openl.rules.security.standalone.persistence.TagType;
import org.openl.rules.webstudio.service.DBTestConfiguration;
import org.openl.studio.tags.model.TagTypeDTO;

/**
 * The order of the tag types, kept in the database.
 *
 * @author Yury Molchan
 */
@SpringJUnitConfig(classes = {DBTestConfiguration.class, TagTypeService.class})
@TestPropertySource(properties = {"db.url = jdbc:h2:mem:tagTypes;DB_CLOSE_DELAY=-1",
        "db.user =",
        "db.password =",
        "db.maximumPoolSize = 3"})
class TagTypeServiceTest {

    @Autowired
    private TagTypeService tagTypeService;

    @Autowired
    @Qualifier("flywayDBReset")
    private Flyway flywayDBReset;

    @BeforeEach
    void setUp() {
        flywayDBReset.clean();
        flywayDBReset.migrate();
        for (var name : List.of("lob", "Domain", "Region")) {
            var type = new TagType();
            type.setName(name);
            tagTypeService.save(type);
        }
    }

    @Test
    void listsTheTypesByNameUntilTheyAreArranged() {
        assertEquals(List.of("Domain", "lob", "Region"), names());
    }

    @Test
    void listsTheTypesInTheArrangedOrder() {
        assertTrue(tagTypeService.reorder(List.of(id("Region"), id("lob"), id("Domain"))));

        assertEquals(List.of("Region", "lob", "Domain"), names());
        assertEquals(List.of("Region", "lob", "Domain"),
                tagTypeService.getAll().stream().map(TagTypeDTO::getName).toList());
    }

    @Test
    void listsTheTypesLeftOutOfTheOrderAfterTheArrangedOnesByName() {
        assertTrue(tagTypeService.reorder(List.of(id("Region"))));
        var added = new TagType();
        added.setName("Attribute");
        tagTypeService.save(added);

        assertEquals(List.of("Region", "Attribute", "Domain", "lob"), names());
    }

    @Test
    void changesNothingWhenAnIdNamesNoType() {
        assertTrue(tagTypeService.reorder(List.of(id("Region"), id("Domain"), id("lob"))));

        assertFalse(tagTypeService.reorder(List.of(id("lob"), -1L)));

        assertEquals(List.of("Region", "Domain", "lob"), names());
    }

    private List<String> names() {
        return tagTypeService.getAllTagTypes().stream().map(TagType::getName).toList();
    }

    private Long id(String name) {
        return tagTypeService.getByName(name).getId();
    }
}
