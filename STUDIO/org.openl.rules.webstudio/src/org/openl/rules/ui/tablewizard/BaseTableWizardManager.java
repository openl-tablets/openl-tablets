package org.openl.rules.ui.tablewizard;

import org.apache.commons.lang.StringUtils;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.BaseWizard;
import org.openl.rules.ui.Message;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.Constants;
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
        String id = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_ID);

        WebStudio studio = WebStudioUtils.getWebStudio();
        final ProjectModel model = studio.getModel();

        if (!StringUtils.isBlank(id)) {
            IOpenLTable table = model.getTableById(id);
            if (table != null) {
                tableUri = table.getUri();
            } else {
                throw new Message("Table with id " + id + " does not exists");
            }
        } else {
            tableUri = WebStudioUtils.getWebStudio().getTableUri();
        }
    }

}
