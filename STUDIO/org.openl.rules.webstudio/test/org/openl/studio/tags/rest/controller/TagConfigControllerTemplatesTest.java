package org.openl.studio.tags.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagType;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.tags.service.TagService;
import org.openl.studio.tags.service.TagTemplateService;
import org.openl.studio.tags.service.TagTypeService;

class TagConfigControllerTemplatesTest {

    private TagTemplateService tagTemplateService;
    private TagConfigController controller;
    private UserWorkspace workspace;

    @BeforeEach
    void setUp() {
        TagTypeService tagTypeService = mock(TagTypeService.class);
        TagService tagService = mock(TagService.class);
        tagTemplateService = mock(TagTemplateService.class);
        workspace = mock(UserWorkspace.class);

        controller = new TagConfigController(tagTypeService, tagService, tagTemplateService) {
            @Override
            public UserWorkspace getUserWorkspace() {
                return workspace;
            }
        };
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
        when(tagTemplateService.getTemplates()).thenReturn(Collections.emptyList());

        var result = controller.getTemplates();

        assertEquals(Collections.emptyList(), result);
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

    @Test
    void testFillTagsForProjects_updatesMatchingProjects() throws Exception {
        var tagType = new TagType();
        tagType.setName("Domain");
        var tag = new Tag();
        tag.setType(tagType);
        tag.setName("Policy");

        RulesProject project = mock(RulesProject.class);
        when(project.getBusinessName()).thenReturn("Policy-rules");
        when(project.getLocalTags()).thenReturn(Collections.emptyMap());
        when(tagTemplateService.getTags("Policy-rules")).thenReturn(List.of(tag));

        @SuppressWarnings("unchecked")
        Collection<RulesProject> projects = (Collection<RulesProject>) mock(Collection.class);
        when(projects.iterator()).thenReturn(List.of(project).iterator());
        when(workspace.getProjects()).thenReturn(projects);

        var result = controller.fillTagsForProjects();

        assertEquals(Map.of("updated", 1, "skipped", 0), result);
        verify(project).saveTags(Map.of("Domain", "Policy"));
        verify(workspace).refresh();
    }

    @Test
    void testFillTagsForProjects_skipsNonMatchingProjects() {
        RulesProject project = mock(RulesProject.class);
        when(project.getBusinessName()).thenReturn("unmatched");
        when(tagTemplateService.getTags("unmatched")).thenReturn(Collections.emptyList());

        @SuppressWarnings("unchecked")
        Collection<RulesProject> projects = (Collection<RulesProject>) mock(Collection.class);
        when(projects.iterator()).thenReturn(List.of(project).iterator());
        when(workspace.getProjects()).thenReturn(projects);

        var result = controller.fillTagsForProjects();

        assertEquals(Map.of("updated", 0, "skipped", 1), result);
        verify(workspace).refresh();
    }

    @Test
    void testFillTagsForProjects_mergesWithExistingTags() throws Exception {
        var tagType = new TagType();
        tagType.setName("Domain");
        var tag = new Tag();
        tag.setType(tagType);
        tag.setName("Policy");

        RulesProject project = mock(RulesProject.class);
        when(project.getBusinessName()).thenReturn("Policy-rules");
        when(project.getLocalTags()).thenReturn(Map.of("LOB", "Auto"));
        when(tagTemplateService.getTags("Policy-rules")).thenReturn(List.of(tag));

        @SuppressWarnings("unchecked")
        Collection<RulesProject> projects = (Collection<RulesProject>) mock(Collection.class);
        when(projects.iterator()).thenReturn(List.of(project).iterator());
        when(workspace.getProjects()).thenReturn(projects);

        var result = controller.fillTagsForProjects();

        assertEquals(Map.of("updated", 1, "skipped", 0), result);
        verify(project).saveTags(Map.of("Domain", "Policy", "LOB", "Auto"));
    }

    @Test
    void testFillTagsForProjects_handlesExceptionGracefully() {
        RulesProject project = mock(RulesProject.class);
        when(project.getBusinessName()).thenReturn("broken-project");
        when(project.getLocalTags()).thenThrow(new RuntimeException("broken"));
        when(tagTemplateService.getTags("broken-project")).thenReturn(List.of(createTag("Domain", "X")));

        @SuppressWarnings("unchecked")
        Collection<RulesProject> projects = (Collection<RulesProject>) mock(Collection.class);
        when(projects.iterator()).thenReturn(List.of(project).iterator());
        when(workspace.getProjects()).thenReturn(projects);

        var result = controller.fillTagsForProjects();

        assertEquals(Map.of("updated", 0, "skipped", 1), result);
        verify(workspace).refresh();
    }

    private Tag createTag(String typeName, String tagName) {
        var tagType = new TagType();
        tagType.setName(typeName);
        var tag = new Tag();
        tag.setType(tagType);
        tag.setName(tagName);
        return tag;
    }
}
