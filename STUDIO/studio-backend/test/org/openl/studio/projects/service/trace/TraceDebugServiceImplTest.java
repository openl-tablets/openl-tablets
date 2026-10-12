package org.openl.studio.projects.service.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.CompiledOpenClass;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.ui.ProjectModel;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

/**
 * The trace runs a single test case: several cases just stack in the tree and rarely help, so a
 * requested range or the whole suite collapses to one case.
 */
class TraceDebugServiceImplTest {

    private final TableInputParserService parser = mock(TableInputParserService.class);
    private final ProjectModel model = mock(ProjectModel.class);
    private final IOpenLTable table = mock(IOpenLTable.class);
    private final IOpenMethod method = mock(IOpenMethod.class);
    private final IMethodSignature signature = mock(IMethodSignature.class);
    private final CompiledOpenClass compiled = mock(CompiledOpenClass.class);

    @BeforeEach
    void init() {
        when(method.getSignature()).thenReturn(signature);
        when(table.getUri()).thenReturn("table-uri");
        when(model.getCompiledOpenClass()).thenReturn(compiled);
    }

    @Test
    void collapsesARangeToItsFirstSelectedCase() {
        var suite = mock(TestSuiteMethod.class);
        when(suite.getIndices("2-4,7")).thenReturn(new int[]{1, 2, 3, 6});

        assertEquals(1, TraceDebugServiceImpl.firstTestIndex(suite, "2-4,7"),
                "a range traces only its first selected case");
    }

    @Test
    void tracesTheFirstCaseForTheWholeSuite() {
        var suite = mock(TestSuiteMethod.class);

        assertEquals(0, TraceDebugServiceImpl.firstTestIndex(suite, null),
                "no range traces the suite's first case");
    }

    @Test
    void fallsBackToTheFirstCaseWhenTheRangeMatchesNothing() {
        var suite = mock(TestSuiteMethod.class);
        when(suite.getIndices("999")).thenReturn(new int[0]);

        assertEquals(0, TraceDebugServiceImpl.firstTestIndex(suite, "999"),
                "an empty range falls back to the first case");
    }

    @Test
    void validatesTheRequestedVersionBeforeBuildingAContextFreeTrace() {
        when(model.getCurrentDispatcherMethod(method, "table-uri")).thenReturn(method);
        var parsed = new TableInputParserService.ParseResult(new Object[0], null);
        when(parser.parseInput(eq("{}"), eq(method), any())).thenReturn(parsed);

        assertNotNull(new TraceDebugServiceImpl(parser).buildTestSuite(request()));
        verify(parser).validateInput(eq(parsed), argThat(selection -> selection.version() == method));
    }

    @Test
    void usesTheProjectMethodWhenContextSelectsTheTraceVersion() {
        when(method.getName()).thenReturn("rule");
        when(signature.getParameterTypes()).thenReturn(new IOpenClass[0]);
        var openClass = mock(IOpenClass.class);
        when(compiled.getOpenClassWithErrors()).thenReturn(openClass);
        when(openClass.getMethod(eq("rule"), any(IOpenClass[].class))).thenReturn(method);
        var parsed = new TableInputParserService.ParseResult(new Object[0],
                RulesRuntimeContextFactory.buildRulesRuntimeContext());
        when(parser.parseInput(eq("{}"), eq(method), any())).thenReturn(parsed);

        assertNotNull(new TraceDebugServiceImpl(parser).buildTestSuite(request()));
        verify(model, never()).getCurrentDispatcherMethod(any(), any());
        verify(parser).validateInput(eq(parsed), argThat(selection -> selection.version() == method));
    }

    private TraceDebugStartRequest request() {
        return new TraceDebugStartRequest(model, table, method, null, "table", null, false, "{}",
                new ObjectMapper(), Set.of(), Set.of(), false, false, false, false, null, "session");
    }
}
