package org.openl.rules.webstudio.web.tableeditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.CompiledOpenClass;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.OpenLWarnMessage;
import org.openl.message.Severity;
import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.service.TableServiceException;
import org.openl.rules.service.TableServiceImpl;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ITable;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.properties.def.TablePropertyDefinition.SystemValuePolicy;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.ui.AllTestsRunResult;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.validation.properties.dimentional.DispatcherTableBuilder;
import org.openl.rules.web.jsf.util.FacesUtils;
import org.openl.rules.webstudio.properties.SystemValuesManager;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.webtools.WebTool;
import org.openl.rules.workspace.uw.UserWorkspaceProject;
import org.openl.syntax.ISyntaxNode;
import org.openl.util.StringTool;

/**
 * Request scope managed bean for showTable page.
 */
public class ShowTableBean {

    private static final Log LOG = LogFactory.getLog(ShowTableBean.class);

    private String url;
    private boolean runnable;
    private boolean testable;
    private String uri;
    private ITable table;

    private List<OpenLMessage> errors;
    private List<OpenLMessage> warnings;
    // Errors + Warnings
    private List<OpenLMessage> problems;

    private String notViewParams;
    private String paramsWithoutShowFormulas;
    private String paramsWithoutUri;

    @SuppressWarnings("unchecked")
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

        url = model.makeXlsUrl(uri);
        runnable = model.isRunnable(uri);
        testable = model.isTestable(uri);

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

    public boolean canModifyCurrentProject() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        UserWorkspaceProject currentProject = studio.getCurrentProject();

        if (currentProject != null) {
            return currentProject.isCheckedOut() || currentProject.isLocalOnly();
        }

        return false;
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

        CompiledOpenClass compiledOpenClass = model.getWrapper().getCompiledOpenClass();

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

    public TestRunsResultBean getTestRunResults() {
        AllTestsRunResult atr = WebStudioUtils.getWebStudio().getModel().getRunMethods(uri);
        AllTestsRunResult.Test[] tests = null;
        if (atr != null) {
            tests = atr.getTests();
        }
        return new TestRunsResultBean(tests);
    }

    public String getUri() {
        return uri;
    }

    public String getUrl() {
        return url;
    }

    public String getView() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        return studio.getModel().getTableView(FacesUtils.getRequestParameter("view"));
    }

    public boolean isCopyable() {        
        return canModifyCurrentProject() && !isServiceTable()  && !isDispatcherValidationNode();
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

    private boolean isDispatcherValidationNode() {
        return table.getNameFromHeader().startsWith(DispatcherTableBuilder.DEFAULT_METHOD_NAME);
    }

    public boolean isEditable() {
        return canModifyCurrentProject() && !isDispatcherValidationNode();
    }
    
    public boolean isEditableAsNewVersion() {
        return canModifyCurrentProject() && !isServiceTable() && !isDispatcherValidationNode();
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

    public boolean isTestable() {
        return testable;
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

    public String getTreeNodeId() {
        final WebStudio studio = WebStudioUtils.getWebStudio();
        String id = studio.getModel().getTreeNodeId(getUri());
        return id;
    }

    public static class TestRunsResultBean {

        private AllTestsRunResult.Test[] tests;

        private TestProxy[] proxies;

        public TestRunsResultBean(AllTestsRunResult.Test[] tests) {
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
            return tests != null && tests.length > 0;
        }

        public class TestProxy {

            int index;

            public TestProxy(int index) {
                this.index = index;
            }

            public String[] getDescriptions() {
                AllTestsRunResult.Test test = getTest();
                String[] descriptions = new String[test.ntests()];
                for (int i = 0; i < descriptions.length; i++) {
                    descriptions[i] = test.getTestDescription(i);
                }
                return descriptions;
            }

            private AllTestsRunResult.Test getTest() {
                return tests[index];
            }

            public String getTestName() {
                return StringTool.encodeURL(getTest().getTestName());
            }
        }

    }
}
