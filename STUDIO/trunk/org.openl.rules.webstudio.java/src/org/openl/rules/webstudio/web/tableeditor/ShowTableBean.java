package org.openl.rules.webstudio.web.tableeditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.CompiledOpenClass;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.commons.web.util.WebTool;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.OpenLWarnMessage;
import org.openl.message.Severity;
import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.service.TableServiceException;
import org.openl.rules.service.TableServiceImpl;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ITable;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.properties.def.TablePropertyDefinition.SystemValuePolicy;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.ui.tests.results.RanTestsResults;
import org.openl.rules.ui.tests.results.Test;
import org.openl.rules.validation.properties.dimentional.DispatcherTableBuilder;
import org.openl.rules.webstudio.properties.SystemValuesManager;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.syntax.ISyntaxNode;
import org.openl.util.StringTool;

/**
 * Request scope managed bean for showTable page.
 */
public class ShowTableBean {

    private static final Log LOG = LogFactory.getLog(ShowTableBean.class);

    // Filled and runnable tests(this group of tests is more tight than allTests).
    private Test[] runnableTestMethods = {};
    
    // All checks and tests for current table (including tests with no cases, run methods).
    private Test[] allTests = {};
    
    private boolean runnable;    
    private List<ITable> targetTables;

    private String uri;
    private ITable table;

    private List<OpenLMessage> errors;
    private List<OpenLMessage> warnings;
    // Errors + Warnings
    private List<OpenLMessage> problems;

    private String notViewParams;
    private String paramsWithoutShowFormulas;
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

        initProblems();

        initTests(model);

        runnable = model.isRunnable(uri); 
        
        if (runnable) {
            targetTables = model.getTargetTables(uri);
        }

        initParams();
    }
    
    @SuppressWarnings("unchecked")
    private void initParams() {
        Map paramMap = new HashMap(FacesUtils.getRequestParameterMap());
        for (Map.Entry entry : (Set<Map.Entry>) paramMap.entrySet()) {
            if (entry.getValue() instanceof String) {
                entry.setValue(new String[] { (String) entry.getValue() });
            }
        }

        notViewParams = WebTool.listRequestParams(paramMap, new String[] { "transparency", "filterType", "view" });
        paramsWithoutUri = WebTool.listRequestParams(paramMap, new String[] { "uri", "mode" });
        paramsWithoutShowFormulas = WebTool.listRequestParams(
                paramMap, new String[] { "transparency", "filterType", "showFormulas" });
    }

    private void initTests(final ProjectModel model) {
        initRunnableTestMethods(model);
        
        initAllTests(model);
    }

    private void initAllTests(final ProjectModel model) {
        RanTestsResults allTestRunner = model.getAllTestMethods(uri);
        if (allTestRunner != null) {
            allTests = allTestRunner.getTests();
        }
    }

    private void initRunnableTestMethods(final ProjectModel model) {
        RanTestsResults testsRunner = model.getTestMethods(uri);
        if (testsRunner != null) {
            runnableTestMethods =  testsRunner.getTests();
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

        WebStudio studio = WebStudioUtils.getWebStudio();
        ProjectModel model = studio.getModel();

        CompiledOpenClass compiledOpenClass = model.getCompiledOpenClass();

        List<OpenLMessage> messages = compiledOpenClass.getMessages();
        List<OpenLMessage> warningMessages = OpenLMessagesUtils.filterMessagesBySeverity(messages, Severity.WARN);
        for (OpenLMessage message : warningMessages) {
            OpenLWarnMessage warning = (OpenLWarnMessage) message;
            ISyntaxNode syntaxNode = warning.getSource();
            if (syntaxNode instanceof TableSyntaxNode
                     && ((TableSyntaxNode) syntaxNode).getUri().equals(uri)) {
                warnings.add(warning);
            }
        }
    }
    
    private boolean isDispatcherValidationNode() {
        return ((TableSyntaxNodeAdapter) table).getNameFromHeader().startsWith(
                DispatcherTableBuilder.DEFAULT_DISPATCHER_TABLE_NAME);
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

    public String getNotViewParams() {
        return notViewParams;
    }

    public String getParamsWithoutUri() {
        return paramsWithoutUri;
    }

    public String getParamsWithoutShowFormulas() {
        return paramsWithoutShowFormulas;
    }

    public ITable getTable() {
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
     * Gets the results for run methods.
     * 
     * @return results of run methods.
     */
    public TestRunsResultBean getTestRunResults() {
        RanTestsResults atr = WebStudioUtils.getProjectModel().getRunMethods(uri);
        Test[] tests = null;
        if (atr != null) {
            tests = atr.getTests();
        }
        return new TestRunsResultBean(tests);
    }
    
    /**
     * Return test methods for current table. Test methods are methods with test cases. 
     * 
     * @return array of tests for current table. 
     */
    public Test[] getTests() {
        return runnableTestMethods;
    }

    public String getUri() {
        return uri;
    }

    public List<ITable> getTargetTables() {
        return targetTables;
    }

    public String getView() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        return studio.getModel().getTableView(FacesUtils.getRequestParameter("view"));
    }

    public boolean isCopyable() {
        ProjectModel projectModel = WebStudioUtils.getProjectModel();
        return projectModel.isEditable() && !isServiceTable()  && !isDispatcherValidationNode();
    }

    public boolean isServiceTable() {
        String tableType = table.getType();
        if (ITableNodeTypes.XLS_ENVIRONMENT.equals(tableType)
                || ITableNodeTypes.XLS_OTHER.equals(tableType)
                || ITableNodeTypes.XLS_PROPERTIES.equals(tableType)
                || ITableNodeTypes.XLS_DATATYPE.equals(tableType)) {
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

    public boolean isEditableAsNewVersion() {
        return isEditable() && !isServiceTable();
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
        return allTests.length > 0;
    }
    
    /**
     * Gets all tests, including tests with test cases, runs with filled runs, tests without cases(empty),
     * runs without any parameters and tests without cases and runs.
     */
    public Test[] getAllTests() {
        return allTests;
    }

    public void setAllTests(Test[] allTests) {
        this.allTests = allTests;
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
    
    public boolean isCollapseProperties() {
        String collapseProperties = FacesUtils.getRequestParameter("collapseProperties");
        if (collapseProperties != null) {
            return Boolean.parseBoolean(collapseProperties);
        } else {
            WebStudio webStudio = WebStudioUtils.getWebStudio();
            return webStudio != null && webStudio.isCollapseProperties();
        }
    }

    public String removeTable() {
        final WebStudio studio = WebStudioUtils.getWebStudio();
        IGridTable gridTable = table.getGridTable(IXlsTableNames.VIEW_DEVELOPER);
        try {
            new TableServiceImpl(true).removeTable(gridTable);
            studio.rebuildModel();
        } catch (TableServiceException e) {
            e.printStackTrace();
            // TODO UI exception
            return null;
        }
        return "mainPage";
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

    public static class TestRunsResultBean {

        private Test[] tests;

        private TestProxy[] proxies;

        public TestRunsResultBean(Test[] tests) {
            this.tests = tests;
            if (tests == null) {
                proxies = new TestProxy[0];
            } else {
                proxies = new TestProxy[tests.length];
            }

            for (int i = 0; i < proxies.length; i++) {
                proxies[i] = new TestProxy(i);
            }
        }

        public TestProxy[] getTests() {
            return proxies;
        }

        public boolean isNotEmpty() {
            if (ArrayUtils.isNotEmpty(tests)) {
                for (TestProxy testProxy : getTests()) {
                    String[] descriptions = testProxy.getDescriptions();
                    if (ArrayUtils.isNotEmpty(descriptions)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public class TestProxy {

            int index;

            public TestProxy(int index) {
                this.index = index;
            }

            public String[] getDescriptions() {
                Test test = getTest();
                String[] descriptions = new String[test.ntests()];
                for (int i = 0; i < descriptions.length; i++) {
                    descriptions[i] = test.getTestDescription(i);
                }
                return descriptions;
            }

            private Test getTest() {
                return tests[index];
            }

            public String getTestName() {
                return StringTool.encodeURL(getTest().getTestName());
            }
        }

    }
}
