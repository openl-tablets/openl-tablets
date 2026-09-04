package org.openl.studio.projects.model.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import org.junit.jupiter.api.Test;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.testmethod.ITestUnit;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestStatus;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.types.IMemberMetaInfo;

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
        var result = mapper.mapToTestCaseResult(results, new TestExecutionSummaryQuery(true, 5));

        assertEquals(1, result.numberOfTests());
        assertEquals(1, result.numberOfFailures());
        var mappedUnit = result.testUnits().getFirst();
        assertEquals(TestStatus.TR_EXCEPTION, mappedUnit.status());
        assertTrue(mappedUnit.testAssertions().isEmpty());
        assertEquals("Compilation failed", mappedUnit.errors().getFirst().summary());
        verify(testUnit, never()).getComparisonResults();
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
