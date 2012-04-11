package org.openl.rules.ui.tablewizard;

import javax.faces.component.html.HtmlInputHidden;

import org.openl.rules.ui.tablewizard.jsf.BaseWizardBean;

/**
 * @author Aliaksandr Antonik.
 */
public class TableWizardManager {
    static enum TableType {
        UNKNOWN,
        DECISION,
        TEST
    }
    private BaseWizardBean wizard;

    private HtmlInputHidden hiddenStep;

    private TableType tableType = TableType.DECISION;

    public String cancel() {
        if (wizard != null) {
            wizard.cancel();
        }
        return "newTableCancel";
    }

    public HtmlInputHidden getHiddenStep() {
        return hiddenStep;
    }

    public String getTableType() {
        return tableType.name();
    }

    public BaseWizardBean getWizard() {
        return wizard;
    }

    public String next() {
        return wizard.next();
    }

    public String prev() {
        return wizard.prev();
    }

    public void setHiddenStep(HtmlInputHidden hidden) {
        hiddenStep = hidden;
        try {
            int step = Integer.parseInt((String) hidden.getValue());
            wizard.setStep(step);
        } catch (NumberFormatException nfe) {
        }
    }

    public void setTableType(String tableType) {
        try {
            this.tableType = TableType.valueOf(tableType);
        } catch (IllegalArgumentException e) {
            this.tableType = TableType.DECISION;
        }
    }

    public String start() {
        tableType = TableType.DECISION;
        return "wizardSelect";
    }

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
}
