package org.openl.studio.tags.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagType;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.tags.model.TagTypeDTO;
import org.openl.studio.tags.service.TagFillService;
import org.openl.studio.tags.service.TagService;
import org.openl.studio.tags.service.TagTemplateService;
import org.openl.studio.tags.service.TagTypeService;

/**
 * Changing a tag type and renaming a tag through the administration API.
 *
 * @author Yury Molchan
 */
class TagConfigControllerTypesTest {

    private static final long REGION = 1L;
    private static final long EU = 10L;

    private TagTypeService tagTypeService;
    private TagService tagService;
    private TagConfigController controller;

    @BeforeEach
    void setUp() {
        tagTypeService = mock(TagTypeService.class);
        tagService = mock(TagService.class);
        controller = new TagConfigController(tagTypeService, tagService, mock(TagTemplateService.class),
                mock(TagFillService.class), mock(WorkspaceProjectService.class));
        var region = type(REGION, "Region");
        when(tagTypeService.getById(REGION)).thenReturn(region);
        when(tagTypeService.getByName("Region")).thenReturn(region);
        var eu = new Tag();
        eu.setId(EU);
        eu.setName("EU");
        eu.setType(region);
        when(tagService.getById(EU)).thenReturn(eu);
        when(tagService.getByName(REGION, "EU")).thenReturn(eu);
    }

    @Test
    void changesATagType() {
        when(tagTypeService.update(REGION, "Area", true, true)).thenReturn(true);

        assertEquals(HttpStatus.NO_CONTENT, controller.updateTagType(REGION, dto("Area", true, true)).getStatusCode());
        verify(tagTypeService).update(REGION, "Area", true, true);
    }

    @Test
    void changesATagTypeThatKeepsItsName() {
        when(tagTypeService.update(REGION, "Region", false, false)).thenReturn(true);

        assertEquals(HttpStatus.NO_CONTENT,
                controller.updateTagType(REGION, dto("Region", false, false)).getStatusCode());
    }

    @Test
    void changesNoTagTypeThatDoesNotExistOrWasDeletedMeanwhile() {
        var area = dto("Area", true, false);

        assertThrows(NotFoundException.class, () -> controller.updateTagType(2L, area));
        // Found by the check, and gone by the time it is changed.
        assertThrows(NotFoundException.class, () -> controller.updateTagType(REGION, area));
    }

    @Test
    void givesATagTypeNoNameThatIsTakenOrNotValid() {
        when(tagTypeService.getById(2L)).thenReturn(type(2L, "Product"));
        var taken = dto("Region", true, false);
        var notValid = dto("Region.", true, false);

        assertThrows(ConflictException.class, () -> controller.updateTagType(2L, taken));
        assertThrows(BadRequestException.class, () -> controller.updateTagType(2L, notValid));
        verify(tagTypeService, never()).update(anyLong(), anyString(), anyBoolean(), anyBoolean());
    }

    @Test
    void renamesATag() {
        when(tagService.rename(EU, "Europe")).thenReturn(true);

        assertEquals(HttpStatus.NO_CONTENT, controller.updateTag(REGION, EU, "Europe").getStatusCode());
        verify(tagService).rename(EU, "Europe");
    }

    @Test
    void renamesNoTagThatDoesNotExistOrWasDeletedMeanwhile() {
        assertThrows(NotFoundException.class, () -> controller.updateTag(REGION, 11L, "Europe"));
        // Found by the check, and gone by the time it is renamed.
        assertThrows(NotFoundException.class, () -> controller.updateTag(REGION, EU, "Europe"));
    }

    @Test
    void givesATagNoNameThatIsTaken() {
        var us = new Tag();
        us.setId(11L);
        us.setType(type(REGION, "Region"));
        when(tagService.getById(11L)).thenReturn(us);

        assertThrows(ConflictException.class, () -> controller.updateTag(REGION, 11L, "EU"));
        verify(tagService, never()).rename(any(), any());
    }

    private static TagType type(long id, String name) {
        var type = new TagType();
        type.setId(id);
        type.setName(name);
        return type;
    }

    private static TagTypeDTO dto(String name, boolean nullable, boolean extensible) {
        var dto = new TagTypeDTO();
        dto.setName(name);
        dto.setNullable(nullable);
        dto.setExtensible(extensible);
        return dto;
    }
}
