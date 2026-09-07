package org.openl.studio.projects.service.merge;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.git.MergeConflictDetails;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.model.merge.ConflictBase;
import org.openl.studio.projects.model.merge.MergeConflictInfo;

class ProjectsMergeConflictsServiceImplTest {

    private static final String FIRST_FILE = "rules/DeletedOurs.xlsx";
    private static final String SECOND_FILE = "rules/DeletedTheirs.xlsx";

    @Test
    void reportsAvailabilityForEveryConflictedFile() throws Exception {
        var context = createContext();
        var repository = context.repository();
        when(repository.checkHistory(SECOND_FILE, "ours")).thenReturn(file(SECOND_FILE));
        when(repository.checkHistory(FIRST_FILE, "theirs")).thenReturn(file(FIRST_FILE));
        when(repository.checkHistory(FIRST_FILE, "base")).thenReturn(file(FIRST_FILE));
        when(repository.checkHistory(SECOND_FILE, "base")).thenReturn(file(SECOND_FILE));

        var result = context.service().getConflictDetails(context.conflict());

        assertTrue(result.oursRevision().exists());
        assertTrue(result.theirsRevision().exists());
        assertFalse(result.fileAvailability().get(FIRST_FILE).ours());
        assertTrue(result.fileAvailability().get(FIRST_FILE).theirs());
        assertTrue(result.fileAvailability().get(FIRST_FILE).base());
        assertTrue(result.fileAvailability().get(SECOND_FILE).ours());
        assertFalse(result.fileAvailability().get(SECOND_FILE).theirs());
        assertTrue(result.fileAvailability().get(SECOND_FILE).base());
    }

    @Test
    void propagatesRepositoryFailureInsteadOfReportingDeletedFiles() throws Exception {
        var context = createContext();
        when(context.repository().checkHistory(anyString(), anyString()))
                .thenThrow(new IOException("Repository is unavailable"));

        assertThrows(IOException.class, () -> context.service().getConflictDetails(context.conflict()));
    }

    @Test
    void rejectsDownloadWhenFileDoesNotExistInRequestedRevision() throws Exception {
        var context = createContext();
        when(context.repository().readHistory(FIRST_FILE, "ours")).thenReturn(null);

        var exception = assertThrows(NotFoundException.class,
                () -> context.service().getConflictFileItem(context.conflict(), FIRST_FILE, ConflictBase.OURS));

        assertEquals("openl.error.404.project.merge.conflict.file.revision.not.found", exception.getErrorCode());
        assertArrayEquals(new Object[]{"ours", FIRST_FILE}, exception.getArgs());
    }

    private TestContext createContext() throws IOException {
        var repository = mock(BranchRepository.class);
        var designTimeRepository = mock(DesignTimeRepository.class);
        var workspace = mock(UserWorkspace.class);
        var project = mock(RulesProject.class);
        var features = mock(Features.class);
        when(repository.getId()).thenReturn("design");
        when(repository.supports()).thenReturn(features);
        when(repository.forBranch("main")).thenReturn(repository);
        when(project.getRepository()).thenReturn(repository);
        when(workspace.getDesignTimeRepository()).thenReturn(designTimeRepository);
        when(workspace.getProjectByPath(anyString(), anyString())).thenReturn(Optional.empty());
        when(designTimeRepository.getRulesLocation()).thenReturn("");
        when(designTimeRepository.getRepository("design")).thenReturn(repository);

        var diffs = new LinkedHashMap<String, String>();
        diffs.put(FIRST_FILE, "diff");
        diffs.put(SECOND_FILE, "diff");
        var conflictDetails = MergeConflictDetails.builder()
                .diffs(diffs)
                .yourCommit("ours")
                .theirCommit("theirs")
                .baseCommit("base")
                .build();
        var conflict = MergeConflictInfo.builder()
                .details(conflictDetails)
                .project(project)
                .mergeBranchFrom("feature")
                .mergeBranchTo("main")
                .currentBranch("main")
                .build();
        var service = new ProjectsMergeConflictsServiceImpl() {
            @Override
            public UserWorkspace getUserWorkspace() {
                return workspace;
            }
        };
        return new TestContext(repository, conflict, service);
    }

    private static FileData file(String name) {
        var file = new FileData();
        file.setName(name);
        return file;
    }

    private record TestContext(BranchRepository repository,
                               MergeConflictInfo conflict,
                               ProjectsMergeConflictsServiceImpl service) {
    }
}
