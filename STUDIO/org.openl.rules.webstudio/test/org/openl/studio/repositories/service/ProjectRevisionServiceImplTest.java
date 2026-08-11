package org.openl.studio.repositories.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.security.acl.repository.RepositoryAclService;

/**
 * A project answers about its own history: it carries the folder its repository holds it under, so nothing is
 * looked up by name. A project that was never published has no history at all.
 */
class ProjectRevisionServiceImplTest {

    private final HistoryRepositoryMapper mapper = mock(HistoryRepositoryMapper.class);

    /** The repository the history was actually read from, as the service asked for it. */
    private final AtomicReference<Repository> askedRepository = new AtomicReference<>();

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
        var project = mock(RulesProject.class);
        when(project.isLocalOnly()).thenReturn(false);
        when(project.getDesignRepository()).thenReturn(mock(Repository.class));
        when(project.getDesignFolderName()).thenReturn("DESIGN/rules/Rates:a1b2");

        var page = Pageable.unpaged();
        service.getProjectRevision(project, "fix", true, page);

        verify(mapper).getProjectHistory("DESIGN/rules/Rates:a1b2", "fix", true, page);
    }

    /**
     * The project carries the branch it is on in its own design repository, and that is the branch whose revisions
     * the user must be shown. Reading them from any other view of the repository would offer revisions from a
     * branch the user is not on, and opening one would move the project onto it.
     */
    @Test
    void theHistoryIsReadFromTheBranchTheProjectIsOn() throws IOException {
        var branchOfTheProject = mock(Repository.class);
        var project = mock(RulesProject.class);
        when(project.isLocalOnly()).thenReturn(false);
        when(project.getDesignRepository()).thenReturn(branchOfTheProject);
        when(project.getDesignFolderName()).thenReturn("DESIGN/rules/Rates:a1b2");

        service.getProjectRevision(project, null, false, Pageable.unpaged());

        assertSame(branchOfTheProject, askedRepository.get());
    }
}
