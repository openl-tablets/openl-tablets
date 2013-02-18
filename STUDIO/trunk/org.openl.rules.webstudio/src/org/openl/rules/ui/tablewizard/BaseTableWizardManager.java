package org.openl.rules.ui.tablewizard;

import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.BaseWizard;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

public abstract class BaseTableWizardManager {

    private IOpenLTable table;

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
        return table;
    }

    protected void reload() {
        table = null;
        init();        
    }

    protected void init() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        String tableUri = studio.getTableUri();
        table = studio.getModel().getTable(tableUri);
    }

}
