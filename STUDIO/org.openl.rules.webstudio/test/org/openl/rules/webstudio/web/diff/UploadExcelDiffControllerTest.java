package org.openl.rules.webstudio.web.diff;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.studio.projects.converter.ProjectIdentityConverter;
import org.openl.studio.projects.service.history.ProjectHistoryService;

class UploadExcelDiffControllerTest {

    @Test
    void resetsPreviousComparisonBeforeComparingHistoryVersions(@TempDir Path historyFolder) throws Exception {
        Files.createFile(historyFolder.resolve("first"));
        Files.createFile(historyFolder.resolve("second"));
        var project = mock(RulesProject.class);
        var projectIdentityConverter = mock(ProjectIdentityConverter.class);
        when(projectIdentityConverter.convert("project-id")).thenReturn(project);
        var historyService = mock(ProjectHistoryService.class);
        var versions = List.of(historyFolder.resolve("first").toFile(), historyFolder.resolve("second").toFile());
        when(historyService.getHistoryVersions(project, "Pricing", List.of("first", "second")))
                .thenReturn(versions);
        var controller = spy(new UploadExcelDiffController(projectIdentityConverter, historyService));
        doNothing().when(controller).compare(argThat(files -> files.size() == 2));

        controller.compareVersions("project-id", "  Pricing  ", "first", "second");

        verify(controller).reset();
        verify(historyService).getHistoryVersions(project, "Pricing", List.of("first", "second"));
        verify(controller).compare(argThat(files -> files.get(0).getName().equals("first")
                && files.get(1).getName().equals("second")));
    }
}
