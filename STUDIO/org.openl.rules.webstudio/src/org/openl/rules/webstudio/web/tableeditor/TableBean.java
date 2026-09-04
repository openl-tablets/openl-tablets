package org.openl.rules.webstudio.web.tableeditor;

import java.util.Map;
import java.util.regex.Pattern;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.env.PropertyResolver;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.service.TableServiceImpl;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUtils;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.validation.properties.dimentional.DispatcherTablesBuilder;
import org.openl.rules.webstudio.web.test.Utils;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.studio.common.utils.XSSFOptimizer;
import org.openl.types.IOpenMethod;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;

/**
 * Request scope managed bean for Table page.
 */
@Service
@RequestScope
@Slf4j
public class TableBean {
    private static final String REQUEST_ID_FORMAT = "request-id:%s;project-name:%s";
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("request-id:(.+);project-name:(.+)");

    private IOpenMethod method;

    // Test in current table (only for test tables)
    private TestDescription[] runnableTestMethods = {}; // test units
    private IOpenMethod[] tests = {};

    @Getter
    private String uri;
    @Getter
    private String id;
    @Getter
    private IOpenLTable table;
    @Getter
    private boolean editable;
    @Getter
    private boolean copyable;

    private final PropertyResolver propertyResolver;

    public TableBean(PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;

        id = WebStudioUtils.getRequestParameter(Constants.REQUEST_PARAM_ID);

        WebStudio studio = WebStudioUtils.getWebStudio();
        final var model = studio.getModel();

        table = model.getTableById(id);

        // TODO: There is should be a method to get the table by the ID without using URI which is used to generate the
        // ID.
        if (table == null) {
            table = model.getTable(studio.getTableUri());
        }

        if (table != null) {
            id = table.getId();
            uri = table.getUri();
            // Save URI because some actions don't provide table ID
            studio.setTableUri(uri);
            var currentOpenedModule = !model.isProjectCompilationCompleted();
            method = currentOpenedModule ? model.getOpenedModuleMethod(uri) : model.getMethod(uri);
            editable = model.isEditableTable(uri) && !isDispatcherValidationNode();
            copyable = editable && table
                    .isCanContainProperties() && !XlsNodeTypes.XLS_DATATYPE.toString().equals(table.getType());

            initTests(model, currentOpenedModule);

            // Save last visited table
            model.getRecentlyVisitedTables().setLastVisitedTable(table);
            // Check the save table parameter
            String saveTable1 = WebStudioUtils.getRequestParameter("saveTable");
            var saveTable = saveTable1 == null || Boolean.parseBoolean(saveTable1);
            if (saveTable) {
                storeTable();
            }
        }
    }

    private void storeTable() {
        ProjectModel model = WebStudioUtils.getProjectModel();
        var recentlyVisitedTables = model.getRecentlyVisitedTables();
        recentlyVisitedTables.add(table);
    }

    private void initTests(final ProjectModel model, boolean currentOpenedModule) {
        initRunnableTestMethods();
        tests = model.getTestMethods(uri, currentOpenedModule);
    }

    private void initRunnableTestMethods() {
        if (method instanceof TestSuiteMethod suiteMethod) {
            try {
                runnableTestMethods = suiteMethod.getTests();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                runnableTestMethods = new TestDescription[0];
            }
        }
    }

    public boolean isDispatcherValidationNode() {
        return table != null && table.getName().startsWith(DispatcherTablesBuilder.DEFAULT_DISPATCHER_TABLE_NAME);
    }

    public String getMode() {
        return isEditable() ? WebStudioUtils.getRequestParameter("mode") : null;
    }

    /**
     * Return test cases for current table.
     *
     * @return array of tests for current table.
     */
    public TestDescription[] getTests() {
        return runnableTestMethods;
    }

    public ParameterWithValueDeclaration[] getTestCaseParams(TestDescription testCase) {
        ParameterWithValueDeclaration[] params;
        if (testCase != null) {
            ParameterWithValueDeclaration[] contextParams = TestUtils
                    .getContextParams(new TestSuite((TestSuiteMethod) method), testCase);
            Utils.getDb(WebStudioUtils.getProjectModel(), false);
            var inputParams = testCase.getExecutionParams();

            params = new ParameterWithValueDeclaration[contextParams.length + inputParams.length];
            var n = 0;
            for (ParameterWithValueDeclaration contextParam : contextParams) {
                params[n++] = contextParam;
            }
            for (ParameterWithValueDeclaration inputParam : inputParams) {
                params[n++] = inputParam;
            }
        } else {
            params = ParameterWithValueDeclaration.EMPTY_ARRAY;
        }
        return params;
    }

    /**
     * @return true if it is possible to create tests for current table.
     */
    public boolean isCanCreateTest() {
        return table != null && table.isExecutable() && isEditable();
    }

    public boolean isTablePart() {
        return WebStudioUtils.getProjectModel().isTablePart(uri);
    }

    /**
     * Checks if there are runnable tests for current table.
     *
     * @return true if there are runnable tests for current table.
     */
    public boolean isTestable() {
        return runnableTestMethods.length > 0;
    }

    public boolean isHasTests() {
        return CollectionUtils.isNotEmpty(tests);
    }

    public String removeTable() throws Throwable {
        try {
            final WebStudio studio = WebStudioUtils.getWebStudio();
            var gridTable = table.getGridTable(IXlsTableNames.VIEW_DEVELOPER);

            gridTable.edit();
            new TableServiceImpl().removeTable(gridTable);
            var sheetModel = (XlsSheetGridModel) gridTable.getGrid();
            sheetModel.getSheetSource().getWorkbookSource().save();
            gridTable.stopEditing();
            WebStudioUtils.getExternalContext()
                    .getSessionMap()
                    .remove(org.openl.rules.tableeditor.util.Constants.TABLE_EDITOR_MODEL_NAME);

            studio.compile();
            var visitedTables = studio.getModel().getRecentlyVisitedTables();
            visitedTables.remove(table);
        } catch (Exception e) {
            throw e.getCause() == null ? e : e.getCause();
        }
        return null;
    }

    public boolean beforeEditAction() {
        final WebStudio studio = WebStudioUtils.getWebStudio();
        var currentProject = studio.getCurrentProject();
        if (currentProject != null) {
            try {
                return currentProject.tryLock();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return false;
            }
        }

        return true;
    }

    public boolean beforeSaveAction() {
        final WebStudio studio = WebStudioUtils.getWebStudio();
        studio.freezeProject(studio.getCurrentProject().getName());
        String editorId = WebStudioUtils
                .getRequestParameter(org.openl.rules.tableeditor.util.Constants.REQUEST_PARAM_EDITOR_ID);

        var editorModelMap = (Map<?, ?>) WebStudioUtils.getExternalContext()
                .getSessionMap()
                .get(org.openl.rules.tableeditor.util.Constants.TABLE_EDITOR_MODEL_NAME);

        var editorModel = (TableEditorModel) editorModelMap.get(editorId);

        var workbook = editorModel.getSheetSource().getWorkbookSource().getWorkbook();
        if (workbook instanceof XSSFWorkbook fWorkbook) {
            XSSFOptimizer.removeUnusedStyles(fWorkbook);
        }

        if (studio.isUpdateSystemProperties()) {
            return EditHelper.updateSystemProperties(table, editorModel, propertyResolver.getProperty("user.mode"));
        }
        return true;
    }

    public void afterSaveAction(String newId) {
        final WebStudio studio = WebStudioUtils.getWebStudio();
        studio.releaseProject(studio.getCurrentProject().getName());
        studio.compile();
    }

    public String getRequestId() {
        final WebStudio studio = WebStudioUtils.getWebStudio();
        var currentProject = studio.getCurrentProject();
        String requestId = currentProject == null ? "" : currentProject.getRepository().getId();
        String projectName = currentProject == null ? "" : currentProject.getName();
        return REQUEST_ID_FORMAT.formatted(requestId, projectName);
    }

    public static void tryUnlock(String requestId) {
        if (StringUtils.isBlank(requestId)) {
            return;
        }
        var matcher = REQUEST_ID_PATTERN.matcher(requestId);
        if (!matcher.matches()) {
            return;
        }

        var repositoryId = matcher.group(1);
        var projectName = matcher.group(2);

        final WebStudio studio = WebStudioUtils.getWebStudio();
        var currentProject = studio.getProject(repositoryId, projectName);
        if (currentProject != null) {
            try {
                if (!currentProject.isModified()) {
                    currentProject.releaseMyLock();
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public boolean getCanRun() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        var currentProject = studio.getCurrentProject();
        if (currentProject == null) {
            return false;
        }
        return currentProject.hasArtefact(studio.getCurrentModule().getRulesRootPath());
    }

    public boolean getCanBenchmark() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        var currentProject = studio.getCurrentProject();
        if (currentProject == null) {
            return false;
        }
        return currentProject.hasArtefact(studio.getCurrentModule().getRulesRootPath());
    }

    public Integer getRowIndex() {
        if (runnableTestMethods.length > 0 && !runnableTestMethods[0].hasId()) {
            if (method instanceof TestSuiteMethod suiteMethod) {
                var boundNode = suiteMethod.getBoundNode();
                if (boundNode != null && !boundNode.getTable().getHeaderTable().isNormalOrientation()) {
                    // Currently row indexes aren't supported for transposed test tables
                    return null;
                }
            }

            return table.getGridTable().getHeight() - runnableTestMethods.length + 1;
        }
        return null;
    }

    @RequiredArgsConstructor
    public static class TableDescription {
        @Getter
        private final String uri;
        @Getter
        private final String id;
        @Getter
        private final String name;
    }
}
