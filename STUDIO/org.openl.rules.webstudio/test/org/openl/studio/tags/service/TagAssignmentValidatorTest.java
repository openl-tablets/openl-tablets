package org.openl.studio.tags.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagType;
import org.openl.studio.common.exception.BadRequestException;

class TagAssignmentValidatorTest {

    private TagTypeService tagTypeService;
    private TagService tagService;
    private TagAssignmentValidator validator;

    @BeforeEach
    void setUp() {
        tagTypeService = mock(TagTypeService.class);
        tagService = mock(TagService.class);
        validator = new TagAssignmentValidator(tagTypeService, tagService);
    }

    private static TagType type(long id, String name, boolean nullable, boolean extensible) {
        var type = new TagType();
        type.setId(id);
        type.setName(name);
        type.setNullable(nullable);
        type.setExtensible(extensible);
        return type;
    }

    @Test
    void mandatory_tag_without_value_is_rejected() {
        when(tagTypeService.getAllTagTypes()).thenReturn(List.of(type(1, "Region", false, false)));
        Map<String, String> tags = Map.of();

        assertThrows(BadRequestException.class, () -> validator.sanitize(tags));
    }

    @Test
    void unknown_tag_type_with_value_is_rejected() {
        when(tagTypeService.getAllTagTypes()).thenReturn(List.of());
        var tags = Map.of("Ghost", "x");

        assertThrows(BadRequestException.class, () -> validator.sanitize(tags));
    }

    @Test
    void non_extensible_unknown_value_is_rejected() {
        when(tagTypeService.getAllTagTypes()).thenReturn(List.of(type(1, "Region", true, false)));
        when(tagService.getByTypeNameAndName("Region", "Mars")).thenReturn(null);
        var tags = Map.of("Region", "Mars");

        assertThrows(BadRequestException.class, () -> validator.sanitize(tags));
    }

    @Test
    void invalid_name_is_rejected() {
        when(tagTypeService.getAllTagTypes()).thenReturn(List.of(type(1, "Region", true, true)));
        var tags = Map.of("Region", "bad/name");

        assertThrows(BadRequestException.class, () -> validator.sanitize(tags));
    }

    @Test
    void blank_value_is_dropped_for_nullable_type() {
        when(tagTypeService.getAllTagTypes()).thenReturn(List.of(type(1, "Region", true, true)));

        var result = validator.sanitize(Map.of("Region", "  "));

        assertTrue(result.isEmpty());
        verify(tagService, never()).save(any());
    }

    @Test
    void known_value_passes_without_registration() {
        when(tagTypeService.getAllTagTypes()).thenReturn(List.of(type(1, "Region", true, false)));
        when(tagService.getByTypeNameAndName("Region", "EU")).thenReturn(new Tag());
        when(tagService.getByName(1L, "EU")).thenReturn(new Tag());

        var result = validator.sanitize(Map.of("Region", "EU"));

        assertEquals(Map.of("Region", "EU"), result);
        verify(tagService, never()).save(any());
    }

    @Test
    void new_extensible_value_is_registered() {
        when(tagTypeService.getAllTagTypes()).thenReturn(List.of(type(1, "Region", true, true)));
        when(tagService.getByTypeNameAndName(eq("Region"), any())).thenReturn(null);
        when(tagService.getByName(anyLong(), any())).thenReturn(null);

        var result = validator.sanitize(Map.of("Region", "Pluto"));

        assertEquals(Map.of("Region", "Pluto"), result);
        verify(tagService).save(any(Tag.class));
    }
}
