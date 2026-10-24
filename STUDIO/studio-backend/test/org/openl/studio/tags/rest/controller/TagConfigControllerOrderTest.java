package org.openl.studio.tags.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.tags.service.TagFillService;
import org.openl.studio.tags.service.TagService;
import org.openl.studio.tags.service.TagTemplateService;
import org.openl.studio.tags.service.TagTypeService;

/**
 * Arranging the tag types through the administration API.
 *
 * @author Yury Molchan
 */
class TagConfigControllerOrderTest {

    private TagTypeService tagTypeService;
    private TagConfigController controller;

    @BeforeEach
    void setUp() {
        tagTypeService = mock(TagTypeService.class);
        controller = new TagConfigController(tagTypeService, mock(TagService.class), mock(TagTemplateService.class),
                mock(TagFillService.class), mock(WorkspaceProjectService.class));
    }

    @Test
    void arrangesTheTypes() {
        when(tagTypeService.reorder(List.of(3L, 1L))).thenReturn(true);

        assertEquals(HttpStatus.NO_CONTENT, controller.reorderTagTypes(List.of(3L, 1L)).getStatusCode());
    }

    @Test
    void answersNotFoundWhenAnIdNamesNoType() {
        var ids = List.of(3L, 99L);
        when(tagTypeService.reorder(ids)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> controller.reorderTagTypes(ids));
    }
}
