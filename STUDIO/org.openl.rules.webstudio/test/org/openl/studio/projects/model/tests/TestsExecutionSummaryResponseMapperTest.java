package org.openl.studio.projects.model.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import org.junit.jupiter.api.Test;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.repository.api.Page;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.testmethod.ITestUnit;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestStatus;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.studio.projects.model.ParameterValue;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;

class TestsExecutionSummaryResponseMapperTest {

    @Test
    void mapsUnexpectedExceptionWithoutAssertions() {
        var results = mock(TestUnitsResults.class);
        var testUnit = mock(ITestUnit.class);
        var test = mock(TestDescription.class);
        mockTestTable(results);

        when(results.getNumberOfTestUnits()).thenReturn(1);
        when(results.getNumberOfFailures()).thenReturn(1);
        when(results.getFilteredTestUnits(true, 5)).thenReturn(List.of(testUnit));
        when(results.getTestDataColumnDisplayNames()).thenReturn(new String[0]);
        when(results.getContextColumnDisplayNames()).thenReturn(new String[0]);

        when(testUnit.getTest()).thenReturn(test);
        when(testUnit.getResultStatus()).thenReturn(TestStatus.TR_EXCEPTION);
        when(testUnit.getContextParams(results)).thenReturn(ParameterWithValueDeclaration.EMPTY_ARRAY);
        when(testUnit.getErrors()).thenReturn(List.of(new OpenLMessage("Compilation failed", Severity.ERROR)));
        when(test.getExecutionParams()).thenReturn(ParameterWithValueDeclaration.EMPTY_ARRAY);

        var mapper = new TestsExecutionSummaryResponseMapper(new ObjectMapper(), mock(SchemaGenerator.class), null);
        var result = mapper.mapToTestCaseResult(results, new TestExecutionSummaryQuery(true, 5, false, false));

        assertEquals(1, result.numberOfTests());
        assertEquals(1, result.numberOfFailures());
        var mappedUnit = result.testUnits().getFirst();
        assertEquals(TestStatus.TR_EXCEPTION, mappedUnit.status());
        assertTrue(mappedUnit.testAssertions().isEmpty());
        assertEquals("Compilation failed", mappedUnit.errors().getFirst().summary());
        verify(testUnit, never()).getComparisonResults();
    }

    /**
     * A page of the summary says how many test tables ran in all, so a screen can page through them.
     */
    @Test
    void reportsHowManyTestTablesRan() {
        var first = mock(TestUnitsResults.class);
        var second = mock(TestUnitsResults.class);
        mockTestTable(first);
        mockTestTable(second);
        when(first.getName()).thenReturn("AlphaTest");
        when(second.getName()).thenReturn("BetaTest");
        when(first.getFilteredTestUnits(false, 5)).thenReturn(List.of());
        var mapper = new TestsExecutionSummaryResponseMapper(new ObjectMapper(), mock(SchemaGenerator.class), null);

        var summary = mapper.mapExecutionSummary(List.of(first, second), TestExecutionSummaryQuery.noFilter(), Page.of(0, 1));

        assertEquals(2L, summary.getTotal());
        assertEquals(1, summary.getContent().size());
    }

    /**
     * The compound result carries the whole value the rule returned, and not only the values the test compares.
     */
    @Test
    void carriesTheWholeReturnedValueOnRequest() {
        var results = mock(TestUnitsResults.class);
        var testUnit = mock(ITestUnit.class);
        var test = mock(TestDescription.class);
        mockTestTable(results);
        when(results.getFilteredTestUnits(false, 5)).thenReturn(List.of(testUnit));
        when(results.getTestDataColumnDisplayNames()).thenReturn(new String[0]);
        when(results.getContextColumnDisplayNames()).thenReturn(new String[0]);
        when(results.getTestResultColumnDisplayNames()).thenReturn(new String[0]);
        when(testUnit.getTest()).thenReturn(test);
        when(testUnit.getResultStatus()).thenReturn(TestStatus.TR_OK);
        when(testUnit.getComparisonResults()).thenReturn(List.of());
        when(testUnit.getActualResult()).thenReturn(42);
        when(testUnit.getContextParams(results)).thenReturn(ParameterWithValueDeclaration.EMPTY_ARRAY);
        when(testUnit.getErrors()).thenReturn(List.of());
        when(test.getExecutionParams()).thenReturn(ParameterWithValueDeclaration.EMPTY_ARRAY);
        var mapper = new TestsExecutionSummaryResponseMapper(new ObjectMapper(), mock(SchemaGenerator.class), null);

        var asked = mapper.mapToTestCaseResult(results, new TestExecutionSummaryQuery(false, 5, true, false));
        var plain = mapper.mapToTestCaseResult(results, new TestExecutionSummaryQuery(false, 5, false, false));

        assertEquals(42, asked.testUnits().getFirst().result().value().asInt());
        assertNull(plain.testUnits().getFirst().result());
    }

    @Test
    void refersToAnInputWithInnerStructureWhenLazyValuesAreAsked() {
        var results = mock(TestUnitsResults.class);
        var testUnit = mock(ITestUnit.class);
        var test = mock(TestDescription.class);
        var type = mock(IOpenClass.class);
        mockTestTable(results);
        when(results.getFilteredTestUnits(false, 5)).thenReturn(List.of(testUnit));
        when(results.getTestDataColumnDisplayNames()).thenReturn(new String[]{"Driver"});
        when(results.getContextColumnDisplayNames()).thenReturn(new String[0]);
        when(results.getTestResultColumnDisplayNames()).thenReturn(new String[0]);
        when(testUnit.getTest()).thenReturn(test);
        when(testUnit.getResultStatus()).thenReturn(TestStatus.TR_OK);
        when(testUnit.getComparisonResults()).thenReturn(List.of());
        when(testUnit.getContextParams(results)).thenReturn(ParameterWithValueDeclaration.EMPTY_ARRAY);
        when(testUnit.getErrors()).thenReturn(List.of());
        when(test.getExecutionParams()).thenReturn(new ParameterWithValueDeclaration[]{
                new ParameterWithValueDeclaration("driver", Map.of("name", "Sara"), type)});
        var mapper = new TestsExecutionSummaryResponseMapper(new ObjectMapper(), mock(SchemaGenerator.class), null);

        var lazy = firstParameter(mapper, results, true);
        var full = firstParameter(mapper, results, false);

        assertEquals(Boolean.TRUE, lazy.lazy());
        assertNull(lazy.value(), "the value itself is left out, and read when it is asked for");
        assertEquals("Driver", lazy.description());
        assertEquals("Sara", full.value().get("name").asText());
    }

    private static ParameterValue firstParameter(TestsExecutionSummaryResponseMapper mapper,
                                                 TestUnitsResults results,
                                                 boolean lazyValues) {
        var query = new TestExecutionSummaryQuery(false, 5, false, lazyValues);
        return mapper.mapToTestCaseResult(results, query).testUnits().getFirst().parameters().getFirst();
    }

    private static void mockTestTable(TestUnitsResults results) {
        var testSuite = mock(TestSuite.class);
        var testMethod = mock(TestSuiteMethod.class);
        var methodInfo = mock(IMemberMetaInfo.class);
        var syntaxNode = mock(TableSyntaxNode.class);
        var properties = mock(ITableProperties.class);

        when(results.getTestSuite()).thenReturn(testSuite);
        when(testSuite.getTestSuiteMethod()).thenReturn(testMethod);
        when(testSuite.getUri()).thenReturn("file://test.xlsx#Sheet1!A1");
        when(testMethod.getInfo()).thenReturn(methodInfo);
        when(testMethod.getSyntaxNode()).thenReturn(syntaxNode);
        when(methodInfo.getSyntaxNode()).thenReturn(syntaxNode);
        when(syntaxNode.getTableProperties()).thenReturn(properties);
        when(properties.getName()).thenReturn("BrokenTest");
    }
}
