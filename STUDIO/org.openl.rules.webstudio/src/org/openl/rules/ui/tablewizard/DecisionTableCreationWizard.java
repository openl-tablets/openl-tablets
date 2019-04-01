package org.openl.rules.ui.tablewizard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;
import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.NotEmpty;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.builder.CreateTableException;
import org.openl.rules.table.xls.builder.DecisionTableBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Aliaksandr Antonik.
 */
public class DecisionTableCreationWizard extends TableCreationWizard {
    private final Logger log = LoggerFactory.getLogger(DecisionTableCreationWizard.class);
    private static final String ORIENTATATION_HORIZONTAL = "hor";
    private static final String ORIENTATATION_VERTICAL = "ver";

    @NotEmpty(message = "Technical name can not be empty")
    @Pattern(regexp = "([a-zA-Z_][a-zA-Z_0-9]*)?", message = "Invalid technical name")
    private String tableName;

    private String returnType;
    private boolean vertical;

    private List<TypeNamePair> parameters = new ArrayList<>();
    private ListWithSelection<TableCondition> conditions = new ListWithSelection<>();
    private ListWithSelection<TableArtifact> actions = new ListWithSelection<>();
    private TableArtifact returnValue = new TableArtifact();
    private DomainTree domainTree;

    private SelectItem[] domainTypes;

    public DecisionTableCreationWizard() {
    }

    public void addAction() {
        if (validateAtStep4()) {
            actions.add(new TableArtifact());
            WizardUtils.autoRename(actions, "A");
            actions.selectLast();
        }
    }

    public void addActionParameter() {
        TableArtifact action = getCurrentAction();
        if (action != null) {
            action.getParameters().add(new Parameter());
        }
    }

    public void addCondition() {
        if (validateAtStep3()) {
            conditions.add(new TableCondition());
            WizardUtils.autoRename(conditions, "C");
            conditions.selectLast();
        }
    }

    public void addConditionClause() {
        TableCondition cond = getCurrentCondition();
        if (cond != null) {
            ConditionClause newClause = new ConditionClause(cond);
            cond.getLogicClauses().add(newClause);
            if (cond.getParamsCount() > 0) {
                newClause.initParamName(cond.getParameters().get(0).getName());
            }
        }
    }

    public void addConditionParameter() {
        TableArtifact cond = getCurrentCondition();
        if (cond != null) {
            cond.getParameters().add(new Parameter());
        }
    }

    public void addParameter() {
        parameters.add(new TypeNamePair());
    }

    public void addReturnParameter() {
        getReturnValue().getParameters().add(new Parameter());
    }

    protected String buildTable(XlsSheetSourceCodeModule sourceCodeModule) throws CreateTableException {
        XlsSheetGridModel gridModel = new XlsSheetGridModel(sourceCodeModule);
        DecisionTableBuilder builder = new DecisionTableBuilder(gridModel);

        // compute table dimensions
        int tableWidth = returnValue.getParameters().size();
        for (TableArtifact artifact : conditions) {
            tableWidth += artifact.getParameters().size();
        }
        for (TableArtifact artifact : actions) {
            tableWidth += artifact.getParameters().size();
        }
        int tableHeight = DecisionTableBuilder.LOGIC_ELEMENT_HEIGHT + 4; // let it be 1-3 extra empty rows

        // start writing finally
        builder.beginTable(tableWidth, tableHeight);

        // make up header
        StringBuilder sbHeader = new StringBuilder(returnType).append(" ").append(tableName).append("(");
        boolean first = true;
        for (TypeNamePair typeNamePair : parameters) {
            if (first) {
                first = false;
            } else {
                sbHeader.append(", ");
            }
            sbHeader.append(typeNamePair.getType()).append(" ").append(typeNamePair.getName());
        }
        sbHeader.append(")");
        builder.writeHeader(sbHeader.toString());

        builder.writeProperties(buildProperties(), null);

        // writing conditions, actions and return section
        doSaveWriteArtifacts(builder, conditions);
        doSaveWriteArtifacts(builder, actions);
        doSaveWriteArtifacts(builder, Collections.singletonList(returnValue));

        String newTableUri = gridModel.getRangeUri(builder.getTableRegion());

        builder.endTable();

        return newTableUri;
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

    public int getActionCount() {
        return actions.size();
    }

    public List<TableArtifact> getActions() {
        return actions;
    }

    public int getConditionCount() {
        return conditions.size();
    }

    public List<TableCondition> getConditions() {
        return conditions;
    }

    public TableArtifact getCurrentAction() {
        return actions.getSelectedElement();
    }

    public TableCondition getCurrentCondition() {
        return conditions.getSelectedElement();
    }

    public DomainTree getDomainTree() {
        return domainTree;
    }

    public SelectItem[] getDomainTypes() {
        return domainTypes;
    }

    @Override
    public String getName() {
        return "newTable";
    }

    public String getOrientation() {
        return vertical ? ORIENTATATION_VERTICAL : ORIENTATATION_HORIZONTAL;
    }

    public int getParameterCount() {
        return parameters.size();
    }

    public List<TypeNamePair> getParameters() {
        return parameters;
    }

    public TableArtifact getReturnValue() {
        return returnValue;
    }

    public String getReturnType() {
        return returnType;
    }

    public int getSelectedAction() {
        return actions.getSelectedIndex();
    }

    public int getSelectedCondition() {
        return conditions.getSelectedIndex();
    }

    public String getTableName() {
        return tableName;
    }

    public boolean isVertical() {
        return vertical;
    }

    @Override
    public String next() {
        int old, sz;
        boolean valid;

        switch (getStep()) {
            case 1:
                break;
            case 2:
                break;
            case 3:
                old = conditions.getSelectedIndex();
                sz = conditions.size();
                valid = true;
                try {
                    for (int i = 0; i < sz; ++i) {
                        conditions.setSelectedIndex(i);
                        valid = false;
                        if (!validateAtStep3()) {
                            return null;
                        }
                    }
                } finally {
                    if (valid) {
                        conditions.setSelectedIndex(old);
                    }
                }
                break;

            case 4:
                old = actions.getSelectedIndex();
                sz = actions.size();
                valid = true;
                try {
                    for (int i = 0; i < sz; ++i) {
                        actions.setSelectedIndex(i);
                        valid = false;
                        if (!validateAtStep4()) {
                            return null;
                        }
                    }
                } finally {
                    if (valid) {
                        actions.setSelectedIndex(old);
                    }
                }
                break;

            case 5:
                if (!validateAfterStep5()) {
                    return null;
                }
                break;
        }

        return super.next();
    }

    @Override
    protected void onCancel() {
        reset();
    }

    @Override
    protected void onStart() {
        reset();

        domainTree = DomainTree.buildTree(WizardUtils.getProjectOpenClass());
        domainTypes = FacesUtils.createSelectItems(domainTree.getAllClasses());
    }

    @Override
    protected void onStepFirstVisit(int step) {
        switch (step) {
            case 1:
                break;
            case 2:
                parameters.add(new TypeNamePair());
                break;
            case 3:
                addCondition();
                break;
            case 4:
                addAction();
                break;
            case 5:
                initWorkbooks();
                break;
        }
    }

    public void removeAction() {
        if (removeByIndex(actions)) {
            WizardUtils.autoRename(actions, "A");
        }
    }

    public void removeActionParameter() {
        TableArtifact action = getCurrentAction();
        if (action != null && action.getParameters().size() > 1) {
            removeByIndex(action.getParameters());
        }
    }

    private boolean removeByIndex(List<?> list) {
        try {
            int index = Integer.parseInt(FacesUtils.getRequestParameter("index"));
            if (0 <= index && index < list.size()) {
                list.remove(index);
                return true;
            }
        } catch (NumberFormatException nfe) {
        }

        return false;
    }

    public void removeCondition() {
        if (removeByIndex(conditions)) {
            WizardUtils.autoRename(conditions, "C");
        }
    }

    public void removeConditionClause() {
        TableCondition cond = getCurrentCondition();
        if (cond != null && cond.getLogicClauseCount() > 1) {
            removeByIndex(cond.getLogicClauses());
        }
    }

    public void removeConditionParameter() {
        TableArtifact cond = getCurrentCondition();
        if (cond != null && cond.getParameters().size() > 1) {
            removeByIndex(cond.getParameters());
        }
    }

    public void removeParameter() {
        removeByIndex(parameters);
    }

    public void removeReturnParameter() {
        TableArtifact ret = getReturnValue();
        if (ret.getParameters().size() > 1) {
            removeByIndex(ret.getParameters());
        }
    }

    private void reportError(String clientId, String detail) {
        FacesUtils.addMessage(clientId, "Validation Error: ", detail, FacesMessage.SEVERITY_ERROR);
    }

    @Override
    protected void reset() {
        tableName = returnType = null;
        parameters = new ArrayList<>();
        conditions = new ListWithSelection<>();
        actions = new ListWithSelection<>();
        returnValue = new TableArtifact();
        returnValue.setName("RET1");
        vertical = false;
        domainTree = null;
        domainTypes = null;

        super.reset();
    }

    @Override
    protected void onFinish() throws Exception {
        XlsSheetSourceCodeModule sheetSourceModule = getDestinationSheet();
        String newTableUri = buildTable(sheetSourceModule);
        setNewTableId(newTableUri);
        getModifiedWorkbooks().add(sheetSourceModule.getWorkbookSource());
        super.onFinish();
    }

    public void selectAction() {
        try {
            int index = Integer.parseInt(FacesUtils.getRequestParameter("index"));
            if (actions.get(index) != null) {
                if (validateAtStep4()) {
                    actions.setSelectedIndex(index);
                }
            }
        } catch (Exception e) {
            log.warn("Error while selecting action", e);
        }
    }

    public void selectCondition() {
        try {
            int index = Integer.parseInt(FacesUtils.getRequestParameter("index"));
            if (conditions.get(index) != null && validateAtStep3()) {
                conditions.setSelectedIndex(index);
            }
        } catch (Exception e) {
            log.warn("Error while selecting condition", e);
        }
    }

    public void setOrientation(String or) {
        vertical = or != null && or.equals(ORIENTATATION_VERTICAL);
    }

    public void setParameters(List<TypeNamePair> parameters) {
        this.parameters = parameters;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public void setVertical(boolean vertical) {
        this.vertical = vertical;
    }

    public void useEditorLogicCondition() {
        TableCondition currentCondition = getCurrentCondition();
        if (currentCondition != null) {
            currentCondition.setLogicEditor(true);
        }
    }

    public void useManualLogicCondition() {
        TableCondition currentCondition = getCurrentCondition();
        if (currentCondition != null) {
            currentCondition.setLogicEditor(false);
        }
    }

    private boolean validateAfterStep5() {
        return validateParameterNames(returnValue.getParameters(), "newTableWiz5:paramTable:", ":pname");
    }

    private boolean validateAtStep3() {
        TableArtifact cond = getCurrentCondition();
        return cond == null || validateParameterNames(cond.getParameters(), "newTableWiz3:paramTable:", ":pname");
    }

    private boolean validateAtStep4() {
        TableArtifact action = getCurrentAction();
        return action == null || validateParameterNames(action.getParameters(), "newTableWiz4:paramTable:", ":pname");
    }

    private boolean validateParameterNames(List<? extends TypeNamePair> parameters, String prefix, String suffix) {
        boolean res = true;
        String message;
        for (int i = 0; i < parameters.size(); i++) {
            if ((message = WizardUtils.checkParameterName(parameters.get(i).getName())) != null) {
                reportError(prefix + i + suffix, message);
                res = false;
            }
        }
        return res;
    }

}
