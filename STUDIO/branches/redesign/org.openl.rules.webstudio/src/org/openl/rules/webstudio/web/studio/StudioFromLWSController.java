package org.openl.rules.webstudio.web.studio;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.servlet.http.HttpSession;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.workspace.lw.LocalWorkspaceManager;

@ManagedBean(name="studioOpener")
@RequestScoped
public class StudioFromLWSController {

    @ManagedProperty(value="#{localWorkspaceManager}")
    private LocalWorkspaceManager localWorkspaceManager;

    public String openRulesEditor() {
        HttpSession session = FacesUtils.getSession();
        session.setAttribute("studio", new WebStudio(FacesUtils.getSession()));
        return "rulesEditor";
    }

    public void setLocalWorkspaceManager(LocalWorkspaceManager localWorkspaceManager) {
        this.localWorkspaceManager = localWorkspaceManager;
    }
}
