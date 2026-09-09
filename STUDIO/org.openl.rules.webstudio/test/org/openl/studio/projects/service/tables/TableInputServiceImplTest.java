package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.repository.api.Page;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.config.ObjectSchemaGeneratorConfiguration;
import org.openl.studio.projects.model.ParameterValue;
import org.openl.studio.projects.model.tables.TestCaseView;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;

@ExtendWith(MockitoExtension.class)
class TableInputServiceImplTest {

    private static final String TABLE_URI = "file:/Bank%20Rating.xlsx?sheet=Rating%20Algorithm&range=D54:G68";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SchemaGenerator schemaGenerator = new ObjectSchemaGeneratorConfiguration()
            .inputSchemaGenerator(objectMapper);

    @Mock
    private WorkspaceProjectService projectService;
    @Mock
    private WebStudio webStudio;
    @Mock
    private ProjectModel projectModel;
    @Mock
    private IOpenLTable table;

    @InjectMocks
    private TableInputServiceImpl service;

    @BeforeEach
    void setUp() {
        when(table.getUri()).thenReturn(TABLE_URI);
    }

    @Test
    void describesRuleTableParametersBySchema() {
        var method = ruleMethod();
        when(projectModel.getMethod(TABLE_URI)).thenReturn(method);
        when(projectService.getWebStudio()).thenReturn(webStudio);
        when(webStudio.getCurrentProjectRulesDeploy()).thenReturn(null);

        var view = service.describe(projectModel, table, false, objectMapper, schemaGenerator);

        assertFalse(view.testTable());
        assertEquals("Premium", view.name());
        assertNotNull(view.tableId());
        assertEquals(List.of("age", "since"), view.parameters().stream().map(ParameterValue::name).toList());
        var age = view.parameters().getFirst();
        assertEquals("int", age.description());
        assertEquals("integer", age.schema().get("type").asText());
        assertTrue(age.value().isNull());
        var since = view.parameters().get(1);
        assertEquals("date-time", since.schema().get("format").asText());
    }

    /** A test table takes no parameters of its own. Its input is the cases it carries. */
    @Test
    void describesATestTableAsCarryingCases() {
        var suite = testSuite();
        when(projectModel.getMethod(TABLE_URI)).thenReturn(suite);

        var view = service.describe(projectModel, table, false, objectMapper, schemaGenerator);

        assertTrue(view.testTable());
        assertEquals("PremiumTest", view.name());
        assertTrue(view.parameters().isEmpty());
        assertNull(view.runtimeContext());
    }

    /**
     * A project without a deployment configuration provides the runtime context, so its schema is described.
     */
    @Test
    void describesRuntimeContextWhenTheProjectProvidesIt() {
        var method = ruleMethod();
        when(projectModel.getMethod(TABLE_URI)).thenReturn(method);
        when(projectService.getWebStudio()).thenReturn(webStudio);
        when(webStudio.getCurrentProjectRulesDeploy()).thenReturn(null);

        var view = service.describe(projectModel, table, false, objectMapper, schemaGenerator);

        var context = view.runtimeContext();
        assertNotNull(context);
        assertEquals(TableInputServiceImpl.RUNTIME_CONTEXT, context.name());
        assertTrue(context.schema().get("properties").has("lob"));
        assertTrue(context.schema().get("properties").has("currentDate"));
    }

    @Test
    void omitsRuntimeContextWhenTheDeploymentConfigurationDisablesIt() {
        var rulesDeploy = new RulesDeploy();
        rulesDeploy.setProvideRuntimeContext(false);
        var method = ruleMethod();
        when(projectModel.getMethod(TABLE_URI)).thenReturn(method);
        when(projectService.getWebStudio()).thenReturn(webStudio);
        when(webStudio.getCurrentProjectRulesDeploy()).thenReturn(rulesDeploy);

        var view = service.describe(projectModel, table, false, objectMapper, schemaGenerator);

        assertNull(view.runtimeContext());
    }

    @Test
    void resolvesTheTableWithinTheOpenedModuleOnRequest() {
        var method = ruleMethod();
        when(projectModel.getOpenedModuleMethod(TABLE_URI)).thenReturn(method);
        when(projectService.getWebStudio()).thenReturn(webStudio);

        var view = service.describe(projectModel, table, true, objectMapper, schemaGenerator);

        assertEquals("Premium", view.name());
    }

    /**
     * A page lists the cases of a test table. The context columns come first, then the inputs, each under the
     * column's display name and without a schema.
     *
     * The page reports how many cases the table holds. A plain value is listed. A value with inner structure is
     * only referred to.
     */
    @Test
    void listsAPageOfTestCases() {
        var suite = testSuite();
        when(projectModel.getMethod(TABLE_URI)).thenReturn(suite);

        var page = service.listTestCases(projectModel, table, false, Page.of(0, 1), objectMapper, schemaGenerator);

        assertEquals(2L, page.getTotal());
        assertEquals(0, page.getPageNumber());
        assertEquals(1, page.getPageSize());
        assertEquals(1, page.getContent().size());
        var testCase = page.getContent().iterator().next();
        assertEquals("1", testCase.id());
        assertEquals("Young driver", testCase.description());
        assertEquals(List.of("_context_.lob", "age", "bank"),
                testCase.parameters().stream().map(ParameterValue::name).toList());
        var lob = testCase.parameters().getFirst();
        assertEquals("LOB", lob.description());
        assertEquals("Auto", lob.value().asText());
        assertFalse(lob.lazy());
        var age = testCase.parameters().get(1);
        assertEquals("Age", age.description());
        assertEquals(25, age.value().asInt());
        assertFalse(age.lazy());
        assertNull(age.schema());
        var bank = testCase.parameters().get(2);
        assertEquals("Bank", bank.description());
        assertTrue(bank.lazy());
        assertNull(bank.value());
    }

    /** A later page carries the cases that follow, so a long table is read page by page. */
    @Test
    void listsTheCasesThatFollowOnTheNextPage() {
        var suite = testSuite();
        when(projectModel.getMethod(TABLE_URI)).thenReturn(suite);

        var page = service.listTestCases(projectModel, table, false, Page.of(1, 1), objectMapper, schemaGenerator);

        assertEquals(2L, page.getTotal());
        assertEquals(1, page.getPageNumber());
        assertEquals(List.of("2"), page.getContent().stream().map(TestCaseView::id).toList());
    }

    @Test
    void rejectsTheCasesOfATableThatIsNotATestTable() {
        var method = ruleMethod();
        when(projectModel.getMethod(TABLE_URI)).thenReturn(method);
        var page = Page.of(0, 25);

        assertThrows(NotFoundException.class,
                () -> service.listTestCases(projectModel, table, false, page, objectMapper, schemaGenerator));
    }

    /** A case read on its own carries every value, the referred ones included. */
    @Test
    void describesOneCaseWithEveryValue() {
        var suite = testSuite();
        when(projectModel.getMethod(TABLE_URI)).thenReturn(suite);

        var testCase = service.describeTestCase(projectModel, table, false, "1", objectMapper, schemaGenerator);

        assertEquals("1", testCase.id());
        assertEquals("Young driver", testCase.description());
        var bank = testCase.parameters().get(2);
        assertFalse(bank.lazy());
        assertEquals("DE", bank.value().get("countryCode").asText());
        assertEquals(25, testCase.parameters().get(1).value().asInt());
    }

    @Test
    void rejectsACaseTheTableDoesNotHave() {
        var suite = testSuite();
        when(projectModel.getMethod(TABLE_URI)).thenReturn(suite);

        assertThrows(NotFoundException.class,
                () -> service.describeTestCase(projectModel, table, false, "9", objectMapper, schemaGenerator));
    }

    @Test
    void rejectsACaseOfATableThatIsNotATestTable() {
        var method = ruleMethod();
        when(projectModel.getMethod(TABLE_URI)).thenReturn(method);

        assertThrows(NotFoundException.class,
                () -> service.describeTestCase(projectModel, table, false, "1", objectMapper, schemaGenerator));
    }

    @Test
    void leavesTheCaseDescriptionOutWhenTheTableHasNone() {
        var suite = mock(TestSuiteMethod.class);
        var only = mock(TestDescription.class);
        when(projectModel.getMethod(TABLE_URI)).thenReturn(suite);
        when(suite.getTests()).thenReturn(new TestDescription[]{only});
        when(only.getId()).thenReturn("1");
        when(only.getRuntimeContext()).thenReturn(new DefaultRulesRuntimeContext());
        when(only.getExecutionParams()).thenReturn(ParameterWithValueDeclaration.EMPTY_ARRAY);

        var page = service.listTestCases(projectModel, table, false, Page.of(0, 25), objectMapper, schemaGenerator);

        var testCase = page.getContent().iterator().next();
        assertNull(testCase.description());
        assertTrue(testCase.parameters().isEmpty());
    }

    /**
     * A datatype parameter starts with the defaults its fields declare. The form shows them and sends them back
     * unless the user changes them. A plain parameter starts unset.
     */
    @Test
    void describesTheDefaultsOfADatatypeParameter() {
        var method = mock(IOpenMethod.class);
        var signature = mock(IMethodSignature.class);
        when(method.getName()).thenReturn("Premium");
        when(method.getSignature()).thenReturn(signature);
        when(signature.getNumberOfParameters()).thenReturn(1);
        when(signature.getParameterName(0)).thenReturn("bank");
        when(signature.getParameterType(0)).thenReturn(JavaOpenClass.getOpenClass(Bank.class));
        when(projectModel.getMethod(TABLE_URI)).thenReturn(method);
        when(projectService.getWebStudio()).thenReturn(webStudio);

        var view = service.describe(projectModel, table, false, objectMapper, schemaGenerator);

        var bank = view.parameters().getFirst();
        assertEquals("DE", bank.value().get("countryCode").asText());
        assertTrue(bank.value().get("bankId").isNull());
        assertEquals("string", bank.schema().get("properties").get("countryCode").get("type").asText());
        assertTrue(view.runtimeContext().value().isNull());
    }

    @Test
    void rejectsTableWithoutExecutableMethod() {
        when(projectModel.getMethod(TABLE_URI)).thenReturn(null);

        assertThrows(NotFoundException.class,
                () -> service.describe(projectModel, table, false, objectMapper, schemaGenerator));
    }

    /** Stands in for a generated datatype bean. One field declares a default, the other does not. */
    public static class Bank {
        private String bankId;
        private String countryCode = "DE";

        public String getBankId() {
            return bankId;
        }

        public void setBankId(String bankId) {
            this.bankId = bankId;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public void setCountryCode(String countryCode) {
            this.countryCode = countryCode;
        }
    }

    /** Two cases. The first sets the context, a plain input and a datatype input. */
    private static TestSuiteMethod testSuite() {
        var suite = mock(TestSuiteMethod.class);
        var first = mock(TestDescription.class);
        var second = mock(TestDescription.class);
        var context = new DefaultRulesRuntimeContext();
        context.setLob("Auto");
        lenient().when(suite.getName()).thenReturn("PremiumTest");
        lenient().when(suite.getTests()).thenReturn(new TestDescription[]{first, second});
        lenient().when(suite.getColumnsCount()).thenReturn(3);
        lenient().when(suite.getColumnName(0)).thenReturn("_context_.lob");
        lenient().when(suite.getColumnName(1)).thenReturn("age");
        lenient().when(suite.getColumnName(2)).thenReturn("bank");
        lenient().when(suite.getColumnDisplayName("_context_.lob")).thenReturn("LOB");
        lenient().when(suite.getColumnDisplayName("age")).thenReturn("Age");
        lenient().when(suite.getColumnDisplayName("bank")).thenReturn("Bank");
        lenient().when(first.getId()).thenReturn("1");
        lenient().when(second.getId()).thenReturn("2");
        lenient().when(second.getRuntimeContext()).thenReturn(new DefaultRulesRuntimeContext());
        lenient().when(second.getExecutionParams()).thenReturn(ParameterWithValueDeclaration.EMPTY_ARRAY);
        lenient().when(first.hasDescription()).thenReturn(true);
        lenient().when(first.getDescription()).thenReturn("Young driver");
        lenient().when(first.getRuntimeContext()).thenReturn(context);
        lenient().when(first.getExecutionParams())
                .thenReturn(new ParameterWithValueDeclaration[]{
                        new ParameterWithValueDeclaration("age", 25, JavaOpenClass.INT),
                        new ParameterWithValueDeclaration("bank", new Bank(), JavaOpenClass.getOpenClass(Bank.class))});
        return suite;
    }

    private static IOpenMethod ruleMethod() {
        var method = mock(IOpenMethod.class);
        var signature = mock(IMethodSignature.class);
        lenient().when(method.getName()).thenReturn("Premium");
        lenient().when(method.getSignature()).thenReturn(signature);
        lenient().when(signature.getNumberOfParameters()).thenReturn(2);
        lenient().when(signature.getParameterName(0)).thenReturn("age");
        lenient().when(signature.getParameterType(0)).thenReturn(JavaOpenClass.INT);
        lenient().when(signature.getParameterName(1)).thenReturn("since");
        lenient().when(signature.getParameterType(1)).thenReturn(JavaOpenClass.getOpenClass(Date.class));
        return method;
    }
}
