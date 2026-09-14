package org.openl.studio.projects.service.tables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUtils;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.model.PageResponse;
import org.openl.studio.projects.model.ExecutionValueMapper;
import org.openl.studio.projects.model.ParameterValue;
import org.openl.studio.projects.model.tables.TableInputView;
import org.openl.studio.projects.model.tables.TestCaseView;
import org.openl.studio.projects.service.AbstractMethodExecutorService;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * Default implementation of {@link TableInputService}.
 *
 * <p>The table is resolved the way the run, tests and trace APIs resolve it. The input described here is the
 * input those APIs accept.
 */
@Service
@RequiredArgsConstructor
public class TableInputServiceImpl extends AbstractMethodExecutorService implements TableInputService {

    /** Name the runtime context is described under. The run and trace input JSON carry it under the same key. */
    static final String RUNTIME_CONTEXT = "runtimeContext";

    private final WorkspaceProjectService projectService;

    @Override
    public TableInputView describe(ProjectModel projectModel,
                                   IOpenLTable table,
                                   boolean currentOpenedModule,
                                   ObjectMapper objectMapper,
                                   SchemaGenerator schemaGenerator) {
        var method = resolveMethod(projectModel, table, currentOpenedModule, null);
        if (method == null) {
            throw new NotFoundException("table.message");
        }
        var builder = TableInputView.builder()
                .tableId(TableUtils.makeTableId(table.getUri()))
                .name(method.getName());
        if (method instanceof TestSuiteMethod) {
            return builder.testTable(true).parameters(List.of()).build();
        }
        return describeRuleTable(builder, method, valueMapper(objectMapper, schemaGenerator));
    }

    @Override
    public PageResponse<TestCaseView> listTestCases(ProjectModel projectModel,
                                                    IOpenLTable table,
                                                    boolean currentOpenedModule,
                                                    Pageable page,
                                                    ObjectMapper objectMapper,
                                                    SchemaGenerator schemaGenerator) {
        var suite = testSuiteOf(projectModel, table, currentOpenedModule);
        var tests = suite.getTests();
        var testSuite = new TestSuite(suite);
        var valueMapper = valueMapper(objectMapper, schemaGenerator);
        var content = Arrays.stream(tests)
                .skip(page.getOffset())
                .limit(page.getPageSize())
                .map(test -> describeTestCase(testSuite, test, valueMapper, false))
                .toList();
        return PageResponse.of(content, page, (long) tests.length);
    }

    @Override
    public TestCaseView describeTestCase(ProjectModel projectModel,
                                         IOpenLTable table,
                                         boolean currentOpenedModule,
                                         String caseId,
                                         ObjectMapper objectMapper,
                                         SchemaGenerator schemaGenerator) {
        var suite = testSuiteOf(projectModel, table, currentOpenedModule);
        var test = Arrays.stream(suite.getTests())
                .filter(candidate -> caseId.equals(candidate.getId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("test.case.message", caseId));
        return describeTestCase(new TestSuite(suite), test, valueMapper(objectMapper, schemaGenerator), true);
    }

    /** The test suite the table stands for. A table that is not a test table carries no cases to read. */
    private static TestSuiteMethod testSuiteOf(ProjectModel projectModel, IOpenLTable table, boolean currentOpenedModule) {
        if (resolveMethod(projectModel, table, currentOpenedModule, null) instanceof TestSuiteMethod suite) {
            return suite;
        }
        throw new NotFoundException("table.message");
    }

    private ExecutionValueMapper valueMapper(ObjectMapper objectMapper, SchemaGenerator schemaGenerator) {
        return new ExecutionValueMapper(objectMapper, schemaGenerator,
                projectService.getSpreadsheetResultNamingStrategy());
    }

    private TableInputView describeRuleTable(TableInputView.TableInputViewBuilder builder,
                                             IOpenMethod method,
                                             ExecutionValueMapper valueMapper) {
        var signature = method.getSignature();
        var parameters = IntStream.range(0, signature.getNumberOfParameters())
                .mapToObj(i -> {
                    var type = signature.getParameterType(i);
                    return valueMapper.describeParameter(signature.getParameterName(i), type, defaultValueOf(type));
                })
                .toList();
        return builder
                .testTable(false)
                .parameters(parameters)
                .runtimeContext(providesRuntimeContext()
                        ? valueMapper.describeParameter(RUNTIME_CONTEXT,
                                JavaOpenClass.getOpenClass(DefaultRulesRuntimeContext.class), null)
                        : null)
                .build();
    }

    /**
     * Writes a case as its context columns followed by its input columns.
     *
     * <p>Each parameter is named by its column and described by the column's display name. No schema is written.
     * A client shows the values, it does not edit them.
     *
     * <p>In a page of cases a value with inner structure is only referred to. A case read on its own carries it
     * in full.
     */
    private static TestCaseView describeTestCase(TestSuite testSuite,
                                                 TestDescription test,
                                                 ExecutionValueMapper valueMapper,
                                                 boolean full) {
        var suite = testSuite.getTestSuiteMethod();
        var parameters = new ArrayList<ParameterValue>();
        for (var param : TestUtils.getContextParams(testSuite, test)) {
            parameters.add(writeValue(suite, param, valueMapper, full));
        }
        for (var param : test.getExecutionParams()) {
            parameters.add(writeValue(suite, param, valueMapper, full));
        }
        return TestCaseView.builder()
                .id(test.getId())
                .description(test.hasDescription() ? test.getDescription() : null)
                .parameters(parameters)
                .build();
    }

    private static ParameterValue writeValue(TestSuiteMethod suite,
                                             ParameterWithValueDeclaration param,
                                             ExecutionValueMapper valueMapper,
                                             boolean full) {
        var displayName = suite.getColumnDisplayName(param.getName());
        var description = displayName != null ? displayName : param.getName();
        return full
                ? valueMapper.writeParameter(param, description, false).toBuilder().lazy(false).build()
                : valueMapper.writeParameterLazily(param, description);
    }

    /**
     * The value a parameter starts with.
     *
     * <p>A datatype starts with the defaults its fields declare. A plain value, a list or a type that cannot be
     * created starts unset, as it did in the legacy input form.
     */
    private static @Nullable Object defaultValueOf(IOpenClass type) {
        if (type.isSimple() || type.isArray() || type instanceof DomainOpenClass || type.getInstanceClass() == null) {
            return null;
        }
        try {
            return type.newInstance(new SimpleRulesVM().getRuntimeEnv());
        } catch (Exception | LinkageError e) {
            return null;
        }
    }

    /**
     * Whether the project provides the runtime context to its rules.
     *
     * <p>A project without a deployment configuration provides it, as OpenL Rule Services does by default. A
     * project with a configuration provides it only when the configuration says so.
     */
    private boolean providesRuntimeContext() {
        var rulesDeploy = projectService.getWebStudio().getCurrentProjectRulesDeploy();
        return rulesDeploy == null || Boolean.TRUE.equals(rulesDeploy.isProvideRuntimeContext());
    }
}
