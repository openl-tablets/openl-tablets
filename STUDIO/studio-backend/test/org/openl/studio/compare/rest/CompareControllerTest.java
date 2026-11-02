package org.openl.studio.compare.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.openl.rules.xls.merge.compare.WorkbookComparison;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.model.ResultNotReadyView;
import org.openl.studio.compare.model.ComparisonNodeStatus;
import org.openl.studio.compare.model.ComparisonTableView;
import org.openl.studio.compare.model.ComparisonView;
import org.openl.studio.compare.service.ComparisonLauncher;
import org.openl.studio.compare.service.ComparisonMapper;
import org.openl.studio.compare.service.ComparisonRegistry;

/**
 * Reading a comparison: what it found once it has ended, and an accepted request while it goes on.
 */
class CompareControllerTest {

    private static final String COMPARISON_ID = "cmp-1";
    private static final String TABLE_ID = "t1";

    private final ComparisonRegistry registry = mock(ComparisonRegistry.class);
    private final ComparisonMapper mapper = mock(ComparisonMapper.class);
    private final WorkbookComparison found = mock(WorkbookComparison.class);
    private final CompareController controller = new CompareController(mock(ComparisonLauncher.class), registry,
            mapper);

    @BeforeEach
    void init() {
        when(registry.has(COMPARISON_ID)).thenReturn(true);
    }

    private void comparisonHasEnded() {
        when(registry.isDone(COMPARISON_ID)).thenReturn(true);
        holds(found);
    }

    /** The registry reads the given comparison to the reader the controller hands it. */
    @SuppressWarnings("unchecked")
    private void holds(WorkbookComparison comparison) {
        when(registry.read(eq(COMPARISON_ID), any()))
                .thenAnswer(call -> call.getArgument(1, Function.class).apply(comparison));
    }

    private static void assertNotReady(ResponseEntity<?> answer) {
        assertEquals(HttpStatus.ACCEPTED, answer.getStatusCode());
        assertEquals(ResultNotReadyView.ResultState.NOT_READY, ((ResultNotReadyView) answer.getBody()).status());
    }

    @Test
    void getComparison_reportsWhatTheComparisonFound() {
        comparisonHasEnded();
        var view = ComparisonView.builder().id(COMPARISON_ID).identical(true).sheets(List.of()).build();
        when(mapper.toView(COMPARISON_ID, found)).thenReturn(view);

        var answer = controller.getComparison(COMPARISON_ID);

        assertEquals(HttpStatus.OK, answer.getStatusCode());
        assertSame(view, answer.getBody());
    }

    /**
     * A comparison still going on has nothing to report: the request is accepted rather than refused, so the
     * screen asking after it raises no error while it waits.
     */
    @Test
    void getComparison_whileTheComparisonIsStillGoingOn() {
        assertNotReady(controller.getComparison(COMPARISON_ID));
    }

    @Test
    void getComparison_thisSessionHoldsNoSuchComparison() {
        assertThrows(NotFoundException.class, () -> controller.getComparison("gone"));
    }

    @Test
    void getComparison_stoppedBeforeItFoundAnything() {
        when(registry.isDone(COMPARISON_ID)).thenReturn(true);

        assertThrows(ConflictException.class, () -> controller.getComparison(COMPARISON_ID));
    }

    @Test
    void getComparison_releasedWhileTheRequestWasOnItsWay() {
        when(registry.isDone(COMPARISON_ID)).thenReturn(true);
        // Held when the request asks, gone when it reads
        when(registry.has(COMPARISON_ID)).thenReturn(true, false);

        assertThrows(NotFoundException.class, () -> controller.getComparison(COMPARISON_ID));
    }

    @Test
    void getTable_reportsTheTableAsItStandsInBothFiles() {
        comparisonHasEnded();
        var table = ComparisonTableView.builder().id(TABLE_ID).name("Rates").status(ComparisonNodeStatus.EQUAL).build();
        when(mapper.toTable(found, TABLE_ID)).thenReturn(table);

        var answer = controller.getTable(COMPARISON_ID, TABLE_ID);

        assertEquals(HttpStatus.OK, answer.getStatusCode());
        assertSame(table, answer.getBody());
    }

    @Test
    void getTable_whileTheComparisonIsStillGoingOn() {
        assertNotReady(controller.getTable(COMPARISON_ID, TABLE_ID));
    }

    @Test
    void getTable_theComparisonHoldsNoSuchTable() {
        comparisonHasEnded();

        assertThrows(NotFoundException.class, () -> controller.getTable(COMPARISON_ID, "unknown"));
    }
}
