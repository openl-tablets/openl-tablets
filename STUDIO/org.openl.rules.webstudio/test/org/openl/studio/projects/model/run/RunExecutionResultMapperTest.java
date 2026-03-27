package org.openl.studio.projects.model.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.testmethod.ITestUnit;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;

@ExtendWith(MockitoExtension.class)
class RunExecutionResultMapperTest {

    private static final String TABLE_URI = "file://test.xlsx#Sheet1!A1";
    private static final String TABLE_NAME = "TestTable";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SchemaGenerator schemaGenerator;

    @Mock
    private TestUnitsResults results;

    @Mock
    private TestSuite testSuite;

    private RunExecutionResultMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new RunExecutionResultMapper(objectMapper, schemaGenerator, null);
        when(results.getTestSuite()).thenReturn(testSuite);
        when(testSuite.getUri()).thenReturn(TABLE_URI);
    }

    @Test
    void mapResult_emptyTestUnits() {
        when(results.getTestUnits()).thenReturn(List.of());
        when(results.getName()).thenReturn(TABLE_NAME);
        when(results.getExecutionTime()).thenReturn(2_000_000L);

        var result = mapper.mapResult(results);

        assertEquals(TABLE_NAME, result.tableName());
        assertNotNull(result.tableId());
        assertEquals(2.0, result.executionTimeMs(), 0.001);
        assertNull(result.result());
        assertNull(result.resultSchema());
        assertTrue(result.parameters().isEmpty());
        assertTrue(result.contextParameters().isEmpty());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void mapResult_withActualResult() {
        var testUnit = mock(ITestUnit.class);
        var testDescription = mock(TestDescription.class);
        var schema = objectMapper.createObjectNode();
        var paramType = mock(IOpenClass.class);
        var actualParam = mock(ParameterWithValueDeclaration.class);

        when(results.getTestUnits()).thenReturn(List.of(testUnit));
        when(results.getName()).thenReturn(TABLE_NAME);
        when(results.getExecutionTime()).thenReturn(5_000_000L);
        when(results.getTestDataColumnDisplayNames()).thenReturn(new String[0]);
        when(results.getContextColumnDisplayNames()).thenReturn(new String[0]);
        when(testSuite.getTestSuiteMethod()).thenReturn(null);

        when(testUnit.getActualResult()).thenReturn(42);
        when(testUnit.getActualParam()).thenReturn(actualParam);
        when(testUnit.getTest()).thenReturn(testDescription);
        when(testUnit.getContextParams(results)).thenReturn(ParameterWithValueDeclaration.EMPTY_ARRAY);
        when(testUnit.getErrors()).thenReturn(List.of());
        when(testDescription.getExecutionParams()).thenReturn(ParameterWithValueDeclaration.EMPTY_ARRAY);

        when(actualParam.getType()).thenReturn(paramType);
        when(paramType.getInstanceClass()).thenReturn((Class) Integer.class);
        when(schemaGenerator.generateSchema(Integer.class)).thenReturn(schema);

        var result = mapper.mapResult(results);

        assertEquals(TABLE_NAME, result.tableName());
        assertEquals(5.0, result.executionTimeMs(), 0.001);
        assertNotNull(result.result());
        assertEquals(42, result.result().intValue());
        assertNotNull(result.resultSchema());
        assertTrue(result.parameters().isEmpty());
        assertTrue(result.contextParameters().isEmpty());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void mapResult_withThrowableResult() {
        var testUnit = mock(ITestUnit.class);
        var testDescription = mock(TestDescription.class);

        when(results.getTestUnits()).thenReturn(List.of(testUnit));
        when(results.getName()).thenReturn(TABLE_NAME);
        when(results.getExecutionTime()).thenReturn(1_000_000L);
        when(results.getTestDataColumnDisplayNames()).thenReturn(new String[0]);
        when(results.getContextColumnDisplayNames()).thenReturn(new String[0]);
        when(testSuite.getTestSuiteMethod()).thenReturn(null);

        when(testUnit.getActualResult()).thenReturn(new RuntimeException("error"));
        when(testUnit.getTest()).thenReturn(testDescription);
        when(testUnit.getContextParams(results)).thenReturn(ParameterWithValueDeclaration.EMPTY_ARRAY);
        when(testUnit.getErrors()).thenReturn(List.of());
        when(testDescription.getExecutionParams()).thenReturn(ParameterWithValueDeclaration.EMPTY_ARRAY);

        var result = mapper.mapResult(results);

        assertNull(result.result());
        assertNull(result.resultSchema());
    }

    @Test
    void mapResult_withNullActualResult() {
        var testUnit = mock(ITestUnit.class);
        var testDescription = mock(TestDescription.class);

        when(results.getTestUnits()).thenReturn(List.of(testUnit));
        when(results.getName()).thenReturn(TABLE_NAME);
        when(results.getExecutionTime()).thenReturn(1_000_000L);
        when(results.getTestDataColumnDisplayNames()).thenReturn(new String[0]);
        when(results.getContextColumnDisplayNames()).thenReturn(new String[0]);
        when(testSuite.getTestSuiteMethod()).thenReturn(null);

        when(testUnit.getActualResult()).thenReturn(null);
        when(testUnit.getActualParam()).thenReturn(null);
        when(testUnit.getTest()).thenReturn(testDescription);
        when(testUnit.getContextParams(results)).thenReturn(ParameterWithValueDeclaration.EMPTY_ARRAY);
        when(testUnit.getErrors()).thenReturn(List.of());
        when(testDescription.getExecutionParams()).thenReturn(ParameterWithValueDeclaration.EMPTY_ARRAY);

        var result = mapper.mapResult(results);

        assertNull(result.result());
        assertNull(result.resultSchema());
    }

    @Test
    void mapResult_withParameters() {
        var testUnit = mock(ITestUnit.class);
        var testDescription = mock(TestDescription.class);
        var paramType = mock(IOpenClass.class);
        var schema = objectMapper.createObjectNode();

        var param = mock(ParameterWithValueDeclaration.class);
        when(param.getName()).thenReturn("age");
        when(param.getValue()).thenReturn(25);
        when(param.getType()).thenReturn(paramType);

        when(results.getTestUnits()).thenReturn(List.of(testUnit));
        when(results.getName()).thenReturn(TABLE_NAME);
        when(results.getExecutionTime()).thenReturn(1_000_000L);
        when(results.getTestDataColumnDisplayNames()).thenReturn(new String[]{"Age"});
        when(results.getContextColumnDisplayNames()).thenReturn(new String[0]);
        when(testSuite.getTestSuiteMethod()).thenReturn(null);

        when(testUnit.getActualResult()).thenReturn(new RuntimeException("skip"));
        when(testUnit.getTest()).thenReturn(testDescription);
        when(testUnit.getContextParams(results)).thenReturn(ParameterWithValueDeclaration.EMPTY_ARRAY);
        when(testUnit.getErrors()).thenReturn(List.of());
        when(testDescription.getExecutionParams()).thenReturn(new ParameterWithValueDeclaration[]{param});
        when(paramType.getInstanceClass()).thenReturn((Class) Integer.class);
        when(schemaGenerator.generateSchema(Integer.class)).thenReturn(schema);

        var result = mapper.mapResult(results);

        assertEquals(1, result.parameters().size());
        var p = result.parameters().getFirst();
        assertEquals("age", p.name());
        assertEquals("Age", p.description());
        assertEquals(25, p.value().intValue());
        assertNotNull(p.schema());
    }

    @Test
    void mapResult_withContextParameters() {
        var testUnit = mock(ITestUnit.class);
        var testDescription = mock(TestDescription.class);
        var ctxParam = mock(ParameterWithValueDeclaration.class);
        when(ctxParam.getName()).thenReturn("currentDate");
        when(ctxParam.getValue()).thenReturn("2025-01-01");

        when(results.getTestUnits()).thenReturn(List.of(testUnit));
        when(results.getName()).thenReturn(TABLE_NAME);
        when(results.getExecutionTime()).thenReturn(1_000_000L);
        when(results.getTestDataColumnDisplayNames()).thenReturn(new String[0]);
        when(results.getContextColumnDisplayNames()).thenReturn(new String[]{"Current Date"});
        when(testSuite.getTestSuiteMethod()).thenReturn(null);

        when(testUnit.getActualResult()).thenReturn(new RuntimeException("skip"));
        when(testUnit.getTest()).thenReturn(testDescription);
        when(testUnit.getContextParams(results)).thenReturn(new ParameterWithValueDeclaration[]{ctxParam});
        when(testUnit.getErrors()).thenReturn(List.of());
        when(testDescription.getExecutionParams()).thenReturn(ParameterWithValueDeclaration.EMPTY_ARRAY);

        var result = mapper.mapResult(results);

        assertEquals(1, result.contextParameters().size());
        var cp = result.contextParameters().getFirst();
        assertEquals("currentDate", cp.name());
        assertEquals("Current Date", cp.description());
        assertEquals("2025-01-01", cp.value().textValue());
    }

    @Test
    void mapResult_withErrors() {
        var testUnit = mock(ITestUnit.class);
        var testDescription = mock(TestDescription.class);

        var error1 = new OpenLMessage("Error B", Severity.ERROR);
        var error2 = new OpenLMessage("Error A", Severity.ERROR);
        var warning = new OpenLMessage("Warning", Severity.WARN);

        when(results.getTestUnits()).thenReturn(List.of(testUnit));
        when(results.getName()).thenReturn(TABLE_NAME);
        when(results.getExecutionTime()).thenReturn(1_000_000L);
        when(results.getTestDataColumnDisplayNames()).thenReturn(new String[0]);
        when(results.getContextColumnDisplayNames()).thenReturn(new String[0]);
        when(testSuite.getTestSuiteMethod()).thenReturn(null);

        when(testUnit.getActualResult()).thenReturn(new RuntimeException("skip"));
        when(testUnit.getTest()).thenReturn(testDescription);
        when(testUnit.getContextParams(results)).thenReturn(ParameterWithValueDeclaration.EMPTY_ARRAY);
        when(testUnit.getErrors()).thenReturn(List.of(error1, error2, warning));
        when(testDescription.getExecutionParams()).thenReturn(ParameterWithValueDeclaration.EMPTY_ARRAY);

        var result = mapper.mapResult(results);

        assertEquals(3, result.errors().size());
        // Sorted by severity then id
        assertEquals(Severity.WARN, result.errors().getFirst().severity());
        assertEquals(Severity.ERROR, result.errors().get(1).severity());
        assertEquals(Severity.ERROR, result.errors().get(2).severity());
    }

    @Test
    void mapResult_withTestSuiteMethod() {
        var testUnit = mock(ITestUnit.class);
        var testDescription = mock(TestDescription.class);
        var testSuiteMethod = mock(org.openl.rules.testmethod.TestSuiteMethod.class);
        var memberMetaInfo = mock(IMemberMetaInfo.class);
        var syntaxNode = mock(TableSyntaxNode.class);

        when(results.getTestUnits()).thenReturn(List.of(testUnit));
        when(results.getExecutionTime()).thenReturn(1_000_000L);
        when(results.getTestDataColumnDisplayNames()).thenReturn(new String[0]);
        when(results.getContextColumnDisplayNames()).thenReturn(new String[0]);
        when(testSuite.getTestSuiteMethod()).thenReturn(testSuiteMethod);
        var tableProperties = mock(ITableProperties.class);
        when(testSuiteMethod.getInfo()).thenReturn(memberMetaInfo);
        when(memberMetaInfo.getSyntaxNode()).thenReturn(syntaxNode);
        when(syntaxNode.getTableProperties()).thenReturn(tableProperties);
        when(tableProperties.getName()).thenReturn("myTestMethod");

        when(testUnit.getActualResult()).thenReturn(new RuntimeException("skip"));
        when(testUnit.getTest()).thenReturn(testDescription);
        when(testUnit.getContextParams(results)).thenReturn(ParameterWithValueDeclaration.EMPTY_ARRAY);
        when(testUnit.getErrors()).thenReturn(List.of());
        when(testDescription.getExecutionParams()).thenReturn(ParameterWithValueDeclaration.EMPTY_ARRAY);

        var result = mapper.mapResult(results);

        assertNotNull(result.tableName());
    }
}
