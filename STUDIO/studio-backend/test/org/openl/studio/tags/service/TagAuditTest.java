package org.openl.studio.tags.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagType;
import org.openl.rules.webstudio.service.DBTestConfiguration;
import org.openl.studio.audit.AuditEventsCollector;
import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditEvent;
import org.openl.studio.audit.model.AuditTargetType;
import org.openl.studio.audit.service.TagAuditor;

/**
 * The changes of the tags configuration are announced to the audit log, and only the changes.
 *
 * @author Yury Molchan
 */
@SpringJUnitConfig(classes = {DBTestConfiguration.class,
        TagTypeService.class,
        TagService.class,
        TagTemplateService.class,
        TagAuditor.class,
        TagAuditTest.Config.class})
@TestPropertySource(properties = {"db.url = jdbc:h2:mem:tag-audit;DB_CLOSE_DELAY=-1",
        "db.user =",
        "db.password =",
        "db.maximumPoolSize = 3"})
class TagAuditTest {

    @Configuration
    static class Config {

        @Bean
        AuditEventsCollector auditEventsCollector() {
            return new AuditEventsCollector();
        }
    }

    @Autowired
    private TagTypeService tagTypeService;

    @Autowired
    private TagService tagService;

    @Autowired
    private TagTemplateService tagTemplateService;

    @Autowired
    private AuditEventsCollector audit;

    @Autowired
    @Qualifier("flywayDBReset")
    private Flyway flywayDBReset;

    @BeforeEach
    void setUp() {
        flywayDBReset.clean();
        flywayDBReset.migrate();
        audit.clear();
    }

    @Test
    void announcesATagTypeCreatedChangedAndDeleted() {
        var product = type("Product");
        tagTypeService.define(Map.of("Product", List.of("Auto", "Home")));
        audit.clear();

        assertTrue(tagTypeService.update(product.getId(), "Product", true, false), "Saved as it was");
        assertTrue(tagTypeService.update(product.getId(), "Line", false, true));
        assertTrue(tagTypeService.delete(product.getId()));

        assertEquals(List.of(AuditAction.TAG_TYPE_UPDATED, AuditAction.TAG_TYPE_DELETED), actions(),
                "A save that keeps every value announces nothing");
        var updated = audit.events().getFirst();
        assertEquals(AuditTargetType.TAG_TYPE, updated.targetType());
        assertEquals("Line", updated.target());
        assertEquals(Map.of("changed", Map.of(
                "name", change("Product", "Line"),
                "extensible", change(false, true),
                "required", change(false, true))), updated.details());
        var deleted = audit.events().getLast();
        assertEquals("Line", deleted.target());
        assertEquals(Map.of("tags", List.of("Auto", "Home")), deleted.details());
        assertNull(tagTypeService.getById(product.getId()));
    }

    @Test
    void announcesATagTypeCreatedByItsNameWithNoTagsYet() {
        var type = new TagType();
        type.setName("Region");
        type.setExtensible(true);
        tagTypeService.save(type);

        var created = onlyEvent(AuditAction.TAG_TYPE_CREATED);
        assertEquals("Region", created.target());
        assertEquals(Map.of("extensible", true, "required", true), created.details());
    }

    @Test
    void announcesWhatDefiningTheTagsFoundInProjectsAdds() {
        type("Region");
        audit.clear();

        tagTypeService.define(Map.of("region", List.of("EU")));
        tagTypeService.define(Map.of("Region", List.of("eu", "Asia"), "Product", List.of("Auto")));

        assertEquals(Set.of(Map.of("tag", "EU"), Map.of("tag", "Asia")),
                Set.copyOf(audit.events(AuditAction.TAG_ADDED).stream().map(AuditEvent::details).toList()),
                "A value that exists under another case is not added again");
        audit.events(AuditAction.TAG_ADDED).forEach(event -> assertEquals("Region", event.target()));
        var created = onlyEvent(AuditAction.TAG_TYPE_CREATED);
        assertEquals("Product", created.target());
        assertEquals(Map.of("extensible", false, "required", false, "tags", List.of("Auto")), created.details());
    }

    @Test
    void announcesNothingForATagTypeThatDoesNotExist() {
        assertFalse(tagTypeService.update(-1L, "Region", true, false));
        assertFalse(tagTypeService.delete(-1L));
        assertFalse(tagService.rename(-1L, "EU"));
        assertFalse(tagService.delete(-1L));

        assertEquals(List.of(), audit.events());
    }

    @Test
    void announcesTheTagTypesReorderedOnlyWhenTheOrderChanges() {
        var ids = List.of(type("lob").getId(), type("Domain").getId(), type("Region").getId());
        audit.clear();

        // They are listed by name until they are arranged, so this order changes nothing.
        assertTrue(tagTypeService.reorder(List.of(ids.get(1), ids.get(0), ids.get(2))));
        assertEquals(List.of(), audit.events());

        assertTrue(tagTypeService.reorder(List.of(ids.get(2), ids.get(1))));
        assertFalse(tagTypeService.reorder(List.of(-1L)));

        var reordered = onlyEvent(AuditAction.TAG_TYPES_REORDERED);
        assertEquals("Region, Domain, lob", reordered.target());
        assertEquals(Map.of("changed", Map.of("order",
                        change(List.of("Domain", "lob", "Region"), List.of("Region", "Domain", "lob")))),
                reordered.details());
    }

    @Test
    void announcesATagAddedRenamedAndDeleted() {
        var region = type("Region");
        audit.clear();
        var tag = new Tag();
        tag.setType(region);
        tag.setName("EU");

        tagService.save(tag);
        assertTrue(tagService.rename(tag.getId(), "EU"), "Saved as it was");
        assertTrue(tagService.rename(tag.getId(), "Europe"));
        assertTrue(tagService.delete(tag.getId()));

        assertEquals(List.of(AuditAction.TAG_ADDED, AuditAction.TAG_RENAMED, AuditAction.TAG_DELETED), actions());
        audit.events().forEach(event -> {
            assertEquals(AuditTargetType.TAG_TYPE, event.targetType());
            assertEquals("Region", event.target());
        });
        assertEquals(Map.of("tag", "EU"), audit.events().getFirst().details());
        assertEquals(Map.of("changed", Map.of("tag", change("EU", "Europe"))), audit.events().get(1).details());
        assertEquals(Map.of("tag", "Europe"), audit.events().getLast().details());
        assertNull(tagService.getById(tag.getId()));
    }

    @Test
    void announcesTheTemplatesAddedAndRemoved() {
        tagTemplateService.save(List.of("%Domain%-*", " ", "%LOB%_?"));
        tagTemplateService.save(List.of("%Domain%-*", "%LOB%_?"));
        tagTemplateService.save(List.of("%Domain%-*", "%Region%.*"));

        assertEquals(List.of("%Domain%-*", "%Region%.*"), tagTemplateService.getTemplates());
        var events = audit.events(AuditAction.TAG_TEMPLATES_CHANGED);
        assertEquals(2, events.size(), "A save that keeps the templates announces nothing");
        assertEquals(events, audit.events());
        events.forEach(event -> {
            assertEquals(AuditTargetType.TAG_TEMPLATES, event.targetType());
            assertNull(event.target());
        });
        assertEquals(Map.of("added", Set.of("%Domain%-*", "%LOB%_?"), "removed", Set.of()),
                events.getFirst().details());
        assertEquals(Map.of("added", Set.of("%Region%.*"), "removed", Set.of("%LOB%_?")), events.getLast().details());
    }

    @Test
    void announcesTheTemplatesWhosePriorityChanged() {
        tagTemplateService.save(List.of("%Domain%-*", "%LOB%_?"));
        audit.clear();

        tagTemplateService.save(List.of("%LOB%_?", "%Domain%-*"));

        assertEquals(Map.of("changed", Map.of("order",
                        change(List.of("%Domain%-*", "%LOB%_?"), List.of("%LOB%_?", "%Domain%-*")))),
                onlyEvent(AuditAction.TAG_TEMPLATES_CHANGED).details());
    }

    @Test
    void announcesTheOrderWhenTheTemplatesKeptChangePlacesAsOneIsAdded() {
        tagTemplateService.save(List.of("%Domain%-*", "%LOB%_?", "%Region%.*"));
        tagTemplateService.save(List.of("%Domain%-*", "%Product%+", "%LOB%_?", "%Region%.*"));
        audit.clear();

        tagTemplateService.save(List.of("%Region%.*", "%Domain%-*", "%LOB%_?"));

        assertEquals(Map.of("added", Set.of(), "removed", Set.of("%Product%+"), "changed", Map.of("order",
                        change(List.of("%Domain%-*", "%Product%+", "%LOB%_?", "%Region%.*"),
                                List.of("%Region%.*", "%Domain%-*", "%LOB%_?")))),
                onlyEvent(AuditAction.TAG_TEMPLATES_CHANGED).details());
    }

    private TagType type(String name) {
        var type = new TagType();
        type.setName(name);
        type.setNullable(true);
        tagTypeService.save(type);
        return type;
    }

    private AuditEvent onlyEvent(AuditAction action) {
        var events = audit.events(action);
        assertEquals(1, events.size());
        return events.getFirst();
    }

    private List<AuditAction> actions() {
        return audit.events().stream().map(AuditEvent::action).toList();
    }

    private static Map<String, Object> change(Object from, Object to) {
        return Map.of("from", from, "to", to);
    }
}
