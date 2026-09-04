package org.openl.studio.projects.rest.controller;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.studio.projects.model.history.ProjectHistoryItem;
import org.openl.studio.projects.model.history.RestoreProjectHistoryRequest;
import org.openl.studio.projects.service.history.ProjectHistoryService;

class ProjectHistoryControllerTest {

    private final ProjectHistoryService service = mock(ProjectHistoryService.class);
    private final ProjectHistoryController controller = new ProjectHistoryController(service);
    private final RulesProject project = mock(RulesProject.class);

    @Test
    void readsRequestedModuleHistory() {
        var expected = List.of(new ProjectHistoryItem("current", "now", true));
        when(service.getLocalHistory(project, "Pricing")).thenReturn(expected);

        var result = controller.getLocalHistory(project, "  Pricing  ");

        assertSame(expected, result);
    }

    @Test
    void treatsBlankModuleAsOmitted() {
        controller.getLocalHistory(project, "  ");

        verify(service).getLocalHistory(project, null);
    }

    @Test
    void restoresRequestedProjectAndModule() throws Exception {
        var session = mock(HttpSession.class);
        var webStudio = mock(WebStudio.class);

        try (var webStudioUtils = mockStatic(WebStudioUtils.class)) {
            webStudioUtils.when(() -> WebStudioUtils.getWebStudio(session)).thenReturn(webStudio);

            controller.restore(project,
                    "  Pricing  ",
                    new RestoreProjectHistoryRequest("  Revision Version\n"),
                    session);
        }

        verify(service).restore(project, "Pricing", "Revision Version", webStudio);
    }

    @Test
    void deletesOnlyRequestedProjectHistory() throws Exception {
        controller.deleteProjectHistory(project);

        verify(service).deleteProjectHistory(project);
    }

    @Test
    void deletesAllHistoryForAdministrators() throws Exception {
        controller.deleteAllHistory();

        verify(service).deleteAllHistory();
    }
}
