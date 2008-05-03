package org.openl.rules.webstudio.web.studio;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import org.openl.rules.ui.WebStudio;
import org.openl.rules.workspace.lw.LocalWorkspaceManager;

public class StudioFromLWSController {
    private LocalWorkspaceManager localWorkspaceManager;

    public void setLocalWorkspaceManager(LocalWorkspaceManager localWorkspaceManager) {
        this.localWorkspaceManager = localWorkspaceManager;
    }

    public String openStudio() {
        HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(true);
        session.setAttribute("studio", new WebStudio());
        return "webstudio";
    }
}
