package org.openl.studio.tags.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.studio.tags.model.TagDTO;
import org.openl.studio.tags.model.TagTypeDTO;
import org.openl.studio.tags.service.TagTypeService;

class TagsControllerTest {

    @Test
    void maps_tag_types_to_read_only_views() {
        var value = new TagDTO();
        value.setName("EU");
        var type = new TagTypeDTO();
        type.setName("Region");
        type.setExtensible(true);
        type.setNullable(false);
        type.setTags(List.of(value));

        var service = mock(TagTypeService.class);
        when(service.getAll()).thenReturn(List.of(type));

        var result = new TagsController(service).getTagTypes();

        assertEquals(1, result.size());
        var view = result.getFirst();
        assertEquals("Region", view.name());
        assertTrue(view.extensible());
        assertFalse(view.nullable());
        assertEquals(List.of("EU"), view.values());
    }
}
