package org.openl.rules.ui.tablewizard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.openl.meta.IMetaInfo;
import org.openl.rules.domaintree.DomainTree;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.ui.tablewizard.jsf.BaseWizardBean;
import org.openl.rules.web.jsf.util.FacesUtils;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.table.xls.builder.DecisionTableBuilder;
import org.openl.rules.table.xls.builder.CreateTableException;
import org.openl.rules.table.xls.XlsSheetGridModel;

/**
 * @author Aliaksandr Antonik.
 */
public class WizardBean extends BaseWizardBean {
    private static final Log log = LogFactory.getLog(WizardBean.class);

    private static final String ORIENTATATION_HORIZONTAL="hor";
    private static final String ORIENTATATION_VERTICAL="ver";

    private String tableName;
    private String returnType;
    private boolean vertical;

    private List<TypeNamePair> parameters = new ArrayList<TypeNamePair>();
    private ListWithSelection<TableCondition> conditions = new ListWithSelection<TableCondition>();
    private ListWithSelection<TableArtifact> actions = new ListWithSelection<TableArtifact>();
    private TableArtifact returnValue = new TableArtifact();
    private DomainTree domainTree;

    private SelectItem[] domainTypes;

    private String workbook;
    private int worksheetIndex;
    private Map<String, XlsWorkbookSourceCodeModule> workbooks;

    public WizardBean() {}

    @Override
    protected String getName() {
        return "newTable";
    }

    protected void onFinish(boolean cancelled) {
        reset();
    }

    protected void onStart() {
        reset();

        domainTree = DomainTree.buildTree(getMetaInfo());
        domainTypes = FacesUtils.createSelectItems(domainTree.getAllClasses(true));
    }

    private IMetaInfo getMetaInfo() {
        return WebStudioUtils.getWebStudio().getModel().getWrapper().getOpenClass().getMetaInfo();
    }

    private void reset() {
        tableName = returnType = null;
        parameters = new ArrayList<TypeNamePair>();
        conditions = new ListWithSelection<TableCondition>();
        actions = new ListWithSelection<TableArtifact>();
        returnValue = new TableArtifact();
        returnValue.setName("RET1");
        vertical = false;
        domainTree = null;
        domainTypes = null;
        worksheetIndex = 0;
        workbooks = null;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<TypeNamePair> getParameters() {
        return parameters;
    }

    public int getParameterCount() {
        return parameters.size();
    }

    public void setParameters(List<TypeNamePair> parameters) {
        this.parameters = parameters;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public boolean isVertical() {
        return vertical;
    }

    public void setVertical(boolean vertical) {
        this.vertical = vertical;
    }

    public String getOrientation() {
        return vertical ? ORIENTATATION_VERTICAL : ORIENTATATION_HORIZONTAL;
    }

    public void setOrientation(String or) {
        vertical = or != null && or.equals(ORIENTATATION_VERTICAL);
    }

    public void addParameter() {
        parameters.add(new TypeNamePair());
    }

    public void removeParameter() {
        removeByIndex(parameters);
    }

    public void addCondition() {
        if (validateAtStep2()) {
            conditions.add(new TableCondition());
            WizardUtils.autoRename(conditions, "C");
            conditions.selectLast();
        }
    }

    public void addAction() {
        if (validateAtStep3()) {
            actions.add(new TableArtifact());
            WizardUtils.autoRename(actions, "A");
            actions.selectLast();
        }
    }

    public List<TableArtifact> getActions() {
        return actions;
    }

    public int getSelectedAction() {
        return actions.getSelectedIndex();
    }

    public void removeCondition() {
        if (removeByIndex(conditions))
            WizardUtils.autoRename(conditions, "C");
    }

    public void removeAction() {
        if (removeByIndex(actions))
            WizardUtils.autoRename(actions, "A");
    }

    public void useEditorLogicCondition() {
        TableCondition currentCondition = getCurrentCondition();
        if(currentCondition != null)
            currentCondition.setLogicEditor(true);
    }

    public void useManualLogicCondition() {
        TableCondition currentCondition = getCurrentCondition();
        if(currentCondition != null)
            currentCondition.setLogicEditor(false);
    }

    public void addConditionParameter() {
        TableArtifact cond = getCurrentCondition();
        if (cond != null) {
            cond.getParameters().add(new Parameter());
        }
    }

    public void addConditionClause() {
        TableCondition cond = getCurrentCondition();
        if (cond != null) {
            ConditionClause newClause = new ConditionClause(cond);
            cond.getLogicClauses().add(newClause);
            if (cond.getParamsCount() > 0)
                newClause.initParamName(cond.getParameters().get(0).getName());
        }
    }

    public void removeConditionClause() {
        TableCondition cond = getCurrentCondition();
        if (cond != null && cond.getLogicClauseCount() > 1)
            removeByIndex(cond.getLogicClauses());
    }

    public SelectItem[] getDomainTypes() {
        return domainTypes;
    }

    public void addActionParameter() {
        TableArtifact action = getCurrentAction();
        if (action != null) {
            action.getParameters().add(new Parameter());
        }
    }

    public void addReturnParameter() {
        getReturn().getParameters().add(new Parameter());
    }

    public void removeConditionParameter() {
        TableArtifact cond = getCurrentCondition();
        if (cond != null && cond.getParameters().size() > 1)
            removeByIndex(cond.getParameters());
    }

    public void removeActionParameter() {
        TableArtifact action = getCurrentAction();
        if (action != null && action.getParameters().size() > 1) {
            removeByIndex(action.getParameters());
        }
    }

    public void removeReturnParameter() {
        TableArtifact ret = getReturn();
        if (ret.getParameters().size() > 1) {
            removeByIndex(ret.getParameters());
        }
    }

    private boolean removeByIndex(List list) {
        try {
            int index = Integer.parseInt(FacesUtils.getRequestParameter("index"));
            if (0 <= index && index < list.size()) {
                list.remove(index);
                return true;
            }
        } catch (NumberFormatException nfe) {}

        return false;
    }

    public int getConditionCount() {
        return conditions.size();
    }

    public List<TableCondition> getConditions() {
        return conditions;
    }

    public void selectCondition() {
        try {
            int index = Integer.parseInt(FacesUtils.getRequestParameter("index"));
            if (conditions.get(index) != null) {
                if (validateAtStep2()) 
                    conditions.setSelectedIndex(index);
            }
        } catch (Exception e) {
            log.warn("error while selecting condition", e);
        }
    }

    public TableArtifact getReturn() {
        return returnValue;
    }

    public void selectAction() {
        try {
            int index = Integer.parseInt(FacesUtils.getRequestParameter("index"));
            if (actions.get(index) != null) {
                if (validateAtStep3())
                    actions.setSelectedIndex(index);
            }
        } catch (Exception e) {
            log.warn("error while selecting action", e);
        }
    }

    @Override
    public String next() {
        int old, sz;
        boolean valid;
        
        switch (getStep()) {
            case 1:
                if (!validateAfterStep1()) {
                    return null;
                }
                break;
            case 2:
                old = conditions.getSelectedIndex();
                sz = conditions.size();
                valid = true;
                try {
                    for (int i = 0; i < sz; ++i) {
                        conditions.setSelectedIndex(i);
                        valid = false;
                        if (!validateAtStep2())
                            return null;
                    }
                } finally {
                    if (valid)
                        conditions.setSelectedIndex(old);
                }
                break;

            case 3:
                old = actions.getSelectedIndex();
                sz = actions.size();
                valid = true;
                try {
                    for (int i = 0; i < sz; ++i) {
                        actions.setSelectedIndex(i);
                        valid = false;
                        if (!validateAtStep3())
                            return null;
                    }
                } finally {
                    if (valid)
                        actions.setSelectedIndex(old);
                }
                break;

            case 4:
                if (!validateAfterStep4())
                    return null;
                break;
        }

        return super.next();
    }

    @Override
    protected void onStepFirstVisit(int step) {
        switch (step) {
            case 1:
                parameters.add(new TypeNamePair());
                break;
            case 2:
                addCondition();
                break;
            case 3:
                addAction();
                break;
            case 4:
                workbooks = new HashMap<String, XlsWorkbookSourceCodeModule>();

                TableSyntaxNode[] syntaxNodes = ((XlsMetaInfo) getMetaInfo()).getXlsModuleNode().getXlsTableSyntaxNodes();
                for (TableSyntaxNode node : syntaxNodes) {
                    XlsWorkbookSourceCodeModule module = ((XlsSheetSourceCodeModule) node.getModule()).getWorkbookSource();
                    workbooks.put(module.getUri(), module);
                }

                if (workbooks.size() > 0)
                    workbook = workbooks.keySet().iterator().next();        

                break;
        }
    }

    private boolean validateAfterStep1() {
        if (StringUtils.isEmpty(tableName)) {
            reportError("Table name can not be empty");
            return false;
        }

        return validateParameterNames(parameters, "newTableWiz1:paramTable:", ":pname");
    }

    private boolean validateAtStep2() {
        TableArtifact cond = getCurrentCondition();
        return cond == null || validateParameterNames(cond.getParameters(), "newTableWiz2:paramTable:", ":pname");
    }

    private boolean validateAtStep3() {
        TableArtifact action = getCurrentAction();
        return action == null || validateParameterNames(action.getParameters(), "newTableWiz3:paramTable:", ":pname");
    }

    private boolean validateAfterStep4() {
        return validateParameterNames(returnValue.getParameters(), "newTableWiz4:paramTable:", ":pname");
    }

    private boolean validateParameterNames(List<? extends TypeNamePair> parameters, String prefix, String suffix) {
        boolean res = true;
        String message;
        for (int i = 0; i < parameters.size(); i++) {
            if ((message = WizardUtils.checkParameterName(parameters.get(i).getName())) != null) {
                reportError(prefix +i+ suffix, message);
                res = false;
            }
        }
        return res;
    }

    public DomainTree getDomainTree() {
        return domainTree;
    }

    public int getActionCount() {
        return actions.size();
    }

    public TableCondition getCurrentCondition() {
        return conditions.getSelectedElement();
    }

    public TableArtifact getCurrentAction() {
        return actions.getSelectedElement();
    }

    public int getSelectedCondition() {
        return conditions.getSelectedIndex();
    }

    private void reportError(String detail) {
        reportError(null, detail);
    }

    private void reportError(String clientId, String detail) {
        FacesContext.getCurrentInstance().addMessage(clientId, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation Error: ", detail));
    }

    public String getWorkbook() {
        return workbook;
    }

    public void setWorkbook(String workbook) {
        this.workbook = workbook;
    }

    public int getWorksheetIndex() {
        return worksheetIndex;
    }

    public void setWorksheetIndex(int worksheetIndex) {
        this.worksheetIndex = worksheetIndex;
    }

    public List<SelectItem> getWorkbooks() {
        List<SelectItem> items = new ArrayList<SelectItem>(workbooks.size());
        for (String wbURI : workbooks.keySet()) {
            String[] parts = wbURI.split("/");
            items.add(new SelectItem(wbURI, parts[parts.length - 1]));
        }

        return items;
    }

    public List<SelectItem> getWorksheets() {
        if (workbook == null || workbooks == null)
            return Collections.EMPTY_LIST;

        XlsWorkbookSourceCodeModule currentSheet = workbooks.get(workbook);
        if (currentSheet == null)
            return Collections.EMPTY_LIST;

        HSSFWorkbook hssfWorkbook = currentSheet.getWorkbook();
        List<SelectItem> items = new ArrayList<SelectItem>(hssfWorkbook.getNumberOfSheets());
        for (int i = 0; i < hssfWorkbook.getNumberOfSheets(); ++i) {
            items.add(new SelectItem(i, hssfWorkbook.getSheetName(i)));
        }
        return items;
    }

    public String save() {
        try {
            doSave();
            return "done";
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Could not create table: ",  e.getMessage()));
        }
        return null;
    }

    private void doSave() throws CreateTableException, IOException {
        XlsWorkbookSourceCodeModule module = workbooks.get(workbook);
        HSSFSheet sheet = module.getWorkbook().getSheetAt(worksheetIndex);
        XlsSheetSourceCodeModule sourceCodeModule = new XlsSheetSourceCodeModule(sheet, module.getWorkbook().getSheetName(worksheetIndex), module);
        DecisionTableBuilder builder = new DecisionTableBuilder(new XlsSheetGridModel(sourceCodeModule));

        // compute table dimensions
        int tableWidth = returnValue.getParameters().size();
        for (TableArtifact artifact : conditions) tableWidth += artifact.getParameters().size();
        for (TableArtifact artifact : actions) tableWidth += artifact.getParameters().size();
        int tableHeight = DecisionTableBuilder.HEADER_HEIGHT + 3; // let it be three extra empty rows

        // start writing finally
        builder.beginTable(tableWidth, tableHeight);

        // make up header
        StringBuilder sbHeader = new StringBuilder(returnType).append(" ").append(tableName).append("(");
        boolean first = true;
        for (TypeNamePair typeNamePair : parameters) {
            if (first)
                first = false;
            else
                sbHeader.append(", ");
            sbHeader.append(typeNamePair.getType()).append(" ").append(typeNamePair.getName());
        }
        sbHeader.append(")");
        builder.writeHeader(sbHeader.toString());

        // writing conditions, actions and return section
        doSaveWriteArtifacts(builder, conditions);
        doSaveWriteArtifacts(builder, actions);
        doSaveWriteArtifacts(builder, Collections.singletonList(returnValue));

        builder.endTable();
    }

    private void doSaveWriteArtifacts(DecisionTableBuilder builder, List<? extends TableArtifact> tableArtifacts) {
        for (TableArtifact artifact : tableArtifacts) {
            List<Parameter> params = artifact.getParameters();
            String[] names = new String[params.size()];
            String[] signatures = new String[params.size()];
            int index = 0;
            for (Parameter param : params) {
                names[index] = param.getBusinessName();
                signatures[index++] = param.getType() + " " + param.getName();
            }

            builder.writeElement(artifact.getName(), artifact.getLogic(), names, signatures);
        }
    }

}
