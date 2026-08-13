package org.openl.studio.repositories.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.exception.BadRequestException;

/**
 * A project answers about its own history: it carries the folder its repository holds it under, so nothing is
 * looked up by name. A project that was never published has no history at all.
 */
class ProjectRevisionServiceImplTest {

    private final HistoryRepositoryMapper mapper = mock(HistoryRepositoryMapper.class);

    /** The repository the history was actually read from, as the service asked for it. */
    private final AtomicReference<Repository> askedRepository = new AtomicReference<>();

    private static final String DESIGN_FOLDER = "DESIGN/rules/Rates:a1b2";

    private final ProjectRevisionServiceImpl service = new ProjectRevisionServiceImpl(mock(DesignTimeRepository.class),
            mock(RepositoryAclService.class)) {
        @Override
        protected HistoryRepositoryMapper getHistoryRepositoryMapper(Repository repository) {
            askedRepository.set(repository);
            return mapper;
        }
    };

    @Test
    void aProjectThatWasNeverPublishedHasNoHistory() throws IOException {
        var project = mock(RulesProject.class);
        when(project.isLocalOnly()).thenReturn(true);

        var revisions = service.getProjectRevision(project, null, false, Pageable.unpaged());

        assertTrue(revisions.getContent().isEmpty());
        assertEquals(0L, revisions.getTotal());
        // Nothing is asked of the repository, so a workspace-only project cannot fail on one.
        verifyNoInteractions(mapper);
    }

    // EPBDS-16432: a rename in rules.xml that is not saved yet moves the name the user sees, not the folder the
    // repository holds the project under, and the history is read from that folder.
    @Test
    void aPublishedProjectIsAskedAboutByItsFolder() throws IOException {
        var project = publishedProject(mock(Repository.class));

        var page = Pageable.unpaged();
        service.getProjectRevision(project, "fix", true, page);

        verify(mapper).getProjectHistory(DESIGN_FOLDER, "fix", true, page);
    }

    /**
     * A file's history is the project folder's history narrowed to the file, so it is asked about by the path the
     * repository holds it under rather than by the path the user typed.
     *
     * <p>The trailing path of the request arrives with a leading slash, so an absolute-looking path names a file
     * inside the project rather than one outside it, and the project root names the project itself.
     */
    @ParameterizedTest
    @CsvSource({
            "/rules/Main.xlsx, DESIGN/rules/Rates:a1b2/rules/Main.xlsx",
            "/etc/passwd,      DESIGN/rules/Rates:a1b2/etc/passwd",
            "/,                DESIGN/rules/Rates:a1b2",
    })
    void aFileIsAskedAboutByItsPathUnderTheProjectFolder(String path, String expectedPath) throws IOException {
        var project = publishedProject(mock(Repository.class));

        var page = Pageable.unpaged();
        service.getFileRevision(project, path, null, false, page);

        verify(mapper).getProjectHistory(expectedPath, null, false, page);
    }

    // The path is anchored to the project folder, so a path that climbs out of it must never reach the
    // repository — the contract is enforced here rather than relied upon from the container.
    @ParameterizedTest
    @ValueSource(strings = {"../sibling/rules.xml", "..", "./rules/Main.xlsx", "rules/../../escape.xlsx", "rules/.."})
    void aPathThatLeavesTheProjectFolderIsRefused(String path) {
        var project = mock(RulesProject.class);
        when(project.isLocalOnly()).thenReturn(false);
        var page = Pageable.unpaged();

        assertThrows(BadRequestException.class, () -> service.getFileRevision(project, path, null, false, page));
        verifyNoInteractions(mapper);
    }

    @Test
    void aFileOfAProjectThatWasNeverPublishedHasNoHistory() throws IOException {
        var project = mock(RulesProject.class);
        when(project.isLocalOnly()).thenReturn(true);

        var revisions = service.getFileRevision(project, "rules/Main.xlsx", null, false, Pageable.unpaged());

        assertTrue(revisions.getContent().isEmpty());
        verifyNoInteractions(mapper);
    }

    /**
     * The project carries the branch it is on in its own design repository, and that is the branch whose revisions
     * the user must be shown. Reading them from any other view of the repository would offer revisions from a
     * branch the user is not on, and opening one would move the project onto it.
     */
    @Test
    void theHistoryIsReadFromTheBranchTheProjectIsOn() throws IOException {
        var branchOfTheProject = mock(Repository.class);
        var project = publishedProject(branchOfTheProject);

        service.getProjectRevision(project, null, false, Pageable.unpaged());

        assertSame(branchOfTheProject, askedRepository.get());
    }

    /** A published project the given repository holds under {@link #DESIGN_FOLDER}. */
    private static RulesProject publishedProject(Repository repository) {
        var project = mock(RulesProject.class);
        when(project.isLocalOnly()).thenReturn(false);
        when(project.getDesignRepository()).thenReturn(repository);
        when(project.getDesignFolderName()).thenReturn(DESIGN_FOLDER);
        return project;
    }
}
