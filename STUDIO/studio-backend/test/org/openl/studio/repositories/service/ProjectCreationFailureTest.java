package org.openl.studio.repositories.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.openl.rules.common.ProjectException;
import org.openl.rules.repository.api.Repository;

/**
 * A project whose files failed to be read or written, such as one a Git server refused to take or an empty ZIP
 * archive, is reported with the target repository and the reason of the failure; any other failure keeps the general
 * message.
 *
 * @author Yury Molchan
 */
class ProjectCreationFailureTest {

    @Test
    void namesTheRepositoryAndTheReasonItGave() {
        var repository = mock(Repository.class);
        when(repository.getName()).thenReturn("Design");
        var refused = new IOException("The Git server refused the changes. It does not let the user 'reader' write.");
        var failure = new ProjectException("Failed to save", new IOException(refused.getMessage(), refused));

        var answer = ProjectCreationService.failed("project.create.failed.message",
                "project.create.reason.message", repository, failure);

        assertEquals("openl.error.409.project.create.reason.message", answer.getErrorCode());
        assertArrayEquals(new Object[]{"Design", refused.getMessage()}, answer.getArgs());
    }

    @Test
    void tellsTheReasonOfAnArchiveThatCannotBeRead() {
        var repository = mock(Repository.class);
        when(repository.getId()).thenReturn("design");
        var empty = new IOException("Cannot create a project from the given file. Zip file is empty.");
        var failure = new ProjectException(empty.getMessage(), empty);

        var answer = ProjectCreationService.failed("project.create.failed.message",
                "project.create.reason.message", repository, failure);

        assertEquals("openl.error.409.project.create.reason.message", answer.getErrorCode());
        assertArrayEquals(new Object[]{"design", empty.getMessage()}, answer.getArgs());
    }

    @Test
    void keepsTheGeneralMessageForAnyOtherFailure() {
        var answer = ProjectCreationService.failed("project.copy.failed.message",
                "project.copy.reason.message", mock(Repository.class), new ProjectException("broken"));

        assertEquals("openl.error.409.project.copy.failed.message", answer.getErrorCode());
        assertNull(answer.getArgs());
    }
}
