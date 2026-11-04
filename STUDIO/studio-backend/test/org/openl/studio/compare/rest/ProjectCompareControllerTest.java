package org.openl.studio.compare.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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

        var request = new CompareProjectFilesRequest(first, second, null);

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

        var started = controller.compare(project, new CompareProjectFilesRequest(first, second, null));

        assertEquals("cmp-1", started.id());
    }

    @Test
    void comparesBothFilesWithTheBaseTheyWereMadeFrom() throws Exception {
        var yours = new ComparisonSideRequest("rules/Rates.xlsx", null, null);
        var theirs = new ComparisonSideRequest("rules/Rates.xlsx", "feature", "rev-2");
        var base = new ComparisonSideRequest("rules/Rates.xlsx", "feature", "rev-1");
        var contents = List.of(new ComparisonContent("Rates.xlsx", new ByteArrayInputStream(new byte[0])),
                new ComparisonContent("Rates.xlsx", new ByteArrayInputStream(new byte[0])),
                new ComparisonContent("Rates.xlsx", new ByteArrayInputStream(new byte[0])));
        when(comparisonService.read(project, base)).thenReturn(contents.getFirst());
        when(comparisonService.read(project, yours)).thenReturn(contents.get(1));
        when(comparisonService.read(project, theirs)).thenReturn(contents.get(2));
        // The base first, as a conflict is compared
        when(launcher.startContentOf(contents)).thenReturn(new ComparisonStartedView("cmp-3"));

        var started = controller.compare(project, new CompareProjectFilesRequest(yours, theirs, base));

        assertEquals("cmp-3", started.id());
    }

    @Test
    void putsTheBaseAndYourFileDownWhenTheirFileCannotBeRead() {
        var closed = new AtomicInteger();
        var yours = new ComparisonSideRequest("rules/Rates.xlsx", null, null);
        var theirs = new ComparisonSideRequest("rules/Gone.xlsx", "feature", "rev-2");
        var base = new ComparisonSideRequest("rules/Rates.xlsx", "feature", "rev-1");
        when(comparisonService.read(project, base)).thenReturn(closing("Rates.xlsx", closed));
        when(comparisonService.read(project, yours)).thenReturn(closing("Rates.xlsx", closed));
        when(comparisonService.read(project, theirs)).thenThrow(new NotFoundException("file.not.found.message"));

        var request = new CompareProjectFilesRequest(yours, theirs, base);

        assertThrows(NotFoundException.class, () -> controller.compare(project, request));
        assertEquals(2, closed.get(), "the files that were already opened are not left open");
    }

    /** A file that counts how often it is closed. */
    private static ComparisonContent closing(String name, AtomicInteger closed) {
        return new ComparisonContent(name, new ByteArrayInputStream(new byte[0]) {
            @Override
            public void close() {
                closed.incrementAndGet();
            }
        });
    }
}
