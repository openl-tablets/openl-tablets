package org.openl.studio.repositories.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
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
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileChange;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.RevisionChanges;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
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
    private final HistoryRepositoryMapper history = new HistoryRepositoryMapper(mock(Repository.class), comments());
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

        assertThrows(NotFoundException.class, () -> service.changesOf(project, "r1", null));
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

        var changes = service.changesOf(project(repository), "r2", null);

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

        var changes = service.changesOf(project(repository), "r3", "r1");

        assertRevisions("r1", "r3", changes);
        assertEquals(List.of(), changes.files());
    }

    @Test
    void aRevisionTheRepositoryCannotFindIsNotFound() throws IOException {
        var repository = branchRepository();
        when(repository.listChanges(FOLDER, null, "absent")).thenThrow(new FileNotFoundException("Cannot find revision"));
        var project = project(repository);

        assertThrows(NotFoundException.class, () -> service.changesOf(project, "absent", null));
    }

    @Test
    void aRepositoryThatCannotBeReadIsNoMissingRevision() throws IOException {
        var repository = branchRepository();
        var failure = new IOException("Pack file is corrupt");
        when(repository.listChanges(FOLDER, null, "r1")).thenThrow(failure);
        var project = project(repository);

        assertSame(failure, assertThrows(IOException.class, () -> service.changesOf(project, "r1", null)));
    }

    @Test
    void aRefusalOfTheRepositoryStaysARefusal() throws IOException {
        var repository = branchRepository();
        when(repository.listChanges(FOLDER, null, "r1")).thenThrow(new AccessDeniedException(FOLDER));
        var project = project(repository);

        assertThrows(AccessDeniedException.class, () -> service.changesOf(project, "r1", null));
    }

    @Test
    void aRepositoryWithoutCommitsComparesWithThePreviousRevisionOfTheProject() throws Exception {
        var repository = plainRepository("r1", "r2", "r3");
        var project = project(repository);
        revision("r2", Map.of("rules.xml", "1", "rules/Rates.xlsx", "a", "Old.xlsx", "x"));
        revision("r3", Map.of("rules.xml", "1", "rules/Rates.xlsx", "b", "New.txt", "n"));

        var changes = service.changesOf(project, "r3", null);

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

        var changes = service.changesOf(project, "r1", null);

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

        var changes = service.changesOf(project, "r2", null);

        assertEquals(List.of(new ChangedFile("rules.xml", ChangeType.DELETED)), changes.files());
    }

    @Test
    void aRepositoryWithoutCommitsComparesWithTheBaseAskedFor() throws Exception {
        var repository = plainRepository("r1", "r2", "r3");
        var project = project(repository);
        revision("r1", Map.of("rules.xml", "1"));
        revision("r3", Map.of("rules.xml", "2"));

        var changes = service.changesOf(project, "r3", "r1");

        assertEquals("r1", changes.base().revisionNo());
        assertEquals(List.of(new ChangedFile("rules.xml", ChangeType.MODIFIED)), changes.files());
    }

    @Test
    void aProjectTheRepositoryNoLongerListsIsNotFound() throws Exception {
        var repository = plainRepository("r1", "r2");
        revision("r1", Map.of("rules.xml", "1"));
        var project = project(repository);

        assertThrows(NotFoundException.class, () -> service.changesOf(project, "r2", null));
    }

    @Test
    void aRevisionTheHistoryDoesNotHoldIsNotFound() throws IOException {
        var repository = plainRepository("r1");
        var project = project(repository);

        assertThrows(NotFoundException.class, () -> service.changesOf(project, "absent", null));
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

    private static Comments comments() {
        var comments = mock(Comments.class);
        when(comments.getCommentParts(ArgumentMatchers.any())).thenReturn(List.of());
        return comments;
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
        var artefacts = new ArrayList<AProjectArtefact>();
        var folder = mock(AProjectFolder.class);
        var inFolder = new ArrayList<AProjectArtefact>();
        for (var file : files.entrySet()) {
            var resource = resource(file.getKey(), file.getValue());
            (file.getKey().contains("/") ? inFolder : artefacts).add(resource);
        }
        when(folder.getArtefacts()).thenReturn(inFolder);
        artefacts.add(folder);
        when(project.getArtefacts()).thenReturn(artefacts);
        when(designTimeRepository.getProjectByPath(REPOSITORY_ID, null, FOLDER, revision)).thenReturn(project);
        return project;
    }

    private static AProjectResource resource(String path, String content) throws ProjectException {
        var resource = mock(AProjectResource.class);
        when(resource.getArtefactPath()).thenReturn(new ArtefactPathImpl(FOLDER + "/" + path));
        when(resource.getContent())
                .thenAnswer(invocation -> new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        return resource;
    }
}
