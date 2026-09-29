package org.openl.studio.tags.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import org.openl.rules.project.abstraction.ProjectTags;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagType;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.projects.service.files.FileRoot;
import org.openl.studio.projects.service.files.ProjectFileRootFactory;
import org.openl.studio.tags.model.TagFillPreview.TagFillItem;
import org.openl.studio.tags.model.TagFillState;
import org.openl.util.PropertiesUtils;

/**
 * Filling tags from the project name templates: what it would do to each project, and what it does.
 */
class TagFillServiceTest {

    private TagTemplateService tagTemplateService;
    private TagTypeService tagTypeService;
    private TagService tagService;
    private UserWorkspace workspace;
    private ProjectFileRootFactory projectFileRootFactory;
    private TagFillService service;

    @BeforeEach
    void setUp() {
        tagTemplateService = mock(TagTemplateService.class);
        tagTypeService = mock(TagTypeService.class);
        tagService = mock(TagService.class);
        workspace = mock(UserWorkspace.class);
        projectFileRootFactory = mock(ProjectFileRootFactory.class);
        var provider = new TagCatalogProvider(tagTypeService, tagService);
        service = new TagFillService(tagTemplateService, provider, new TagAssignmentValidator(provider, tagService),
                projectFileRootFactory) {
            @Override
            public UserWorkspace getUserWorkspace() {
                return workspace;
            }
        };
    }

    /** What keeps filling from writing a project now. */
    enum Blocker {
        /** The files of the project cannot be written: another user is editing it, or the user may not. */
        NOT_MODIFIABLE {
            @Override
            void block(RulesProject project, FileRoot files) {
                when(files.isModifiable()).thenReturn(false);
            }
        },
        /** A closed project kept as an archive. */
        ARCHIVE {
            @Override
            void block(RulesProject project, FileRoot files) {
                when(project.isFolder()).thenReturn(false);
            }
        },
        /** An older revision opened to be read. */
        OLDER_REVISION {
            @Override
            void block(RulesProject project, FileRoot files) {
                when(project.isReadingOtherVersion()).thenReturn(true);
            }
        };

        abstract void block(RulesProject project, FileRoot files);
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

    /** A project kept as a folder, whose files are written through a mount that takes the write. */
    private RulesProject project(String name, Map<String, String> tags) {
        var project = mock(RulesProject.class);
        when(project.getBusinessName()).thenReturn(name);
        when(project.getLocalTags()).thenReturn(tags);
        when(project.isFolder()).thenReturn(true);
        var files = mock(FileRoot.class);
        when(files.isModifiable()).thenReturn(true);
        when(projectFileRootFactory.of(project)).thenReturn(files);
        return project;
    }

    private FileRoot filesOf(RulesProject project) {
        return projectFileRootFactory.of(project);
    }

    private void workspaceHolds(RulesProject... projects) {
        when(workspace.getProjects()).thenReturn(List.of(projects));
    }

    /** The tags the fill wrote into the tags file of the project, as one change committed with its comment. */
    private Map<String, String> writtenTags(RulesProject project) throws IOException {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FileItem>> files = ArgumentCaptor.forClass(List.class);
        verify(filesOf(project)).writeBatch(eq(""), files.capture(), eq(ChangesetType.DIFF),
                eq(TagFillService.FILL_COMMENT));
        assertEquals(1, files.getValue().size());
        var file = files.getValue().getFirst();
        assertEquals(ProjectTags.TAGS_FILE_NAME, file.getData().getName());
        var tags = new LinkedHashMap<String, String>();
        PropertiesUtils.load(file.getStream(), tags::put);
        return tags;
    }

    private void verifyNothingWritten(RulesProject project) {
        verify(filesOf(project), never()).writeBatch(any(), any(), any(), any());
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
        assertEquals("Policy-rules", preview.getFirst().projectName());
        assertTrue(preview.getFirst().modifiable());
        var states = preview.getFirst().tags().stream()
                .collect(Collectors.toMap(TagFillItem::type, TagFillItem::state));
        // Configured value; new value of an extensible type; value no fixed-value type has; already assigned.
        assertEquals(TagFillState.ASSIGN, states.get("Domain"));
        assertEquals(TagFillState.CREATE, states.get("LOB"));
        assertEquals(TagFillState.REJECTED, states.get("Region"));
        assertEquals(TagFillState.KEEP, states.get("Team"));
        // Nothing is written by a preview.
        verify(tagService, never()).save(any());
        verifyNothingWritten(project);
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

    @ParameterizedTest
    @EnumSource(Blocker.class)
    void aProjectThatCannotBeWrittenNowIsNeitherOfferedNorFilled(Blocker blocker) {
        var domain = type("Domain", true);
        configured(List.of(domain), List.of());
        var project = project("Policy-rules", Map.of());
        blocker.block(project, filesOf(project));
        when(tagTemplateService.getTags("Policy-rules")).thenReturn(List.of(tag(domain, "Policy")));
        workspaceHolds(project);

        assertFalse(service.preview().getFirst().modifiable());
        assertEquals(Map.of("updated", 0, "skipped", 1), service.fill(List.of("Policy-rules")));
        verifyNothingWritten(project);
        // A value derived for a project that does not take it is not created either.
        verify(tagService, never()).save(any());
        verify(workspace, never()).refresh();
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
        assertEquals(Map.of("LOB", "Auto", "Domain", "Policy"), writtenTags(project));
        verify(workspace).refresh();
    }

    @Test
    void fillReplacesTheValueTheTemplateDerivesAnotherOneFor() throws Exception {
        var domain = type("Domain", false);
        configured(List.of(domain), List.of(tag(domain, "Policy")));
        var project = project("Policy-rules", Map.of("Domain", "Claims"));
        when(tagTemplateService.getTags("Policy-rules")).thenReturn(List.of(tag(domain, "Policy")));
        workspaceHolds(project);

        service.fill(null);

        assertEquals(Map.of("Domain", "Policy"), writtenTags(project));
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
        assertEquals(Map.of("Domain", "Policy"), writtenTags(project));
        verify(tagService).save(any(Tag.class));
    }

    @Test
    void fillSkipsAValueThatCannotBeAssigned() {
        var domain = type("Domain", false);
        configured(List.of(domain), List.of());
        var project = project("Policy-rules", Map.of());
        when(tagTemplateService.getTags("Policy-rules")).thenReturn(List.of(tag(domain, "Policy")));
        workspaceHolds(project);

        var result = service.fill(null);

        assertEquals(Map.of("updated", 0, "skipped", 1), result);
        verifyNothingWritten(project);
    }

    @Test
    void fillWritesNothingWhenTheProjectWouldNotChange() {
        // The project carries the one value that can be assigned; the other one cannot be.
        var domain = type("Domain", false);
        var region = type("Region", false);
        configured(List.of(domain, region), List.of(tag(domain, "Policy")));
        var project = project("Policy-rules", Map.of("Domain", "Policy"));
        when(tagTemplateService.getTags("Policy-rules"))
                .thenReturn(List.of(tag(domain, "Policy"), tag(region, "Mars")));
        workspaceHolds(project);

        assertEquals(Map.of("updated", 0, "skipped", 1), service.fill(null));
        verifyNothingWritten(project);
        verify(workspace, never()).refresh();
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
        assertEquals(Map.of("Domain", "Policy"), writtenTags(picked));
        verifyNothingWritten(other);
    }

    @Test
    void fillKeepsGoingWhenOneProjectFails() throws Exception {
        var domain = type("Domain", false);
        configured(List.of(domain), List.of(tag(domain, "Policy")));
        var broken = project("Policy-broken", Map.of());
        var brokenFiles = filesOf(broken);
        doThrow(new ConflictException("file.archive.upload.failed.message"))
                .when(brokenFiles).writeBatch(any(), any(), any(), any());
        var healthy = project("Policy-rules", Map.of());
        when(tagTemplateService.getTags("Policy-broken")).thenReturn(List.of(tag(domain, "Policy")));
        when(tagTemplateService.getTags("Policy-rules")).thenReturn(List.of(tag(domain, "Policy")));
        workspaceHolds(broken, healthy);

        var result = service.fill(null);

        assertEquals(Map.of("updated", 1, "skipped", 1), result);
        assertEquals(Map.of("Domain", "Policy"), writtenTags(healthy));
        verify(workspace).refresh();
    }
}
