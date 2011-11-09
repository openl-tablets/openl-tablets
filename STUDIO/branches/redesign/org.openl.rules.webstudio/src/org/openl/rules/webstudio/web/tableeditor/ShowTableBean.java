package org.openl.rules.webstudio.web.tableeditor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.CompiledOpenClass;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.commons.web.util.WebTool;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.OpenLWarnMessage;
import org.openl.message.Severity;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.service.TableServiceException;
import org.openl.rules.service.TableServiceImpl;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.properties.def.TablePropertyDefinition.SystemValuePolicy;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.RecentlyVisitedTables;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.validation.properties.dimentional.DispatcherTablesBuilder;
import org.openl.rules.webstudio.properties.SystemValuesManager;
import org.openl.rules.webstudio.web.trace.TraceIntoFileBean;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenMethod;
import org.openl.util.StringTool;

/**
 * Request scope managed bean for showTable page.
 */
@ManagedBean
@RequestScoped
public class ShowTableBean {

    private static final Log LOG = LogFactory.getLog(ShowTableBean.class);
    
    private static final String INFO_MESSAGE = "Can`t find requested table in current module";

    // Test in current table(only for test tables)
    private TestDescription[] runnableTestMethods = {};//test units
    private Map<TestDescription, Boolean> selectedTests;
    // All checks and tests for current table (including tests with no cases, run methods).
    private IOpenMethod[] allTests = {};
    
    private boolean runnable;    
    private List<IOpenLTable> targetTables;

    private String uri;
    private IOpenLTable table;

    private List<OpenLMessage> errors;
    private List<OpenLMessage> warnings;
    // Errors + Warnings
    private List<OpenLMessage> problems;

    private String paramsWithoutUri;

    public ShowTableBean() {
        uri = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_URI);

        WebStudio studio = WebStudioUtils.getWebStudio();

        if (uri == null) {
            uri = studio.getTableUri();
        } else {
            studio.setTableUri(uri);
        }

        final ProjectModel model = studio.getModel();

        table = model.getTable(uri);

        if (table == null) {
            try {
                String infoLink = 
                    String.format("%s/faces/pages/common/message.xhtml?summary=%s", 
                        FacesUtils.getContextPath(), INFO_MESSAGE);

                FacesUtils.redirect(infoLink);
            } catch (IOException e) {                
                LOG.error("Can`t redirect to info message page", e);
            }
        } else {
            initProblems();
            initTests(model);        
            initParams();

            runnable = !model.isMethodHasParams(uri)
                && (ArrayUtils.isNotEmpty(runnableTestMethods) || table.isExecutable());

            String tableType = table.getType();
            if (tableType.equals(XlsNodeTypes.XLS_TEST_METHOD.toString())
                    || tableType.equals(XlsNodeTypes.XLS_RUN_METHOD.toString())) {
                targetTables = model.getTargetTables(uri);
            }

            storeTable();
        }
    }

    private void storeTable() {
        ProjectModel model = WebStudioUtils.getProjectModel();
        RecentlyVisitedTables recentlyVisitedTables = model.getRecentlyVisitedTables();
        recentlyVisitedTables.add(table);
    }

    @SuppressWarnings("unchecked")
    private void initParams() {
        Map paramMap = new HashMap(FacesUtils.getRequestParameterMap());
        for (Map.Entry entry : (Set<Map.Entry>) paramMap.entrySet()) {
            if (entry.getValue() instanceof String) {
                entry.setValue(new String[] { (String) entry.getValue() });
            }
        }

        paramsWithoutUri = WebTool.listRequestParams(paramMap, new String[] { "uri", "mode" });
    }

    private void initTests(final ProjectModel model) {
        initRunnableTestMethods(model);
        
        initAllTests(model);
    }

    private void initAllTests(final ProjectModel model) {
        allTests = model.getAllTestMethods(uri);
    }

    private void initRunnableTestMethods(final ProjectModel model) {
        if (model.getMethod(uri) instanceof TestSuiteMethod) {
            runnableTestMethods = ((TestSuiteMethod) model.getMethod(uri)).getTests();
            selectedTests = new HashMap<TestDescription, Boolean>();
            for(TestDescription test : runnableTestMethods){
                selectedTests.put(test, false);
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
        List<OpenLMessage> messages = table.getMessages();
        errors = OpenLMessagesUtils.filterMessagesBySeverity(messages, Severity.ERROR);
    }

    private void initWarnings() {
        warnings = new ArrayList<OpenLMessage>();
        
        if (targetTables != null) {
            for (IOpenLTable targetTable : targetTables) {
                if (targetTable.getMessages().size() > 0) {
                    warnings.add(new OpenLMessage("Tested rules have errors", StringUtils.EMPTY, Severity.WARN));
                    // one warning is enough.
                    break;
                }
            }
        }

        WebStudio studio = WebStudioUtils.getWebStudio();
        ProjectModel model = studio.getModel();

        CompiledOpenClass compiledOpenClass = model.getCompiledOpenClass();

        List<OpenLMessage> messages = compiledOpenClass.getMessages();
        List<OpenLMessage> warningMessages = OpenLMessagesUtils.filterMessagesBySeverity(messages, Severity.WARN);
        for (OpenLMessage message : warningMessages) {
            if (message instanceof OpenLWarnMessage) {//there can be simple OpenLMessages with severity WARN
                OpenLWarnMessage warning = (OpenLWarnMessage) message;
                ISyntaxNode syntaxNode = warning.getSource();
                if (syntaxNode instanceof TableSyntaxNode && ((TableSyntaxNode) syntaxNode).getUri().equals(uri)) {
                    warnings.add(warning);
                }
            }
        }
    }
    
    private boolean isDispatcherValidationNode() {
        return ((TableSyntaxNodeAdapter) table).getNameFromHeader().startsWith(
                DispatcherTablesBuilder.DEFAULT_DISPATCHER_TABLE_NAME);
    }
    
    private boolean updateSystemValue(TableEditorModel editorModel, TablePropertyDefinition sysProperty) {
        boolean result = false;
        String systemValue = null;

        if (sysProperty.getSystemValuePolicy().equals(SystemValuePolicy.ON_EACH_EDIT)) {
            systemValue = SystemValuesManager.getInstance().getSystemValueString(
                    sysProperty.getSystemValueDescriptor());
            if (systemValue != null) {
                try {
                    editorModel.setProperty(sysProperty.getName(), systemValue);
                    result = true;
                } catch (Exception e) {
                    LOG.error(String.format("Can`t update system property '%s' with value '%s'", sysProperty.getName(),
                            systemValue), e);
                }
            }
        }
        return result;
    }

    public String getEncodedUri() {
        return StringTool.encodeURL(uri);
    }

    public String getMode() {
        return FacesUtils.getRequestParameter("mode");
    }

    public String getParamsWithoutUri() {
        return paramsWithoutUri;
    }

    public IOpenLTable getTable() {
        return table;
    }

    public List<OpenLMessage> getErrors() {
        return errors;
    }

    public List<OpenLMessage> getWarnings() {
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

    public Map<TestDescription, Boolean> getSelectedTests() {
        return selectedTests;
    }
    
    public String makeTestSuite(){
        WebStudio studio = WebStudioUtils.getWebStudio();
        TestSuiteMethod testSuiteMethodSelected = (TestSuiteMethod) studio.getModel().getMethod(uri);
        TestSuite testSuite = new TestSuite(testSuiteMethodSelected, getSelectedIndices());
        studio.getModel().setLastTest(testSuite);
        return null;
    }

    public String traceIntoFile(){
        makeTestSuite();
        return ((TraceIntoFileBean)FacesUtils.getBackingBean("traceIntoFileBean")).traceIntoFile();
    }

    public int[] getSelectedIndices() {
        ArrayList<Integer> list = new ArrayList<Integer>();
        for(int i = 0; i < runnableTestMethods.length; i++){
            if(selectedTests.get(runnableTestMethods[i])){
                list.add(i);
            }
        }
        Integer[] indices = new Integer[list.size()];
        return ArrayUtils.toPrimitive(list.toArray(indices));
    }

    public String getUri() {
        return uri;
    }

    public List<IOpenLTable> getTargetTables() {
        return targetTables;
    }

    public boolean isCopyable() {
        ProjectModel projectModel = WebStudioUtils.getProjectModel();
        return projectModel.isEditable() && !isServiceTable()  && !XlsNodeTypes.XLS_DATATYPE.toString().equals(table.getType()) 
            && !isDispatcherValidationNode();
    }

    public boolean isServiceTable() {
        String tableType = table.getType();
        if (XlsNodeTypes.XLS_ENVIRONMENT.toString().equals(tableType)
                || XlsNodeTypes.XLS_OTHER.toString().equals(tableType)
                || XlsNodeTypes.XLS_PROPERTIES.toString().equals(tableType)) {
            return true;
        }
        return false;
    }

    /**
     * 
     * @return true if it is possible to create tests for current table.
     */
    public boolean isCanCreateTest() {
        return table.isExecutable() && isEditable();
    }    

    public boolean isEditable() {
        ProjectModel projectModel = WebStudioUtils.getProjectModel();
        return projectModel.isEditable() && !isDispatcherValidationNode();
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
     * Checks if table can be run.
     * Runnable tables:
     *   - Test and Run tables;
     *   - Executable tables without parameters.
     */
    public boolean isRunnable() {
        return runnable;
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
        return ArrayUtils.isNotEmpty(allTests);
    }
    
    /**
     * Gets all tests for current table.
     */
    public IOpenMethod[] getAllTests() {
        return allTests;
    }

    public boolean isShowFormulas() {
        String showFormulas = FacesUtils.getRequestParameter("showFormulas");
        if (showFormulas != null) {
            return Boolean.parseBoolean(showFormulas);
        } else {
            WebStudio webStudio = WebStudioUtils.getWebStudio();
            return webStudio != null && webStudio.isShowFormulas();
        }
    }

    public String removeTable() {
        final WebStudio studio = WebStudioUtils.getWebStudio();
        IGridTable gridTable = table.getGridTable(IXlsTableNames.VIEW_DEVELOPER);
        try {
            new TableServiceImpl(true).removeTable(gridTable);
            studio.rebuildModel();
            RecentlyVisitedTables visitedTables = studio.getModel().getRecentlyVisitedTables();
            visitedTables.getTables().remove(table);
        } catch (TableServiceException e) {
            e.printStackTrace();
            // TODO UI exception
            return null;
        }
        return "mainPage";
    }

    public boolean beforeSaveAction() {
        return updateSystemProperties();
    }

    public void afterSaveAction(String newUri) {
        final WebStudio studio = WebStudioUtils.getWebStudio();
        studio.setTableUri(newUri);
        studio.rebuildModel();
    }

    @SuppressWarnings("unchecked")
    public boolean updateSystemProperties() {
        boolean result = true;
        if (!isServiceTable()) {
            String editorId = FacesUtils.getRequestParameter(
                    org.openl.rules.tableeditor.util.Constants.REQUEST_PARAM_EDITOR_ID);

            Map editorModelMap = (Map) FacesUtils.getSessionParam(
                    org.openl.rules.tableeditor.util.Constants.TABLE_EDITOR_MODEL_NAME);

            TableEditorModel editorModel = (TableEditorModel) editorModelMap.get(editorId);

            List<TablePropertyDefinition> sysProps = TablePropertyDefinitionUtils.getSystemProperties();

            for (TablePropertyDefinition sysProperty : sysProps) {
                result = updateSystemValue(editorModel, sysProperty);
            }
        } 
        return result;
    }    

    public String getTreeNodeId() {
        final WebStudio studio = WebStudioUtils.getWebStudio();
        String id = studio.getModel().getTreeNodeId(getUri());
        return id;
    }
    
    public void switchView() {
        String view = FacesUtils.getRequestParameter("view");
        final WebStudio studio = WebStudioUtils.getWebStudio();
        studio.setTableView(view);
    }

    public void setShowFormulas() {
        final WebStudio studio = WebStudioUtils.getWebStudio();
        studio.setShowFormulas(!studio.isShowFormulas());
    }

    public void setCollapseProperties() {
        final WebStudio studio = WebStudioUtils.getWebStudio();
        studio.setCollapseProperties(!studio.isCollapseProperties());
    }

    public static class TestRunsResultBean {

        private TestSuite[] tests;

        public TestRunsResultBean(TestSuite[] tests) {
            this.tests = tests;
        }

        public TestSuite[] getTests() {
            return tests;
        }

        public boolean isNotEmpty() {
            if (ArrayUtils.isNotEmpty(tests)) {
                for (TestSuite test : getTests()) {
                    if (ArrayUtils.isNotEmpty(test.getTests())) {
                        return true;
                    }
                }
            }
            return false;
        }
        
    }
}
