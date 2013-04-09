package org.openl.rules.ui.tablewizard;

import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.BaseWizard;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

public abstract class BaseTableWizardManager {

    private String tableUri;

    protected BaseWizard wizard;

    public abstract String startWizard();

    public abstract String start();

    public abstract String cancel();

    public BaseWizard getWizard() {
        return wizard;
    }

    public String next() {
        return wizard.next();
    }

    public String prev() {
        return wizard.prev();
    }

    public IOpenLTable getTable() {
        return WebStudioUtils.getWebStudio().getModel().getTable(tableUri);
    }

    protected void reload() {
        tableUri = null;
        init();        
    }

    protected void init() {
        tableUri = WebStudioUtils.getWebStudio().getTableUri();
    }

}
