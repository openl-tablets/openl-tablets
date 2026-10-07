package org.openl.studio.tags.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagType;

/**
 * What the server-side flows may assign: the fill-from-templates and project-import registration match
 * their tags against the catalog, while the project file itself is never validated here.
 */
class TagAssignmentValidatorTest {

    private TagTypeService tagTypeService;
    private TagService tagService;
    private TagAssignmentValidator validator;

    @BeforeEach
    void setUp() {
        tagTypeService = mock(TagTypeService.class);
        tagService = mock(TagService.class);
        var provider = new TagCatalogProvider(tagTypeService, tagService);
        validator = new TagAssignmentValidator(provider, tagService);
    }

    private static TagType type(long id, String name, boolean nullable, boolean extensible) {
        var type = new TagType();
        type.setId(id);
        type.setName(name);
        type.setNullable(nullable);
        type.setExtensible(extensible);
        return type;
    }

    private static Tag tag(TagType type, String name) {
        var tag = new Tag();
        tag.setType(type);
        tag.setName(name);
        return tag;
    }

    private void configured(List<TagType> types, List<Tag> tags) {
        when(tagTypeService.getAllTagTypes()).thenReturn(types);
        when(tagService.getAll()).thenReturn(tags);
    }

    @Test
    void drops_what_it_cannot_assign_instead_of_rejecting_it() {
        var region = type(1, "Region", true, false);
        var team = type(2, "Team", true, true);
        configured(List.of(region, team), List.of(tag(region, "EU")));

        var result = validator.applicable(Map.of("Region", "Mars", "Team", "Payroll", "Ghost", "x"));

        // A value the fixed-value type does not have is left out; the extensible type gains its new one.
        assertEquals(Map.of("Team", "Payroll"), result);
        verify(tagService).save(any());
    }

    @Test
    void extensible_type_takes_a_new_value_and_gains_it() {
        var region = type(1, "Region", true, true);
        configured(List.of(region), List.of(tag(region, "EU")));

        var result = validator.applicable(Map.of("Region", "Pluto"));

        assertEquals(Map.of("Region", "Pluto"), result);
        var created = ArgumentCaptor.forClass(Tag.class);
        verify(tagService).save(created.capture());
        assertEquals("Pluto", created.getValue().getName());
        assertEquals(region, created.getValue().getType());
    }

    @Test
    void a_new_value_that_is_not_a_valid_name_is_dropped_not_rejected() {
        // These flows run after the project is saved, so one bad value must not fail the request.
        var region = type(1, "Region", true, true);
        configured(List.of(region), List.of(tag(region, "EU")));

        assertTrue(validator.applicable(Map.of("Region", "a/b")).isEmpty());
        verify(tagService, never()).save(any());
    }

    @Test
    void blank_value_is_dropped() {
        var region = type(1, "Region", true, true);
        configured(List.of(region), List.of(tag(region, "EU")));

        assertTrue(validator.applicable(Map.of("Region", "  ")).isEmpty());
    }

    @Test
    void value_is_assigned_the_way_it_is_configured() {
        var region = type(1, "Region", true, false);
        configured(List.of(region), List.of(tag(region, "EU")));

        var result = validator.applicable(Map.of("region", "eu"));

        assertEquals(Map.of("Region", "EU"), result);
    }
}
