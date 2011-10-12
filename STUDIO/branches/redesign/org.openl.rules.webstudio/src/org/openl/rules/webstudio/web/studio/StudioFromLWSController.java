package org.openl.rules.webstudio.web.studio;

import javax.servlet.http.HttpSession;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.workspace.lw.LocalWorkspaceManager;

public class StudioFromLWSController {

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
