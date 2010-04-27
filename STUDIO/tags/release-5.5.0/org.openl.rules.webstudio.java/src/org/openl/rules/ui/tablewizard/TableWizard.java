package org.openl.rules.ui.tablewizard;

import org.openl.rules.ui.tablewizard.jsf.BaseWizardBean;

public abstract class TableWizard {

    protected BaseWizardBean wizard;

    public abstract String startWizard();

    public abstract String start();

    public abstract String cancel();

    public BaseWizardBean getWizard() {
        return wizard;
    }

    public String next() {
        return wizard.next();
    }

    public String prev() {
        return wizard.prev();
    }

}
