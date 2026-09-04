package org.openl.studio.tags.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.tags.service.TagFillService;
import org.openl.studio.tags.service.TagService;
import org.openl.studio.tags.service.TagTemplateService;
import org.openl.studio.tags.service.TagTypeService;

class TagConfigControllerTemplatesTest {

    private TagTemplateService tagTemplateService;
    private TagConfigController controller;
    private TagTypeService tagTypeService;
    private TagService tagService;

    @BeforeEach
    void setUp() {
        tagTypeService = mock(TagTypeService.class);
        tagService = mock(TagService.class);
        tagTemplateService = mock(TagTemplateService.class);
        controller = new TagConfigController(tagTypeService, tagService, tagTemplateService,
                mock(TagFillService.class));
    }

    @Test
    void testGetTemplates_returnsTemplateList() {
        var expected = List.of("%Domain%-*", "%LOB%_?");
        when(tagTemplateService.getTemplates()).thenReturn(expected);

        var result = controller.getTemplates();

        assertEquals(expected, result);
        verify(tagTemplateService).getTemplates();
    }

    @Test
    void testGetTemplates_empty() {
        when(tagTemplateService.getTemplates()).thenReturn(List.of());

        var result = controller.getTemplates();

        assertEquals(List.of(), result);
    }

    @Test
    void testSaveTemplates_validTemplates() {
        var templates = List.of("%Domain%-*", "%LOB%_?");
        when(tagTemplateService.validate("%Domain%-*")).thenReturn(null);
        when(tagTemplateService.validate("%LOB%_?")).thenReturn(null);

        controller.saveTemplates(templates);

        verify(tagTemplateService).save(templates);
    }

    @Test
    void testSaveTemplates_skipsBlankTemplates() {
        var templates = List.of("%Domain%-*", "", "  ");
        when(tagTemplateService.validate("%Domain%-*")).thenReturn(null);

        controller.saveTemplates(templates);

        verify(tagTemplateService).save(templates);
        verify(tagTemplateService).validate("%Domain%-*");
    }

    @Test
    void testSaveTemplates_invalidTemplate_throwsBadRequest() {
        var templates = List.of("%NonExistent%-*");
        when(tagTemplateService.validate("%NonExistent%-*"))
                .thenReturn("Cannot find tag type 'NonExistent'.");

        assertThrows(BadRequestException.class, () -> controller.saveTemplates(templates));
        verify(tagTemplateService, never()).save(any());
    }

}
