package org.openl.studio.projects.service.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FileChange;
import org.openl.rules.repository.api.MergeOutcome;
import org.openl.rules.repository.api.MergeScope;
import org.openl.rules.repository.api.MergedWorkbook;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.model.merge.FileChangeView;
import org.openl.studio.projects.model.merge.SheetChangeView;
import org.openl.studio.projects.model.merge.SheetOrigin;
import org.openl.studio.projects.model.project.status.ChangeType;
import org.openl.studio.projects.service.ProjectIdentifierMapper;

/**
 * A merge changes whole branches: the changed files are told under the project whose folder holds them, the project
 * asked for first, and a project the user may not read only counted.
 */
class MergeChangesServiceTest {

    private static final String REPOSITORY = "design";
    private static final String PRICING = "DESIGN/rules/Pricing";
    private static final String MAIN_XLSX = PRICING + "/rules/Main.xlsx";
    private static final String RULES_XML = PRICING + "/rules.xml";

    private AProject pricing;
    private AProject rating;
    private AProject secret;
    private RulesProject current;
    private DesignTimeRepository designRepository;
    private BranchRepository repository;
    private final Set<AProject> unreadable = new HashSet<>();
    private RepositoryAclService aclService;
    private MergeChangesService service;

    @BeforeEach
    void init() {
        pricing = project("Pricing", PRICING);
        rating = project("Rating", "DESIGN/rules/Rating");
        secret = project("Secret", "DESIGN/rules/Secret");
        unreadable.add(secret);

        current = mock(RulesProject.class);
        when(current.getRealPath()).thenReturn(PRICING);
        repository = mock(BranchRepository.class);
        when(current.getDesignRepository()).thenReturn(repository);
        when(repository.getId()).thenReturn(REPOSITORY);

        designRepository = mock(DesignTimeRepository.class);

        aclService = mock(RepositoryAclService.class);
        when(aclService.filterGranted(any(), anyList())).thenAnswer(invocation -> {
            var granted = new HashSet<AProject>(invocation.<Collection<AProject>>getArgument(0));
            granted.removeAll(unreadable);
            return granted;
        });
        var idMapper = mock(ProjectIdentifierMapper.class);
        when(idMapper.map(any())).thenAnswer(invocation -> ProjectIdModel.builder()
                .repository(REPOSITORY)
                .projectName(invocation.<AProject>getArgument(0).getBusinessName())
                .build());

        service = new MergeChangesService(designRepository, aclService, idMapper);
    }

    @Test
    void aScopeListsTheProjectAskedForFirstThenTheOthersThenTheFilesOutsideProjects() throws IOException {
        when(aclService.isGranted(REPOSITORY, "README.md", List.of(BasePermission.READ))).thenReturn(true);
        when(repository.getMergeScope("feature", "main")).thenReturn(new MergeScope(List.of(
                new FileChange("README.md", FileChange.Kind.DELETED),
                new FileChange("DESIGN/rules/Rating/rules.xml", FileChange.Kind.ADDED),
                new FileChange(RULES_XML, FileChange.Kind.MODIFIED),
                new FileChange(MAIN_XLSX, FileChange.Kind.MODIFIED),
                new FileChange("DESIGN/rules/Secret/Secret.xlsx", FileChange.Kind.MODIFIED)),
                Set.of(MAIN_XLSX, RULES_XML)));
        when(designRepository.getBranchProjects(REPOSITORY, "main")).thenReturn(List.of(pricing, secret));
        when(designRepository.getBranchProjects(REPOSITORY, "feature")).thenReturn(List.of(pricing, rating, secret));

        var scope = service.scopeOf(current, repository, "feature", "main");

        var projects = scope.projects();
        assertEquals(3, projects.size());
        var first = projects.getFirst();
        assertEquals("Pricing", first.name());
        assertEquals(PRICING, first.path());
        assertEquals("Pricing", first.id().getProjectName());
        assertTrue(first.current());
        assertEquals(ChangeType.MODIFIED, first.status());
        assertEquals(2, first.changedFiles());
        // Workbooks first.
        assertEquals(List.of(MAIN_XLSX, RULES_XML), first.changedInBoth());

        var second = projects.get(1);
        assertEquals("Rating", second.name());
        assertFalse(second.current());
        assertEquals(ChangeType.ADDED, second.status());
        assertEquals(List.of(), second.changedInBoth());

        var outside = projects.get(2);
        assertNull(outside.name());
        assertNull(outside.id());
        assertNull(outside.status());
        assertEquals(1, outside.changedFiles());

        // Secret is counted, neither named nor listed.
        assertEquals(1, scope.hiddenProjects());
    }

    @Test
    void aProjectOnlyTheTargetHoldsIsDeletedByTheMerge() throws IOException {
        when(repository.getMergeScope("feature", "main")).thenReturn(new MergeScope(List.of(
                new FileChange("DESIGN/rules/Rating/rules.xml", FileChange.Kind.DELETED)), Set.of()));
        when(designRepository.getBranchProjects(REPOSITORY, "main")).thenReturn(List.of(rating));
        when(designRepository.getBranchProjects(REPOSITORY, "feature")).thenReturn(List.of());

        var scope = service.scopeOf(current, repository, "feature", "main");

        assertEquals(ChangeType.DELETED, scope.projects().getFirst().status());
        assertEquals(0, scope.hiddenProjects());
    }

    @Test
    void aSummaryTellsTheFilesOfEachProjectAndTheSheetsOfTheWorkbooksMergedAutomatically() {
        var outcome = new MergeOutcome("b1", "a1", List.of(
                new FileChange(RULES_XML, FileChange.Kind.MODIFIED),
                new FileChange(MAIN_XLSX, FileChange.Kind.MODIFIED),
                new FileChange("DESIGN/rules/Rating/rules.xml", FileChange.Kind.DELETED),
                new FileChange("DESIGN/rules/Secret/Secret.xlsx", FileChange.Kind.ADDED)),
                List.of(new MergedWorkbook(MAIN_XLSX, List.of("Taxes"), List.of("Rates"), List.of("Rules"))));
        when(designRepository.getBranchProjects(REPOSITORY, "main")).thenReturn(List.of(pricing, secret));
        var changes = service.changesOf(REPOSITORY, "feature", "main", outcome, List.of(pricing, rating));

        var summary = service.summaryOf(current, changes);

        assertEquals("feature", summary.sourceBranch());
        assertEquals("main", summary.targetBranch());
        assertEquals("b1", summary.before());
        assertEquals("a1", summary.after());
        assertEquals(1, summary.hiddenProjects());

        var first = summary.projects().getFirst();
        assertTrue(first.current());
        assertEquals(List.of(new FileChangeView(MAIN_XLSX, ChangeType.MODIFIED, List.of(
                        new SheetChangeView("Taxes", SheetOrigin.TARGET),
                        new SheetChangeView("Rates", SheetOrigin.SOURCE),
                        new SheetChangeView("Rules", SheetOrigin.BOTH))),
                new FileChangeView(RULES_XML, ChangeType.MODIFIED, List.of())), first.files());

        var deleted = summary.projects().get(1);
        assertEquals("Rating", deleted.name());
        assertEquals(ChangeType.DELETED, deleted.status());
        assertEquals(List.of(new FileChangeView("DESIGN/rules/Rating/rules.xml", ChangeType.DELETED, List.of())),
                deleted.files());
    }

    @Test
    void aFileNoProjectHoldsIsOutsideEveryProjectEvenUnderAProjectLikePath() {
        var outcome = new MergeOutcome("b1", "a1",
                List.of(new FileChange("DESIGN/rules/Unknown/a.txt", FileChange.Kind.ADDED)),
                List.of());
        when(aclService.isGranted(REPOSITORY, "DESIGN/rules/Unknown/a.txt", List.of(BasePermission.READ)))
                .thenReturn(true);
        when(designRepository.getBranchProjects(REPOSITORY, "main")).thenReturn(List.of(pricing));

        var summary = service.summaryOf(current,
                service.changesOf(REPOSITORY, "feature", "main", outcome, List.of(pricing)));

        var outside = summary.projects().getFirst();
        assertNull(outside.name());
        assertFalse(outside.current());
        assertEquals(0, summary.hiddenProjects());
    }

    @Test
    void aFileOutsideProjectsTheUserMayNotReadIsLeftOut() {
        var outcome = new MergeOutcome("b1", "a1", List.of(
                new FileChange("docs/a.txt", FileChange.Kind.ADDED),
                new FileChange("docs/b.txt", FileChange.Kind.ADDED),
                new FileChange("secret/c.txt", FileChange.Kind.ADDED),
                new FileChange("README.md", FileChange.Kind.MODIFIED)),
                List.of());
        when(aclService.isGranted(REPOSITORY, "docs/a.txt", List.of(BasePermission.READ))).thenReturn(true);
        when(aclService.isGranted(REPOSITORY, "README.md", List.of(BasePermission.READ))).thenReturn(true);

        var summary = service.summaryOf(current, service.changesOf(REPOSITORY, "feature", "main", outcome, List.of()));

        // Each file is asked under its own rights, as the repository asks when it is read: a file of a folder the
        // user may read can be denied on its own.
        assertEquals(List.of("docs/a.txt", "README.md"),
                summary.projects().getFirst().files().stream().map(FileChangeView::path).toList());
    }

    @Test
    void aMergeKeepsOnlyTheProjectsHoldingAChangedFile() {
        var outcome = new MergeOutcome("b1", "a1", List.of(new FileChange(MAIN_XLSX, FileChange.Kind.MODIFIED)),
                List.of());

        when(designRepository.getBranchProjects(REPOSITORY, "main")).thenReturn(List.of(rating, pricing));

        var changes = service.changesOf(REPOSITORY, "feature", "main", outcome, List.of(pricing, rating));

        assertEquals(List.of(pricing), changes.projectsBefore());
        assertEquals(List.of(pricing), changes.projectsAfter());
    }

    @Test
    void aScopeThatCannotBeToldIsLeftOutOfTheCheck() throws IOException {
        when(repository.getMergeScope("feature", "main")).thenThrow(new IOException("Cannot find branch 'feature'."));

        assertNull(service.scopeOf(current, repository, "feature", "main"));
    }

    @Test
    void theProjectsBeforeAMergeThatCannotBeReadLeaveNothingToTell() {
        var outcome = new MergeOutcome("b1", "a1", List.of(new FileChange(MAIN_XLSX, FileChange.Kind.MODIFIED)),
                List.of());
        when(designRepository.getBranchProjects(REPOSITORY, "main")).thenThrow(new IllegalStateException("index"));

        // The merge goes on without the projects: it only tells nothing of what it changed.
        var before = service.projectsBefore(REPOSITORY, "main");

        assertNull(before);
        assertNull(service.changesOf(REPOSITORY, "feature", "main", outcome, before));
    }

    @Test
    void whatACommittedMergeChangedIsLeftOutWhenItCannotBeTold() {
        var outcome = new MergeOutcome("b1", "a1", List.of(new FileChange(MAIN_XLSX, FileChange.Kind.MODIFIED)),
                List.of());
        when(designRepository.getBranchProjects(REPOSITORY, "main")).thenThrow(new IllegalStateException("index"));

        // The merge is committed: telling what it changed must not fail it.
        assertNull(service.changesOf(REPOSITORY, "feature", "main", outcome, List.of(pricing)));
    }

    @Test
    void aSummaryThatCannotBeToldIsLeftOut() {
        var outcome = new MergeOutcome("b1", "a1", List.of(new FileChange(MAIN_XLSX, FileChange.Kind.MODIFIED)),
                List.of());
        when(designRepository.getBranchProjects(REPOSITORY, "main")).thenReturn(List.of(pricing));
        var changes = service.changesOf(REPOSITORY, "feature", "main", outcome, List.of(pricing));
        doThrow(new IllegalStateException("ACL")).when(aclService).filterGranted(any(), anyList());

        assertNull(service.summaryOf(current, changes));
    }

    private static AProject project(String name, String path) {
        var project = mock(AProject.class);
        when(project.getBusinessName()).thenReturn(name);
        when(project.getRealPath()).thenReturn(path);
        return project;
    }
}
