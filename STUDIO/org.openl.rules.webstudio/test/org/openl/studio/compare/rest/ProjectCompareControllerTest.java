package org.openl.studio.compare.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.compare.model.CompareProjectFilesRequest;
import org.openl.studio.compare.model.ComparisonSideRequest;
import org.openl.studio.compare.model.ComparisonStartedView;
import org.openl.studio.compare.service.ComparisonContent;
import org.openl.studio.compare.service.ComparisonLauncher;
import org.openl.studio.compare.service.ProjectComparisonService;

class ProjectCompareControllerTest {

    private final ProjectComparisonService comparisonService = mock(ProjectComparisonService.class);
    private final ComparisonLauncher launcher = mock(ComparisonLauncher.class);
    private final ProjectCompareController controller = new ProjectCompareController(comparisonService, launcher);
    private final RulesProject project = mock(RulesProject.class);

    @Test
    void listsTheFilesOfTheRevisionAskedAbout() {
        var expected = List.of("rules/Rates.xlsx");
        when(comparisonService.excelFiles(project, "master", "rev-1")).thenReturn(expected);

        var files = controller.getFiles(project, "master", "rev-1");

        assertSame(expected, files);
    }

    @Test
    void putsTheFirstFileDownWhenTheSecondCannotBeRead() {
        var closed = new AtomicBoolean();
        var first = new ComparisonSideRequest("rules/Rates.xlsx", null, null);
        var second = new ComparisonSideRequest("rules/Gone.xlsx", "master", "rev-1");
        when(comparisonService.read(project, first)).thenReturn(new ComparisonContent("Rates.xlsx",
                new ByteArrayInputStream(new byte[0]) {
                    @Override
                    public void close() {
                        closed.set(true);
                    }
                }));
        when(comparisonService.read(project, second)).thenThrow(new NotFoundException("file.not.found.message"));

        var request = new CompareProjectFilesRequest(first, second);

        assertThrows(NotFoundException.class, () -> controller.compare(project, request));

        assertTrue(closed.get(), "the file that was already opened is not left open");
    }

    @Test
    void startsTheComparisonOfTheTwoSidesItIsGiven() throws Exception {
        var first = new ComparisonSideRequest("rules/Rates.xlsx", null, null);
        var second = new ComparisonSideRequest("rules/Rates.xlsx", "master", "rev-1");
        var workingCopy = new ComparisonContent("Rates.xlsx", new ByteArrayInputStream(new byte[0]));
        var revision = new ComparisonContent("Rates.xlsx", new ByteArrayInputStream(new byte[0]));
        when(comparisonService.read(project, first)).thenReturn(workingCopy);
        when(comparisonService.read(project, second)).thenReturn(revision);
        when(launcher.startContentOf(List.of(workingCopy, revision)))
                .thenReturn(new ComparisonStartedView("cmp-1"));

        var started = controller.compare(project, new CompareProjectFilesRequest(first, second));

        assertEquals("cmp-1", started.id());
    }
}
