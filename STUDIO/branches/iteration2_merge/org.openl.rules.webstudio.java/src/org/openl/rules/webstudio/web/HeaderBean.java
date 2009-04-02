package org.openl.rules.webstudio.web;

import java.io.IOException;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.servlet.ServletRequest;

import org.openl.rules.ui.OpenLWrapperInfo;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.util.net.NetUtils;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import static org.openl.rules.webstudio.web.util.WebStudioUtils.getWebStudio;

/**
 * Request scope managed bean providing logic for header page of OpenL Studio. 
 */
public class HeaderBean {

    private boolean hideLogout;

    public SelectItem[] getProjects() throws IOException {
        OpenLWrapperInfo[] wrappers = getWebStudio().getWrappers();
        if (wrappers.length == 0) {
            return new SelectItem[] {new SelectItem("-- No OpenL Projects in the Workspace --")};
        }

        SelectItem[] selectItems = new SelectItem[wrappers.length];
        for (int i = 0; i < wrappers.length; i++) {
            selectItems[i] = new SelectItem(wrappers[i].getWrapperClassName(), wrappers[i].getDisplayName());
        }
        return selectItems;
    }

    public boolean isProjectsExist() throws IOException {
        OpenLWrapperInfo[] wrappers = getWebStudio().getWrappers();
        return wrappers.length > 0;
    }

    public boolean isProjectReadOnly() {
        WebStudio webStudio = WebStudioUtils.getWebStudio();
        return webStudio == null || webStudio.getModel().isReadOnly();
    }

    public String getSelectedProject() {
        OpenLWrapperInfo current = getWebStudio().getCurrentWrapper();
        if (current != null)
            return current.getWrapperClassName();
        return "";
    }

    public void setSelectedProject(String selectedProject) throws Exception {
        // Actual select happens now in a different place.

        //getWebStudio().select(selectedProject);
    }

    public boolean isLocalRequest() {
        return NetUtils.isLocalRequest((ServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest());
    }

    public boolean isRepositoryFailed() {
        return WebStudioUtils.isRepositoryFailed();
    }

    public boolean isHideLogout() {
        return hideLogout;
    }

    public void setHideLogout(boolean hideLogout) {
        this.hideLogout = hideLogout;
    }

}
