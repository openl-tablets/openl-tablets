package org.openl.studio.repositories.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.ObjectProvider;

import org.openl.rules.common.ProjectException;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileChange;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.RevisionChanges;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.model.project.status.ChangeType;
import org.openl.studio.repositories.model.ProjectRevisionChanges;
import org.openl.studio.repositories.model.ProjectRevisionChanges.ChangedFile;

/**
 * A revision tells the files of the project it changed: a repository with commits by its trees, one without them by
 * the content of two revisions of the project.
 */
class ProjectRevisionChangesServiceTest {

    private static final String REPOSITORY_ID = "design";
    private static final String FOLDER = "DESIGN/rules/Rates";

    private final DesignTimeRepository designTimeRepository = mock(DesignTimeRepository.class);
    private final HistoryRepositoryMapper history = new HistoryRepositoryMapper(mock(Repository.class));
    @SuppressWarnings("unchecked")
    private final ObjectProvider<HistoryRepositoryMapper> historyMappers = mock(ObjectProvider.class);
    private final ProjectRevisionChangesService service =
            new ProjectRevisionChangesService(designTimeRepository, historyMappers);

    @BeforeEach
    void tellRevisionsAsTheHistoryDoes() {
        when(historyMappers.getObject(ArgumentMatchers.<Object>any())).thenReturn(history);
    }

    @Test
    void aProjectThatWasNeverPublishedHasNoRevision() {
        var project = mock(RulesProject.class);
        when(project.isLocalOnly()).thenReturn(true);

        assertThrows(NotFoundException.class, () -> service.changesOf(project, "r1", null, true));
        verifyNoInteractions(designTimeRepository);
    }

    @Test
    void aRepositoryWithCommitsTellsTheFilesInsideTheProjectByTheirPathsInIt() throws IOException {
        var repository = branchRepository();
        var r1 = revisionData("r1");
        var r2 = revisionData("r2");
        when(repository.listChanges(FOLDER, null, "r2"))
                .thenReturn(new RevisionChanges(r1,
                        r2,
                        List.of(new FileChange(FOLDER + "/rules.xml", FileChange.Kind.MODIFIED),
                                new FileChange(FOLDER + "/rules/Rates.xlsx", FileChange.Kind.ADDED),
                                new FileChange(FOLDER + "/Old.xlsx", FileChange.Kind.DELETED))));

        var changes = service.changesOf(project(repository), "r2", null, true);

        // The workbooks come first, as the Merge Summary lists them; the revisions are told as the history tells them.
        assertRevisions("r1", "r2", changes);
        assertEquals(List.of(new ChangedFile("Old.xlsx", ChangeType.DELETED),
                new ChangedFile("rules/Rates.xlsx", ChangeType.ADDED),
                new ChangedFile("rules.xml", ChangeType.MODIFIED)), changes.files());
    }

    @Test
    void aRepositoryWithCommitsComparesWithTheBaseAskedFor() throws IOException {
        var repository = branchRepository();
        var r1 = revisionData("r1");
        var r3 = revisionData("r3");
        when(repository.listChanges(FOLDER, "r1", "r3")).thenReturn(new RevisionChanges(r1, r3, List.of()));
        named(repository, "r1", "r1");

        var changes = service.changesOf(project(repository), "r3", "r1", true);

        assertRevisions("r1", "r3", changes);
        assertEquals(List.of(), changes.files());
    }

    @Test
    void aRevisionTheRepositoryCannotFindIsNotFound() throws IOException {
        var repository = branchRepository();
        when(repository.listChanges(FOLDER, null, "absent")).thenThrow(new FileNotFoundException("Cannot find revision"));
        var project = project(repository);

        assertThrows(NotFoundException.class, () -> service.changesOf(project, "absent", null, true));
    }

    @Test
    void aRepositoryThatCannotBeReadIsNoMissingRevision() throws IOException {
        var repository = branchRepository();
        var failure = new IOException("Pack file is corrupt");
        when(repository.listChanges(FOLDER, null, "r1")).thenThrow(failure);
        var project = project(repository);

        assertSame(failure, assertThrows(IOException.class, () -> service.changesOf(project, "r1", null, true)));
    }

    @Test
    void aRefusalOfTheRepositoryStaysARefusal() throws IOException {
        var repository = branchRepository();
        when(repository.listChanges(FOLDER, null, "r1")).thenThrow(new AccessDeniedException(FOLDER));
        var project = project(repository);

        assertThrows(AccessDeniedException.class, () -> service.changesOf(project, "r1", null, true));
    }

    @Test
    void aRepositoryWithoutCommitsComparesWithThePreviousRevisionOfTheProject() throws Exception {
        var repository = plainRepository("r1", "r2", "r3");
        var project = project(repository);
        revision("r2", Map.of("rules.xml", "1", "rules/Rates.xlsx", "a", "Old.xlsx", "x"));
        revision("r3", Map.of("rules.xml", "1", "rules/Rates.xlsx", "b", "New.txt", "n"));

        var changes = service.changesOf(project, "r3", null, true);

        assertRevisions("r2", "r3", changes);
        assertEquals(List.of(new ChangedFile("Old.xlsx", ChangeType.DELETED),
                new ChangedFile("rules/Rates.xlsx", ChangeType.MODIFIED),
                new ChangedFile("New.txt", ChangeType.ADDED)), changes.files());
    }

    @Test
    void theFirstRevisionOfAProjectAddsEveryFile() throws Exception {
        var repository = plainRepository("r1", "r2");
        var project = project(repository);
        revision("r1", Map.of("rules.xml", "1"));

        var changes = service.changesOf(project, "r1", null, true);

        assertNull(changes.base());
        assertEquals(List.of(new ChangedFile("rules.xml", ChangeType.ADDED)), changes.files());
    }

    @Test
    void aRevisionThatDeletedTheProjectDeletesEveryFile() throws Exception {
        var repository = plainRepository("r1", "r2");
        var project = project(repository);
        revision("r1", Map.of("rules.xml", "1"));
        var deleted = revision("r2", Map.of());
        when(deleted.isDeleted()).thenReturn(true);

        var changes = service.changesOf(project, "r2", null, true);

        assertEquals(List.of(new ChangedFile("rules.xml", ChangeType.DELETED)), changes.files());
    }

    @Test
    void aRepositoryWithoutCommitsComparesWithTheBaseAskedFor() throws Exception {
        var repository = plainRepository("r1", "r2", "r3");
        var project = project(repository);
        revision("r1", Map.of("rules.xml", "1"));
        revision("r3", Map.of("rules.xml", "2"));

        var changes = service.changesOf(project, "r3", "r1", true);

        assertEquals("r1", changes.base().revisionNo());
        assertEquals(List.of(new ChangedFile("rules.xml", ChangeType.MODIFIED)), changes.files());
    }

    @Test
    void aProjectTheRepositoryNoLongerListsIsNotFound() throws Exception {
        var repository = plainRepository("r1", "r2");
        revision("r1", Map.of("rules.xml", "1"));
        var project = project(repository);

        assertThrows(NotFoundException.class, () -> service.changesOf(project, "r2", null, true));
    }

    @Test
    void aRevisionTheHistoryDoesNotHoldIsNotFound() throws IOException {
        var repository = plainRepository("r1");
        var project = project(repository);

        assertThrows(NotFoundException.class, () -> service.changesOf(project, "absent", null, true));
    }

    @Test
    void theWorkingCopyIsComparedWithTheBaseByTheContentOfItsFiles() throws Exception {
        var repository = branchRepository();
        when(repository.getId()).thenReturn(REPOSITORY_ID);
        named(repository, "r1", "r1");
        var project = project(repository);
        when(project.getBranch()).thenReturn("main");
        // The base is read on the branch the project is on.
        var base = revision("r1", Map.of("rules.xml", "1", "rules/Rates.xlsx", "a", "Old.xlsx", "x"));
        when(designTimeRepository.getProjectByPath(REPOSITORY_ID, "main", FOLDER, "r1")).thenReturn(base);
        holds(project, Map.of("rules.xml", "1", "rules/Rates.xlsx", "b", "New.txt", "n"));

        var changes = service.changesOf(project, null, "r1", true);

        assertEquals("r1", changes.base().revisionNo());
        assertEquals("Comment of r1", changes.base().fullComment());
        // The working copy is no revision of the history.
        assertNull(changes.revision());
        assertEquals(List.of(new ChangedFile("Old.xlsx", ChangeType.DELETED),
                new ChangedFile("rules/Rates.xlsx", ChangeType.MODIFIED),
                new ChangedFile("New.txt", ChangeType.ADDED)), changes.files());
    }

    @Test
    void theWorkingCopyAndARevisionOfAnotherBranchTellTheRevisionTheyPartedAt() throws Exception {
        var repository = workingCopyAgainst("t1");
        when(repository.findCommonRevision(FOLDER, "t1", "w1")).thenReturn(revisionData("c1"));
        when(repository.findConflicts(FOLDER, "t1", "w1")).thenReturn(Set.of(FOLDER + "/rules.xml"));
        revision("c1", Map.of("rules.xml", "0"));
        var project = openedAt(repository, "w1");

        var changes = service.changesOf(project, null, "t1", true);

        assertEquals("t1", changes.base().revisionNo());
        assertEquals("c1", changes.common().revisionNo());
        assertEquals("Comment of c1", changes.common().fullComment());
        // The base holds what the working copy holds; the files are what the working copy changed since both parted
        assertEquals(List.of(new ChangedFile("rules.xml", ChangeType.MODIFIED)), changes.files());
        // A merge of the base and of the revision the working copy was opened at stops at it
        assertEquals(List.of("rules.xml"), changes.conflicts());
    }

    @Test
    void aWorkingCopyOpenedAtARevisionThatIsGoneIsComparedWithTheBase() throws Exception {
        var repository = workingCopyAgainst("t1");
        when(repository.findCommonRevision(FOLDER, "t1", "gone"))
                .thenThrow(new FileNotFoundException("Cannot find revision 'gone'."));
        var project = openedAt(repository, "gone");

        var changes = service.changesOf(project, null, "t1", true);

        // No revision both parted at is known, so the working copy is compared with the base itself
        assertNull(changes.common());
        assertEquals(List.of(), changes.files());
        assertEquals(List.of(), changes.conflicts());
    }

    @Test
    void anEarlierRevisionOfTheBranchOfTheWorkingCopyHasNoRevisionToPartAt() throws Exception {
        var repository = workingCopyAgainst("r1");
        when(repository.findCommonRevision(FOLDER, "r1", "r3")).thenReturn(revisionData("r1"));
        var project = openedAt(repository, "r3");

        assertNull(service.changesOf(project, null, "r1", true).common());
    }

    @Test
    void aLaterRevisionPartsFromTheWorkingCopyAtTheRevisionItWasOpenedAt() throws Exception {
        var repository = workingCopyAgainst("r3");
        when(repository.findCommonRevision(FOLDER, "r3", "r1")).thenReturn(revisionData("r1"));
        revision("r1", Map.of("rules.xml", "1"));
        var project = openedAt(repository, "r1");

        // The changes not saved yet are made on the revision the working copy was opened at.
        assertEquals("r1", service.changesOf(project, null, "r3", true).common().revisionNo());
    }

    @Test
    void twoRevisionsOfTwoBranchesTellTheRevisionTheyPartedAt() throws IOException {
        var repository = branchRepository();
        when(repository.listChanges(FOLDER, "t1", "w2"))
                .thenReturn(new RevisionChanges(revisionData("t1"), revisionData("w2"), List.of()));
        when(repository.findCommonRevision(FOLDER, "t1", "w2")).thenReturn(revisionData("c1"));
        named(repository, "t1", "t1");
        named(repository, "w2", "w2");
        when(repository.listChanges(FOLDER, "c1", "w2")).thenReturn(new RevisionChanges(revisionData("c1"),
                revisionData("w2"), List.of(new FileChange(FOLDER + "/rules.xml", FileChange.Kind.MODIFIED))));
        when(repository.findConflicts(FOLDER, "t1", "w2"))
                .thenReturn(Set.of(FOLDER + "/rules.xml", FOLDER + "/rules/Rates.xlsx"));

        var changes = service.changesOf(project(repository), "w2", "t1", true);

        assertEquals("c1", changes.common().revisionNo());
        // The revisions compared are the ones named; the files are what w2 changed since both parted
        assertEquals("t1", changes.base().revisionNo());
        assertEquals(List.of(new ChangedFile("rules.xml", ChangeType.MODIFIED)), changes.files());
        // By their paths inside the project, workbooks first
        assertEquals(List.of("rules/Rates.xlsx", "rules.xml"), changes.conflicts());
    }

    @Test
    void anAnswerThatLeavesTheConflictsOutDoesNotEstimateThem() throws IOException {
        var repository = branchRepository();
        when(repository.findCommonRevision(FOLDER, "t1", "w2")).thenReturn(revisionData("c1"));
        named(repository, "t1", "t1");
        named(repository, "w2", "w2");
        when(repository.listChanges(FOLDER, "c1", "w2")).thenReturn(new RevisionChanges(revisionData("c1"),
                revisionData("w2"), List.of(new FileChange(FOLDER + "/rules.xml", FileChange.Kind.MODIFIED))));

        var changes = service.changesOf(project(repository), "w2", "t1", false);

        assertEquals("c1", changes.common().revisionNo());
        assertEquals(List.of(), changes.conflicts());
        verify(repository, never()).findConflicts(FOLDER, "t1", "w2");
    }

    @Test
    void twoRevisionsOfOneLineOfHistoryHaveNoRevisionToPartAt() throws IOException {
        var repository = branchRepository();
        when(repository.listChanges(FOLDER, "r1", "r3"))
                .thenReturn(new RevisionChanges(revisionData("r1"), revisionData("r3"), List.of()));
        when(repository.listChanges(FOLDER, "r3", "r1"))
                .thenReturn(new RevisionChanges(revisionData("r3"), revisionData("r1"), List.of()));
        when(repository.findCommonRevision(FOLDER, "r1", "r3")).thenReturn(revisionData("r1"));
        when(repository.findCommonRevision(FOLDER, "r3", "r1")).thenReturn(revisionData("r1"));
        named(repository, "r1", "r1");
        named(repository, "r3", "r3");
        var project = project(repository);

        assertNull(service.changesOf(project, "r3", "r1", true).common());
        // Either one first.
        assertNull(service.changesOf(project, "r1", "r3", true).common());
        // A revision compared with the one it was made on is never compared through another one.
        when(repository.listChanges(FOLDER, null, "r3"))
                .thenReturn(new RevisionChanges(revisionData("r2"), revisionData("r3"), List.of()));
        assertNull(service.changesOf(project, "r3", null, true).common());
    }

    @Test
    void theWorkingCopyIsComparedWithTheLastRevisionOfABranchNamedAsTheBase() throws Exception {
        var repository = workingCopyAgainst("r7");
        named(repository, "master", "r7");
        var project = openedAt(repository, "r7");

        var changes = service.changesOf(project, null, "master", true);

        // The branch names its last revision, which the base is told and read as
        assertEquals("r7", changes.base().revisionNo());
        assertEquals(List.of(), changes.files());
    }

    @Test
    void aBaseARepositoryWithCommitsDoesNotKnowIsNotFoundForTheWorkingCopy() throws IOException {
        var repository = branchRepository();
        when(repository.findCommonRevision(FOLDER, "no-such-branch", "no-such-branch"))
                .thenThrow(new FileNotFoundException("Cannot find revision 'no-such-branch'."));

        var project = project(repository);

        assertThrows(NotFoundException.class, () -> service.changesOf(project, null, "no-such-branch", true));
    }

    @Test
    void aWorkingCopyThatHoldsWhatTheBaseHoldsChangedNothing() throws Exception {
        var repository = plainRepository("r1", "r2");
        when(repository.checkHistory(FOLDER, "r1")).thenReturn(revisionData("r1"));
        var project = project(repository);
        revision("r1", Map.of("rules.xml", "1"));
        holds(project, Map.of("rules.xml", "1"));

        assertEquals(List.of(), service.changesOf(project, null, "r1", true).files());
    }

    @Test
    void theWorkingCopyNeedsTheBaseItIsComparedWith() {
        var project = project(branchRepository());

        assertThrows(BadRequestException.class, () -> service.changesOf(project, null, null, true));
        verifyNoInteractions(designTimeRepository);
    }

    @Test
    void aBaseTheHistoryDoesNotHoldIsNotFoundForTheWorkingCopy() throws IOException {
        var repository = plainRepository("r1");
        var project = project(repository);

        assertThrows(NotFoundException.class, () -> service.changesOf(project, null, "absent", true));
    }

    @Test
    void aProjectThatWasNeverPublishedHasNoBaseForItsWorkingCopy() {
        var project = mock(RulesProject.class);
        when(project.isLocalOnly()).thenReturn(true);

        assertThrows(NotFoundException.class, () -> service.changesOf(project, null, "r1", true));
        verifyNoInteractions(designTimeRepository);
    }

    /**
     * The working copy of the project, holding the given files by their paths inside it, each with its content. Its
     * files carry the folder a repository with mapped folders keeps the project in, which differs from the folder of
     * the project itself; the files are still told by their paths inside the project.
     */
    private static void holds(RulesProject project, Map<String, String> files) throws ProjectException {
        when(project.getArtefactPath()).thenReturn(new ArtefactPathImpl(FOLDER));
        var artefacts = artefactsOf(files, FOLDER + ":1a2b3c");
        when(project.getArtefacts()).thenReturn(artefacts);
    }

    /** Both revisions are told with their numbers and comments, as the history tells them. */
    private static void assertRevisions(String base, String revision, ProjectRevisionChanges changes) {
        assertEquals(base, changes.base().revisionNo());
        assertEquals("Comment of " + base, changes.base().fullComment());
        assertEquals(revision, changes.revision().revisionNo());
        assertEquals("Comment of " + revision, changes.revision().fullComment());
    }

    /** A revision as a repository tells it in its history. */
    private static FileData revisionData(String revision) {
        var data = new FileData();
        data.setName(FOLDER);
        data.setVersion(revision);
        data.setComment("Comment of " + revision);
        return data;
    }

    private static BranchRepository branchRepository() {
        var repository = mock(BranchRepository.class);
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).build());
        return repository;
    }

    /** A repository without commits, holding the given revisions of the project from the first to the last. */
    private static Repository plainRepository(String... revisions) throws IOException {
        var repository = mock(Repository.class);
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).build());
        when(repository.getId()).thenReturn(REPOSITORY_ID);
        var history = new ArrayList<FileData>();
        for (var revision : revisions) {
            history.add(revisionData(revision));
        }
        when(repository.listHistory(FOLDER)).thenReturn(history);
        return repository;
    }

    /**
     * A repository with commits whose base revision of the project holds what the working copy holds: the working copy
     * changed nothing, and only the revision the two parted at is asked about.
     */
    private BranchRepository workingCopyAgainst(String base) throws Exception {
        var repository = branchRepository();
        when(repository.getId()).thenReturn(REPOSITORY_ID);
        named(repository, base, base);
        revision(base, Map.of("rules.xml", "1"));
        return repository;
    }

    /** A repository with commits that reads the given name, a revision or a branch, as the given revision. */
    private static void named(BranchRepository repository, String name, String revision) throws IOException {
        when(repository.findCommonRevision(FOLDER, name, name)).thenReturn(revisionData(revision));
    }

    /** The working copy of the project, opened at a revision and holding what the base of its comparison holds. */
    private static RulesProject openedAt(BranchRepository repository, String opened) throws ProjectException {
        var project = project(repository);
        when(project.getHistoryVersion()).thenReturn(opened);
        holds(project, Map.of("rules.xml", "1"));
        return project;
    }

    private static RulesProject project(Repository repository) {
        var project = mock(RulesProject.class);
        when(project.getDesignRepository()).thenReturn(repository);
        when(project.getRealPath()).thenReturn(FOLDER);
        when(project.getDesignFolderName()).thenReturn(FOLDER);
        when(project.getName()).thenReturn("Rates");
        return project;
    }

    /**
     * The project at a revision, holding the given files by their paths inside it, each with its content. A path with
     * a folder is kept in a folder of the project.
     */
    private AProject revision(String revision, Map<String, String> files) throws Exception {
        var project = mock(AProject.class);
        when(project.getArtefactPath()).thenReturn(new ArtefactPathImpl(FOLDER));
        var artefacts = artefactsOf(files, FOLDER);
        when(project.getArtefacts()).thenReturn(artefacts);
        when(designTimeRepository.getProjectByPath(REPOSITORY_ID, null, FOLDER, revision)).thenReturn(project);
        return project;
    }

    /**
     * The files of a project by their paths inside it, each with its content. A path with a folder is kept in a
     * folder of the project.
     *
     * @param stored the folder the files carry in their paths
     */
    private static List<AProjectArtefact> artefactsOf(Map<String, String> files, String stored)
            throws ProjectException {
        var artefacts = new ArrayList<AProjectArtefact>();
        var inFolder = new ArrayList<AProjectArtefact>();
        for (var file : files.entrySet()) {
            var resource = resource(stored, file.getKey(), file.getValue());
            (file.getKey().contains("/") ? inFolder : artefacts).add(resource);
        }
        var folder = mock(AProjectFolder.class);
        when(folder.getArtefacts()).thenReturn(inFolder);
        artefacts.add(folder);
        return artefacts;
    }

    private static AProjectResource resource(String stored, String path, String content) throws ProjectException {
        var resource = mock(AProjectResource.class);
        when(resource.getArtefactPath()).thenReturn(new ArtefactPathImpl(stored + "/" + path));
        when(resource.getInternalPath()).thenReturn(path);
        when(resource.getContent())
                .thenAnswer(invocation -> new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        return resource;
    }
}
