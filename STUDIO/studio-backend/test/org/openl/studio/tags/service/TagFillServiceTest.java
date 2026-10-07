package org.openl.studio.tags.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import org.openl.rules.lock.LockInfo;
import org.openl.rules.project.abstraction.ProjectTags;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagType;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.projects.service.files.FileRoot;
import org.openl.studio.projects.service.files.ProjectFileRootFactory;
import org.openl.studio.projects.validator.ModifyRestrictedException;
import org.openl.studio.projects.validator.ModifyRestriction;
import org.openl.studio.tags.model.TagFillBlocker;
import org.openl.studio.tags.model.TagFillPreview.TagFillItem;
import org.openl.studio.tags.model.TagFillResult;
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

    /** What keeps filling from writing a project now, and the reason the project is reported with. */
    enum Blocker {
        /** Another user is editing the project. */
        LOCKED(TagFillBlocker.locked("jdoe")) {
            @Override
            void block(RulesProject project, FileRoot files) {
                lockedBy(project, "jdoe");
                refuse(files, new ModifyRestrictedException(project, ModifyRestriction.LOCKED));
            }
        },
        /** A lock of the current user outlived their editing. */
        LOCKED_BY_YOU(TagFillBlocker.of(TagFillBlocker.Reason.LOCKED_BY_YOU)) {
            @Override
            void block(RulesProject project, FileRoot files) {
                var lock = lockedBy(project, "admin");
                when(project.isLockedByMe(lock)).thenReturn(true);
                refuse(files, new ModifyRestrictedException(project, ModifyRestriction.LOCKED));
            }
        },
        /** The branch of the project is protected. */
        BRANCH_PROTECTED(TagFillBlocker.branchProtected("main")) {
            @Override
            void block(RulesProject project, FileRoot files) {
                when(project.getBranch()).thenReturn("main");
                refuse(files, new ModifyRestrictedException(project, ModifyRestriction.BRANCH_PROTECTED));
            }
        },
        /** The project could be changed, but the user may not write to it. */
        NO_PERMISSION(TagFillBlocker.of(TagFillBlocker.Reason.NO_PERMISSION)) {
            @Override
            void block(RulesProject project, FileRoot files) {
                refuse(files, new ForbiddenException("default.message"));
            }
        },
        /** A closed project kept as an archive. */
        ARCHIVE(TagFillBlocker.of(TagFillBlocker.Reason.ARCHIVE)) {
            @Override
            void block(RulesProject project, FileRoot files) {
                when(project.isFolder()).thenReturn(false);
            }
        },
        /** An older revision opened to be read. */
        OLDER_REVISION(TagFillBlocker.of(TagFillBlocker.Reason.OLDER_REVISION)) {
            @Override
            void block(RulesProject project, FileRoot files) {
                when(project.isReadingOtherVersion()).thenReturn(true);
            }
        };

        private final TagFillBlocker reported;

        Blocker(TagFillBlocker reported) {
            this.reported = reported;
        }

        abstract void block(RulesProject project, FileRoot files);
    }

    /** The project is locked by the given user. */
    private static LockInfo lockedBy(RulesProject project, String userName) {
        var lock = mock(LockInfo.class);
        when(lock.getLockedBy()).thenReturn(userName);
        when(project.getLockInfo()).thenReturn(lock);
        return lock;
    }

    /** The files mount refuses the change the way it refuses a write into the project. */
    private static void refuse(FileRoot files, RuntimeException refusal) {
        doThrow(refusal).when(files).requireModifiable();
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

    private static TagFillResult updated(String projectName, Map<String, String> tags) {
        return TagFillResult.updated(projectName, tags, Map.of());
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
        assertNull(preview.getFirst().blocker());
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

        // The preview and the fill both say why the project cannot be changed now.
        var preview = service.preview().getFirst();
        assertFalse(preview.modifiable());
        assertEquals(blocker.reported, preview.blocker());
        assertEquals(List.of(TagFillResult.notModifiable("Policy-rules", blocker.reported)),
                service.fill(List.of("Policy-rules")));
        verifyNothingWritten(project);
        // A value derived for a project that does not take it is not created either.
        verify(tagService, never()).save(any());
        verify(workspace, never()).refresh();
    }

    @Test
    void aLockOfSomeoneUnknownNamesNobody() {
        var domain = type("Domain", true);
        configured(List.of(domain), List.of());
        var project = project("Policy-rules", Map.of());
        lockedBy(project, "");
        refuse(filesOf(project), new ModifyRestrictedException(project, ModifyRestriction.LOCKED));
        when(tagTemplateService.getTags("Policy-rules")).thenReturn(List.of(tag(domain, "Policy")));
        workspaceHolds(project);

        assertEquals(new TagFillBlocker(TagFillBlocker.Reason.LOCKED, null, null),
                service.preview().getFirst().blocker());
    }

    @Test
    void fillAssignsTheDerivedTags() throws Exception {
        var domain = type("Domain", false);
        configured(List.of(domain), List.of(tag(domain, "Policy")));
        var project = project("Policy-rules", Map.of("LOB", "Auto"));
        when(tagTemplateService.getTags("Policy-rules")).thenReturn(List.of(tag(domain, "Policy")));
        workspaceHolds(project);

        var result = service.fill(null);

        assertEquals(List.of(updated("Policy-rules", Map.of("Domain", "Policy"))), result);
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

        assertEquals(List.of(updated("Policy-rules", Map.of("Domain", "Policy"))), service.fill(null));
        assertEquals(Map.of("Domain", "Policy"), writtenTags(project));
    }

    @Test
    void fillReportsOnlyTheValuesTheProjectGot() throws Exception {
        var domain = type("Domain", false);
        var lob = type("LOB", false);
        configured(List.of(domain, lob), List.of(tag(domain, "Policy"), tag(lob, "Auto")));
        var project = project("Policy-rules", Map.of("Domain", "policy"));
        when(tagTemplateService.getTags("Policy-rules")).thenReturn(List.of(tag(domain, "Policy"), tag(lob, "Auto")));
        workspaceHolds(project);

        // The project already carries the domain, whatever case it spelled it in.
        assertEquals(List.of(updated("Policy-rules", Map.of("LOB", "Auto"))), service.fill(null));
        assertEquals(Map.of("Domain", "policy", "LOB", "Auto"), writtenTags(project));
    }

    @Test
    void fillReportsNothingForAProjectThatAlreadyCarriesItsTags() {
        var domain = type("Domain", false);
        configured(List.of(domain), List.of(tag(domain, "Policy")));
        var project = project("Policy-rules", Map.of("Domain", "Policy"));
        when(tagTemplateService.getTags("Policy-rules")).thenReturn(List.of(tag(domain, "Policy")));
        workspaceHolds(project);

        assertEquals(List.of(), service.fill(null));
        verifyNothingWritten(project);
        verify(workspace, never()).refresh();
    }

    @Test
    void fillReplacesAValueWhateverCaseTheProjectSpelledItsTagTypeIn() throws Exception {
        var domain = type("Domain", false);
        configured(List.of(domain), List.of(tag(domain, "Policy")));
        var project = project("Policy-rules", new LinkedHashMap<>(Map.of("domain", "Claims")));
        when(tagTemplateService.getTags("Policy-rules")).thenReturn(List.of(tag(domain, "Policy")));
        workspaceHolds(project);

        assertEquals(List.of(updated("Policy-rules", Map.of("Domain", "Policy"))), service.fill(null));
        // The value is replaced, not added under a second spelling of its tag type.
        assertEquals(Map.of("Domain", "Policy"), writtenTags(project));
    }

    @Test
    void aValueThatIsNotAValidTagNameIsNeitherOfferedForCreationNorCreated() {
        var lob = type("LOB", true);
        configured(List.of(lob), List.of());
        var project = project("Policy-Auto.-rules", Map.of());
        when(tagTemplateService.getTags("Policy-Auto.-rules")).thenReturn(List.of(tag(lob, "Auto.")));
        workspaceHolds(project);

        // A value that ends with a dot is no tag name, so the preview does not promise it and the fill reports it.
        assertEquals(TagFillState.REJECTED, service.preview().getFirst().tags().getFirst().state());
        assertEquals(List.of(TagFillResult.nothingToAssign("Policy-Auto.-rules", Map.of("LOB", "Auto."))),
                service.fill(null));
        verify(tagService, never()).save(any());
        verifyNothingWritten(project);
    }

    @Test
    void fillReportsTheMissingValuesTheProjectCouldNotGet() throws Exception {
        var domain = type("Domain", false);
        var region = type("Region", false);
        configured(List.of(domain, region), List.of(tag(domain, "Policy")));
        var project = project("Policy-rules", Map.of());
        when(tagTemplateService.getTags("Policy-rules"))
                .thenReturn(List.of(tag(domain, "Policy"), tag(region, "Mars")));
        workspaceHolds(project);

        // The region has no such value and takes no new ones, so the project gets the domain alone.
        var expected = TagFillResult.updated("Policy-rules", Map.of("Domain", "Policy"), Map.of("Region", "Mars"));
        assertEquals(List.of(expected), service.fill(null));
        assertEquals(Map.of("Domain", "Policy"), writtenTags(project));
    }

    @Test
    void fillCreatesNoValueForATagTheProjectAlreadyCarries() throws Exception {
        var domain = type("Domain", false);
        var lob = type("LOB", true);
        configured(List.of(domain, lob), List.of(tag(domain, "Policy")));
        // The project carries a value its extensible tag type does not list.
        var project = project("Policy-Home-rules", Map.of("LOB", "Home"));
        when(tagTemplateService.getTags("Policy-Home-rules"))
                .thenReturn(List.of(tag(domain, "Policy"), tag(lob, "Home")));
        workspaceHolds(project);

        assertEquals(List.of(updated("Policy-Home-rules", Map.of("Domain", "Policy"))), service.fill(null));
        assertEquals(Map.of("LOB", "Home", "Domain", "Policy"), writtenTags(project));
        verify(tagService, never()).save(any());
    }

    @Test
    void fillCreatesTheValueOfAnExtensibleType() throws Exception {
        var domain = type("Domain", true);
        configured(List.of(domain), List.of());
        var project = project("Policy-rules", Map.of());
        when(tagTemplateService.getTags("Policy-rules")).thenReturn(List.of(tag(domain, "Policy")));
        workspaceHolds(project);

        var result = service.fill(null);

        assertEquals(List.of(updated("Policy-rules", Map.of("Domain", "Policy"))), result);
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

        assertEquals(List.of(TagFillResult.nothingToAssign("Policy-rules", Map.of("Domain", "Policy"))), result);
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

        assertEquals(List.of(TagFillResult.nothingToAssign("Policy-rules", Map.of("Region", "Mars"))),
                service.fill(null));
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

        assertEquals(List.of(updated("Policy-rules", Map.of("Domain", "Policy"))), result);
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

        assertEquals(List.of(TagFillResult.failed("Policy-broken"),
                updated("Policy-rules", Map.of("Domain", "Policy"))), result);
        assertEquals(Map.of("Domain", "Policy"), writtenTags(healthy));
        verify(workspace).refresh();
    }
}
