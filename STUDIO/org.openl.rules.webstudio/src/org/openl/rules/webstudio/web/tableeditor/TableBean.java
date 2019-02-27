package org.openl.rules.webstudio.web.tableeditor;

import static org.openl.rules.security.AccessManager.isGranted;
import static org.openl.rules.security.Privileges.BENCHMARK;
import static org.openl.rules.security.Privileges.CREATE_TABLES;
import static org.openl.rules.security.Privileges.EDIT_TABLES;
import static org.openl.rules.security.Privileges.REMOVE_TABLES;
import static org.openl.rules.security.Privileges.RUN;
import static org.openl.rules.security.Privileges.TRACE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.OpenLWarnMessage;
import org.openl.message.Severity;
import org.openl.rules.data.IDataBase;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.service.TableServiceImpl;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.table.xls.XlsUrlUtils;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.testmethod.ProjectHelper;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestMethodBoundNode;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUtils;
import org.openl.rules.types.IUriMember;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.RecentlyVisitedTables;
import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.validation.properties.dimentional.DispatcherTablesBuilder;
import org.openl.rules.webstudio.util.XSSFOptimizer;
import org.openl.rules.webstudio.web.test.Utils;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenMethod;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Request scope managed bean for Table page.
 */
@ManagedBean
@RequestScoped
public class TableBean {
    private static final String REQUEST_ID_PREFIX = "project-";
    private final Logger log = LoggerFactory.getLogger(TableBean.class);

    private IOpenMethod method;

    // Test in current table (only for test tables)
    private TestDescription[] runnableTestMethods = {}; //test units
    // All checks and tests for current table (including tests with no cases, run methods).
    private IOpenMethod[] allTests = {};
    private IOpenMethod[] tests = {};

    private List<IOpenLTable> targetTables;

    private String uri;
    private String id;
    private IOpenLTable table;
    private boolean editable;
    private boolean canBeOpenInExcel;
    private boolean copyable;

    private Collection<OpenLMessage> errors;
    private Collection<OpenLMessage> warnings;
    // Errors + Warnings
    private List<OpenLMessage> problems;

    private boolean targetTablesHasErrors;
    
    public TableBean() {
        id = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_ID);

        WebStudio studio = WebStudioUtils.getWebStudio();
        final ProjectModel model = studio.getModel();

        table = model.getTableById(id);

        // TODO: There is should be a method to get the table by the ID without using URI which is used to generate the ID.
        if (table == null) {
            table = model.getTable(studio.getTableUri());
        }

        if (table != null) {
            id = table.getId();
            uri = table.getUri();
            // Save URI because some actions don't provide table ID
            studio.setTableUri(uri);

            method = model.getMethod(uri);

            editable = model.isEditableTable(uri) && !isDispatcherValidationNode();
            canBeOpenInExcel = model.isEditable() && !isDispatcherValidationNode();
            copyable = editable && table.isCanContainProperties()
                    && !XlsNodeTypes.XLS_DATATYPE.toString().equals(table.getType())
                    && isGranted(CREATE_TABLES);

            String tableType = table.getType();
            if (tableType.equals(XlsNodeTypes.XLS_TEST_METHOD.toString())
                    || tableType.equals(XlsNodeTypes.XLS_RUN_METHOD.toString())) {
                targetTables = model.getTargetTables(uri);
            }

            initProblems();
            initTests(model);

            // Save last visited table
            model.getRecentlyVisitedTables().setLastVisitedTable(table);
            // Check the save table parameter
            boolean saveTable = FacesUtils.getRequestParameterMap().get("saveTable") == null ? true :
                    Boolean.valueOf(FacesUtils.getRequestParameterMap().get("saveTable"));
            if (saveTable) {
                storeTable();
            }
        }
    }

    private void storeTable() {
        ProjectModel model = WebStudioUtils.getProjectModel();
        RecentlyVisitedTables recentlyVisitedTables = model.getRecentlyVisitedTables();
        recentlyVisitedTables.add(table);
    }

    private void initTests(final ProjectModel model) {
        initRunnableTestMethods();

        allTests = model.getTestAndRunMethods(uri);
        tests = model.getTestMethods(uri);
    }

    private void initRunnableTestMethods() {
        if (method instanceof TestSuiteMethod) {
            try {
                runnableTestMethods = ((TestSuiteMethod) method).getTests();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                runnableTestMethods = new TestDescription[0];
            }
        }
    }

    private void initProblems() {
        initErrors();
        initWarnings();

        problems = new ArrayList<OpenLMessage>();
        problems.addAll(errors);
        problems.addAll(warnings);
    }

    private void initErrors() {
        Collection<OpenLMessage> messages = table.getMessages();
        errors = OpenLMessagesUtils.filterMessagesBySeverity(messages, Severity.ERROR);
    }

    private void initWarnings() {
        warnings = new ArrayList<OpenLMessage>();
        
        if (targetTables != null) {
            boolean warningWasAdded = false;
            for (IOpenLTable targetTable : targetTables) {
                if (targetTable.getMessages().size() > 0) {
                    if (!warningWasAdded){
                        warnings.add(new OpenLMessage("Tested rules have errors", Severity.WARN));
                        warningWasAdded = true;
                    }
                    if (!OpenLMessagesUtils.filterMessagesBySeverity(targetTable.getMessages(), Severity.ERROR).isEmpty()){
                        targetTablesHasErrors = true;
                    }
                }
            }
        }

        ProjectModel model = WebStudioUtils.getProjectModel();

        Collection<OpenLMessage> warnMessages = OpenLMessagesUtils.filterMessagesBySeverity(model.getModuleMessages(), Severity.WARN);
        for (OpenLMessage message : warnMessages) {
            if (message instanceof OpenLWarnMessage) {//there can be simple OpenLMessages with severity WARN
                OpenLWarnMessage warning = (OpenLWarnMessage) message;
                ISyntaxNode syntaxNode = warning.getSource();
                if (syntaxNode instanceof TableSyntaxNode && ((TableSyntaxNode) syntaxNode).getUri().equals(table.getUri())) {
                    warnings.add(warning);
                } else {
                    String warnUri = warning.getSourceLocation();

                    XlsUrlParser uriParser = new XlsUrlParser();
                    uriParser.parse(warnUri);
                    if (XlsUrlUtils.intersects(uriParser, table.getUriParser())) {
                        warnings.add(warning);
                    }
                }
            }
        }
    }

    public String getTableName (IOpenLTable table) {
        String[] dimensionProps = TablePropertyDefinitionUtils.getDimensionalTablePropertiesNames();
        ITableProperties tableProps = table.getProperties();
        StringBuilder dimensionBuilder = new StringBuilder();
        String tableName = table.getDisplayName();
        if (tableProps != null) {
            for (String dimensionProp : dimensionProps) {
                String propValue = tableProps.getPropertyValueAsString(dimensionProp);

                if (propValue != null && !propValue.isEmpty()) {
                    dimensionBuilder.append(dimensionBuilder.length() == 0 ? "" : ", ").append(dimensionProp).append(" = ").append(propValue);
                }
            }
        }
        if (dimensionBuilder.length() > 0) {
            return tableName +" ["+ dimensionBuilder.toString() +"]";
        } else {
            return tableName;
        }
    }

    public final boolean isDispatcherValidationNode() {
        return table != null && table.getName().startsWith(DispatcherTablesBuilder.DEFAULT_DISPATCHER_TABLE_NAME);
    }

    public String getMode() {
        return getCanEdit() ? FacesUtils.getRequestParameter("mode") : null;
    }

    public IOpenLTable getTable() {
        return table;
    }

    public Collection<OpenLMessage> getErrors() {
        return errors;
    }

    public Collection<OpenLMessage> getWarnings() {
        return warnings;
    }

    public List<OpenLMessage> getProblems() {
        return problems;
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
            ParameterWithValueDeclaration[] contextParams = TestUtils.getContextParams(
                    new TestSuite((TestSuiteMethod) method), testCase);
            IDataBase db = Utils.getDb(WebStudioUtils.getProjectModel());
            ParameterWithValueDeclaration[] inputParams = testCase.getExecutionParams();

            params = new ParameterWithValueDeclaration[contextParams.length + inputParams.length];
            int n = 0;
            for (ParameterWithValueDeclaration contextParam : contextParams) {
                params[n++] = contextParam;
            }
            for (ParameterWithValueDeclaration inputParam : inputParams) {
                params[n++] = inputParam;
            }
        } else {
            params = new ParameterWithValueDeclaration[0];
        }
        return params;
    }

    public String getUri() {
        return uri;
    }

    public String getId() {
        return id;
    }

    public List<TableDescription> getTargetTables() {
        if (targetTables == null) {
            return  null;
        }
        List<TableDescription> tableDescriptions = new ArrayList<TableDescription>(targetTables.size());
        for (IOpenLTable targetTable : targetTables) {
            tableDescriptions.add(new TableDescription(targetTable.getUri(),
                    targetTable.getId(),
                    getTableName(targetTable)));
        }
        return tableDescriptions;
    }

    /**
     * 
     * @return true if it is possible to create tests for current table.
     */
    public boolean isCanCreateTest() {
        return table != null && table.isExecutable() && isEditable() && isGranted(CREATE_TABLES);
    }

    public boolean isEditable() {
        return editable;
    }

    public boolean isCopyable() {
        return copyable;
    }

    public boolean isTablePart() {
        return WebStudioUtils.getProjectModel().isTablePart(uri);
    }

    public boolean isHasErrors() {
        return CollectionUtils.isNotEmpty(errors);
    }

    public boolean isHasWarnings() {
        return CollectionUtils.isNotEmpty(warnings);
    }

    public boolean isHasProblems() {
        return isHasErrors() || isHasWarnings();
    }

    /**
     * Checks if there are runnable tests for current table.
     * 
     * @return true if there are runnable tests for current table.
     */
    public boolean isTestable() {
        return runnableTestMethods.length > 0;
    }
    
    /**
     * Checks if there are tests, including tests with test cases, runs with filled runs, tests without cases(empty),
     * runs without any parameters and tests without cases and runs.
     */
    public boolean isHasAnyTests() {
        return CollectionUtils.isNotEmpty(allTests);
    }

    public boolean isHasTests() {
        return CollectionUtils.isNotEmpty(tests);
    }

    /**
     * Gets all tests for current table.
     */
    public TableDescription[] getAllTests() {
        if (allTests == null) {
            return null;
        }
        List<TableDescription> tableDescriptions = new ArrayList<TableDescription>(allTests.length);
        for (IOpenMethod test : allTests) {
            String tableUri = ((IUriMember) test).getUri();
            TableSyntaxNode syntaxNode = (TableSyntaxNode) test.getInfo().getSyntaxNode();
            tableDescriptions.add(new TableDescription(tableUri, syntaxNode.getId(), getTestName(test)));
        }
        Collections.sort(tableDescriptions, new Comparator<TableDescription>() {
            @Override
            public int compare(TableDescription o1, TableDescription o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return tableDescriptions.toArray(new TableDescription[tableDescriptions.size()]);
    }
    
    public String getTestName(Object testMethod){
        IOpenMethod method = (IOpenMethod) testMethod;
        String name = TableSyntaxNodeUtils.getTestName(method);
        String info = ProjectHelper.getTestInfo(method);
        return String.format("%s (%s)", name, info);
    }

    public String removeTable() throws Throwable {
        try {
            final WebStudio studio = WebStudioUtils.getWebStudio();
            IGridTable gridTable = table.getGridTable(IXlsTableNames.VIEW_DEVELOPER);

            new TableServiceImpl().removeTable(gridTable);
            XlsSheetGridModel sheetModel = (XlsSheetGridModel) gridTable.getGrid();
            sheetModel.getSheetSource().getWorkbookSource().save();
            studio.compile();
            RecentlyVisitedTables visitedTables = studio.getModel().getRecentlyVisitedTables();
            visitedTables.remove(table);
        } catch (Exception e) {
            throw e.getCause();
        }
        return null;
    }

    public boolean beforeEditAction() {
        final WebStudio studio = WebStudioUtils.getWebStudio();
        RulesProject currentProject = studio.getCurrentProject();
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
        String editorId = FacesUtils.getRequestParameter(
                org.openl.rules.tableeditor.util.Constants.REQUEST_PARAM_EDITOR_ID);

        Map<?, ?> editorModelMap = (Map<?, ?>) FacesUtils.getSessionParam(
                org.openl.rules.tableeditor.util.Constants.TABLE_EDITOR_MODEL_NAME);

        TableEditorModel editorModel = (TableEditorModel) editorModelMap.get(editorId);

        Workbook workbook = editorModel.getSheetSource().getWorkbookSource().getWorkbook();
        if (workbook instanceof XSSFWorkbook) {
            XSSFOptimizer.removeUnusedStyles((XSSFWorkbook) workbook);
        }

        if (WebStudioUtils.getWebStudio().isUpdateSystemProperties()) {
            return EditHelper.updateSystemProperties(table, editorModel,
                    WebStudioUtils.getWebStudio().getSystemConfigManager().getStringProperty("user.mode"));
        }
        return true;
    }

    public void afterSaveAction(String newId) {
        final WebStudio studio = WebStudioUtils.getWebStudio();
        studio.compile();
    }

    public String getRequestId() {
        final WebStudio studio = WebStudioUtils.getWebStudio();
        RulesProject currentProject = studio.getCurrentProject();
        String projectName = currentProject == null ? "" : currentProject.getName();
        return REQUEST_ID_PREFIX + projectName;
    }

    public static void tryUnlock(String requestId) {
        if (StringUtils.isBlank(requestId) || !requestId.startsWith(REQUEST_ID_PREFIX)) {
            return;
        }

        String projectName = requestId.substring(REQUEST_ID_PREFIX.length());

        final WebStudio studio = WebStudioUtils.getWebStudio();
        RulesProject currentProject = studio.getProject(projectName);
        if (currentProject != null) {
            try {
                if (!currentProject.isModified()) {
                    if (currentProject.isLockedByMe()) {
                        currentProject.unlock();
                    }
                }
            } catch (Exception e) {
                Logger logger = LoggerFactory.getLogger(TableBean.class);
                logger.error(e.getMessage(), e);
            }
        }
    }

    public boolean getCanEdit() {
        return isEditable() && isGranted(EDIT_TABLES);
    }

    public boolean isCanOpenInExcel() {
        return canBeOpenInExcel;
    }

    public boolean getCanRemove() {
        return isEditable() && isGranted(REMOVE_TABLES);
    }
    
    public boolean hasErrorsInTargetTables(){
        return targetTablesHasErrors;
    }

    public boolean getCanRun() {
        return isGranted(RUN) && !hasErrorsInTargetTables() && !isHasErrors();
    }

    public boolean getCanTrace() {
        return isGranted(TRACE) && !hasErrorsInTargetTables() && !isHasErrors();
    }

    public boolean getCanBenchmark() {
        return isGranted(BENCHMARK) && !hasErrorsInTargetTables() && !isHasErrors();
    }

    public Integer getRowIndex() {
        if (runnableTestMethods.length > 0 && !runnableTestMethods[0].hasId()) {
            if (method instanceof TestSuiteMethod) {
                TestMethodBoundNode boundNode = ((TestSuiteMethod) method).getBoundNode();
                if (boundNode != null && !boundNode.getTable().getHeaderTable().isNormalOrientation()) {
                    // Currently row indexes aren't supported for transposed test tables
                    return null;
                }
            }

            return table.getGridTable().getHeight() - runnableTestMethods.length + 1;
        }
        return null;
    }

    public static class TableDescription {
        private final String uri;
        private final String id;
        private final String name;

        public TableDescription(String uri, String id, String name) {
            this.uri = uri;
            this.id = id;
            this.name = name;
        }

        public String getUri() {
            return uri;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}
