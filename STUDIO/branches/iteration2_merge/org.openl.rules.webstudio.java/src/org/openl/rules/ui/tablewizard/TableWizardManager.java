package org.openl.rules.ui.tablewizard;

import javax.faces.component.html.HtmlInputHidden;

import org.openl.rules.ui.tablewizard.jsf.BaseWizardBean;

/**
 * @author Aliaksandr Antonik.
 */
public class TableWizardManager {
    private BaseWizardBean wizard;
    private HtmlInputHidden hiddenStep;

    static enum TableType {
        UNKNOWN, DECISION, TEST
    }
    private TableType tableType = TableType.DECISION;

    public String startWizard() {
        switch (tableType) {
            case DECISION:
                wizard = new WizardDecisionTable();
                break;
            case TEST:
                wizard = new WizardTestTable();
                break;
            default:
                return null;
        }


        String ret = wizard.start();
        wizard.next();
        return ret;
    }

    public String start() {
        tableType = TableType.DECISION;
        return "wizardSelect";
    }

    public String next() {
        return wizard.next();
    }

    public String prev() {
        return wizard.prev();
    }

    public String getTableType() {
        return tableType.name();
    }

    public BaseWizardBean getWizard() {
        return wizard;
    }

    public void setTableType(String tableType) {
        try {
            this.tableType = TableType.valueOf(tableType);
        } catch (IllegalArgumentException e) {
            this.tableType = TableType.DECISION;
        }
    }

    public HtmlInputHidden getHiddenStep() {
        return hiddenStep;
    }

    public void setHiddenStep(HtmlInputHidden hidden) {
        this.hiddenStep = hidden;
        try{
            int step = Integer.parseInt((String) hidden.getValue());
            wizard.setStep(step);
        } catch (NumberFormatException nfe) {}
    }

    public String cancel() {
        if (wizard != null) {
            wizard.cancel();
        }
        return "newTableCancel";
    }
}
