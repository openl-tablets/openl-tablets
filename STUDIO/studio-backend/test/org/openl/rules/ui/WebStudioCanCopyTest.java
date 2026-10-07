package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.studio.projects.service.ProjectAccessService;

/**
 * The editor toolbar asks {@link WebStudio#getCanCopy()} while a page renders, so it answers for every state
 * the page can be in — including no project and an unreadable repository — rather than failing the render.
 */
class WebStudioCanCopyTest {

    private final ProjectAccessService accessService = mock(ProjectAccessService.class);

    private WebStudio studio(RulesProject project) {
        WebStudio studio = mock(WebStudio.class, CALLS_REAL_METHODS);
        doReturn(project).when(studio).getCurrentProject();
        ReflectionTestUtils.setField(studio, "projectAccessService", accessService);
        return studio;
    }

    @Test
    void offersCopyWhenTheProjectCanBeCopiedOrBranched() {
        var project = mock(RulesProject.class);
        when(accessService.canCopyOrBranch(project)).thenReturn(true);

        assertTrue(studio(project).getCanCopy());
    }

    @Test
    void hidesCopyWhenNeitherIsPermitted() {
        var project = mock(RulesProject.class);
        when(accessService.canCopyOrBranch(project)).thenReturn(false);

        assertFalse(studio(project).getCanCopy());
    }

    // The button is rendered before a project is chosen, so no project simply means nothing to copy.
    @Test
    void hidesCopyWhenNoProjectIsOpen() {
        assertFalse(studio(null).getCanCopy());
    }

    // An unreachable repository must cost the button, not the whole editor page.
    @Test
    void hidesCopyWhenTheRightsCannotBeRead() {
        var project = mock(RulesProject.class);
        when(project.getName()).thenReturn("Rates");
        when(accessService.canCopyOrBranch(project)).thenThrow(new IllegalStateException("repository is down"));

        assertFalse(studio(project).getCanCopy());
    }
}
