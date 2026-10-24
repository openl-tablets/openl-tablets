package org.openl.studio.tags.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.tags.service.TagFillService;
import org.openl.studio.tags.service.TagService;
import org.openl.studio.tags.service.TagTemplateService;
import org.openl.studio.tags.service.TagTypeService;

/**
 * Defining the tags a project carries through the administration API.
 *
 * @author Yury Molchan
 */
class TagConfigControllerDefineTest {

    private TagTypeService tagTypeService;
    private TagConfigController controller;

    @BeforeEach
    void setUp() {
        tagTypeService = mock(TagTypeService.class);
        controller = new TagConfigController(tagTypeService, mock(TagService.class), mock(TagTemplateService.class),
                mock(TagFillService.class), mock(WorkspaceProjectService.class));
    }

    @Test
    void definesTheTags() {
        var tags = Map.of("Region", List.of("EU", "US"));

        assertEquals(HttpStatus.NO_CONTENT, controller.defineTags(tags).getStatusCode());
        verify(tagTypeService).define(tags);
    }

    @Test
    void definesNothingWhenANameIsEmpty() {
        var tags = Map.of("Region", List.of("EU", " "));

        assertThrows(BadRequestException.class, () -> controller.defineTags(tags));
        verify(tagTypeService, never()).define(any());
    }

    @Test
    void definesNothingWhenATypeHasNoValueList() {
        var tags = new HashMap<String, List<String>>();
        tags.put("Region", null);

        assertThrows(BadRequestException.class, () -> controller.defineTags(tags));
        verify(tagTypeService, never()).define(any());
    }

    @Test
    void definesNothingWhenANameIsNotValid() {
        var tags = Map.of("Region.", List.<String>of());

        assertThrows(BadRequestException.class, () -> controller.defineTags(tags));
        verify(tagTypeService, never()).define(any());
    }
}
