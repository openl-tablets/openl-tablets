package org.openl.studio.tags.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagType;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.tags.model.TagFillPreview.TagFillItem;
import org.openl.studio.tags.model.TagFillState;

/**
 * Filling tags from the project name templates: what it would do to each project, and what it does.
 */
class TagFillServiceTest {

    private TagTemplateService tagTemplateService;
    private TagTypeService tagTypeService;
    private TagService tagService;
    private UserWorkspace workspace;
    private AclProjectsHelper aclProjectsHelper;
    private TagFillService service;

    @BeforeEach
    void setUp() {
        tagTemplateService = mock(TagTemplateService.class);
        tagTypeService = mock(TagTypeService.class);
        tagService = mock(TagService.class);
        workspace = mock(UserWorkspace.class);
        aclProjectsHelper = mock(AclProjectsHelper.class);
        when(aclProjectsHelper.hasPermission(any(RulesProject.class), any())).thenReturn(true);
        var provider = new TagCatalogProvider(tagTypeService, tagService);
        service = new TagFillService(tagTemplateService, provider, new TagAssignmentValidator(provider, tagService), aclProjectsHelper) {
            @Override
            public UserWorkspace getUserWorkspace() {
                return workspace;
            }
        };
    }

    private static TagType type(String name, boolean extensible) {
        var type = new TagType();
        type.setName(name);
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

    private RulesProject project(String name, Map<String, String> tags) {
        var project = mock(RulesProject.class);
        when(project.getBusinessName()).thenReturn(name);
        when(project.getLocalTags()).thenReturn(tags);
        return project;
    }

    private void workspaceHolds(RulesProject... projects) {
        @SuppressWarnings("unchecked")
        var collection = (Collection<RulesProject>) mock(Collection.class);
        when(collection.iterator()).thenReturn(List.of(projects).iterator());
        when(workspace.getProjects()).thenReturn(collection);
    }

    @Test
    void previewSaysWhatHappensToEveryDerivedTag() {
        var domain = type("Domain", false);
        var lob = type("LOB", true);
        var region = type("Region", false);
        var team = type("Team", false);
        configured(List.of(domain, lob, region, team), List.of(tag(domain, "Policy"), tag(team, "Payroll")));
        var project = project("Policy-rules", Map.of("Team", "Payroll"));
        when(tagTemplateService.getTags("Policy-rules")).thenReturn(List.of(
                tag(domain, "Policy"), tag(lob, "Auto"), tag(region, "Mars"), tag(team, "Payroll")));
        workspaceHolds(project);

        var preview = service.preview();

        assertEquals(1, preview.size());
        assertEquals("Policy-rules", preview.get(0).projectName());
        assertTrue(preview.get(0).modifiable());
        var states = preview.get(0).tags().stream()
                .collect(java.util.stream.Collectors.toMap(TagFillItem::type, TagFillItem::state));
        // Configured value; new value of an extensible type; value no fixed-value type has; already assigned.
        assertEquals(TagFillState.ASSIGN, states.get("Domain"));
        assertEquals(TagFillState.CREATE, states.get("LOB"));
        assertEquals(TagFillState.REJECTED, states.get("Region"));
        assertEquals(TagFillState.KEEP, states.get("Team"));
        // Nothing is written by a preview.
        verify(tagService, never()).save(any());
    }

    @Test
    void previewLeavesOutAProjectThatAlreadyCarriesItsTags() {
        var domain = type("Domain", false);
        configured(List.of(domain), List.of(tag(domain, "Policy")));
        var project = project("Policy-rules", Map.of("Domain", "Policy"));
        when(tagTemplateService.getTags("Policy-rules")).thenReturn(List.of(tag(domain, "Policy")));
        workspaceHolds(project);

        assertTrue(service.preview().isEmpty());
    }

    @Test
    void previewMarksAProjectTheUserMayNotWrite() {
        // A closed project is still fillable — only the missing write permission blocks the row.
        var domain = type("Domain", false);
        configured(List.of(domain), List.of(tag(domain, "Policy")));
        var project = project("Policy-rules", Map.of());
        when(aclProjectsHelper.hasPermission(project, BasePermission.WRITE)).thenReturn(false);
        when(tagTemplateService.getTags("Policy-rules")).thenReturn(List.of(tag(domain, "Policy")));
        workspaceHolds(project);

        assertEquals(false, service.preview().get(0).modifiable());
    }

    @Test
    void fillAssignsTheDerivedTags() throws Exception {
        var domain = type("Domain", false);
        configured(List.of(domain), List.of(tag(domain, "Policy")));
        var project = project("Policy-rules", Map.of("LOB", "Auto"));
        when(tagTemplateService.getTags("Policy-rules")).thenReturn(List.of(tag(domain, "Policy")));
        workspaceHolds(project);

        var result = service.fill(null);

        assertEquals(Map.of("updated", 1, "skipped", 0), result);
        // The template tag is added to what the project carries.
        verify(project).saveTags(Map.of("Domain", "Policy", "LOB", "Auto"));
        verify(workspace).refresh();
    }

    @Test
    void fillCreatesTheValueOfAnExtensibleType() throws Exception {
        var domain = type("Domain", true);
        configured(List.of(domain), List.of());
        var project = project("Policy-rules", Map.of());
        when(tagTemplateService.getTags("Policy-rules")).thenReturn(List.of(tag(domain, "Policy")));
        workspaceHolds(project);

        var result = service.fill(null);

        assertEquals(Map.of("updated", 1, "skipped", 0), result);
        verify(project).saveTags(Map.of("Domain", "Policy"));
        verify(tagService).save(any(Tag.class));
    }

    @Test
    void fillSkipsAValueThatCannotBeAssigned() throws Exception {
        var domain = type("Domain", false);
        configured(List.of(domain), List.of());
        var project = project("Policy-rules", Map.of());
        when(tagTemplateService.getTags("Policy-rules")).thenReturn(List.of(tag(domain, "Policy")));
        workspaceHolds(project);

        var result = service.fill(null);

        assertEquals(Map.of("updated", 0, "skipped", 1), result);
        verify(project, never()).saveTags(any());
    }

    @Test
    void fillTouchesOnlyTheProjectsItWasAskedFor() throws Exception {
        var domain = type("Domain", false);
        configured(List.of(domain), List.of(tag(domain, "Policy")));
        var picked = project("Policy-rules", Map.of());
        var other = project("Policy-other", Map.of());
        when(tagTemplateService.getTags("Policy-rules")).thenReturn(List.of(tag(domain, "Policy")));
        when(tagTemplateService.getTags("Policy-other")).thenReturn(List.of(tag(domain, "Policy")));
        workspaceHolds(picked, other);

        var result = service.fill(List.of("Policy-rules"));

        assertEquals(Map.of("updated", 1, "skipped", 1), result);
        verify(picked).saveTags(Map.of("Domain", "Policy"));
        verify(other, never()).saveTags(any());
    }

    @Test
    void fillKeepsGoingWhenOneProjectFails() {
        var domain = type("Domain", false);
        configured(List.of(domain), List.of(tag(domain, "Policy")));
        var broken = mock(RulesProject.class);
        when(broken.getBusinessName()).thenReturn("broken-project");
        when(broken.getLocalTags()).thenThrow(new RuntimeException("broken"));
        when(tagTemplateService.getTags("broken-project")).thenReturn(List.of(tag(domain, "Policy")));
        workspaceHolds(broken);

        var result = service.fill(null);

        assertEquals(Map.of("updated", 0, "skipped", 1), result);
        verify(workspace).refresh();
    }
}
