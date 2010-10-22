package org.openl.rules.ui.tablewizard;

import org.apache.commons.lang.StringUtils;

/**
 * @author Aliaksandr Antonik.
 */
public class TableWizardManager extends TableWizard{

    static enum TableType {
        UNKNOWN,
        DECISION,
        DATATYPE,
        DATATYPE_ALIAS,
        TEST,
        TEST_DIRECT,
        PROPERTY
    }

    private TableType tableType = TableType.DECISION;

    @Override
    public String cancel() {
        if (wizard != null) {
            wizard.cancel();
        }
        return "newTableCancel";
    }
    
    public String getTableType() {
        return tableType.name();
    }

    public void setTableType(String tableType) {
        try {
            this.tableType = TableType.valueOf(tableType);
        } catch (IllegalArgumentException e) {
            this.tableType = TableType.DECISION;
        }
    }
    
    @Override
    public String start() {
        tableType = TableType.DECISION;
        return "wizardSelect";
    }
    
    @Override
    public String startWizard() {
        reload();
        switch (tableType) {
            case DECISION:
                wizard = new DecisionTableCreationWizard();
                wizard.setStepsCount(6);
                break;
            case DATATYPE:
                wizard = new DatatypeTableCreationWizard();
                wizard.setStepsCount(4);
                break;
            case DATATYPE_ALIAS:
                wizard = new DatatypeAliasTableCreationWizard();
                wizard.setStepsCount(4);
                break;
            case TEST:
                wizard = new TestTableCreationWizard();
                wizard.setStepsCount(4);
                break;
            case TEST_DIRECT:
                wizard = new TestTableCreationWizardDirect(getElementUri());
                wizard.setStepsCount(3);
                break;
            case PROPERTY:
                wizard = new PropertyTableCreationWizard();
                wizard.setStepsCount(3);
                break;
            default:
                return null;
        }

        String ret = wizard.start();
        if (ERROR.equals(ret)) {
            // process the error situation on start.
            return StringUtils.EMPTY;
        } else {
            wizard.next();
            return ret;
        }
    }
}
