package org.openl.studio.tags.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.studio.projects.model.FacetCount;
import org.openl.studio.projects.model.TagFacetSummary;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.tags.service.TagFillService;
import org.openl.studio.tags.service.TagService;
import org.openl.studio.tags.service.TagTemplateService;
import org.openl.studio.tags.service.TagTypeService;

/**
 * Reading the tags the projects carry through the administration API.
 *
 * @author Yury Molchan
 */
class TagConfigControllerProjectTagsTest {

    @Test
    void listsTheTagsTheProjectsCarry() {
        var projectService = mock(WorkspaceProjectService.class);
        var carried = List.of(new TagFacetSummary("Region", List.of(new FacetCount("EU", "EU", 3))));
        when(projectService.getTagCounts(any())).thenReturn(carried);
        var controller = new TagConfigController(mock(TagTypeService.class), mock(TagService.class),
                mock(TagTemplateService.class), mock(TagFillService.class), projectService);

        assertEquals(carried, controller.getProjectTags());
    }
}
