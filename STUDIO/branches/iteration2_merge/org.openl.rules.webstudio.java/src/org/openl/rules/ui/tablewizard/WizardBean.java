package org.openl.rules.ui.tablewizard;

import java.util.ArrayList;
import java.util.List;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.ui.tablewizard.jsf.BaseWizardBean;
import org.openl.rules.web.jsf.util.FacesUtils;

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

    public WizardBean() {
        start();
    }

    @Override
    protected String getName() {
        return "newTable";
    }

    protected void onFinish(boolean cancelled) {
        reset();
    }

    protected void onStart() {
        reset();
    }

    private void reset() {
        tableName = returnType = null;
        parameters = new ArrayList<TypeNamePair>();
        conditions = new ListWithSelection<TableCondition>();
        actions = new ListWithSelection<TableArtifact>();
        returnValue = new TableArtifact();
        vertical = false;
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
}
