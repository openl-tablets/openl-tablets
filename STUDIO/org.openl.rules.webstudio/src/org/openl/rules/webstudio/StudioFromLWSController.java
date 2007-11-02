package org.openl.rules.webstudio;

import org.openl.rules.WorkspaceException;
import org.openl.rules.ui.WebStudio;
import org.openl.util.Log;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

public class StudioFromLWSController {
    public String openStudio() {
        HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(true);
        RulesUserSession rulesUserSession = (RulesUserSession) session.getAttribute(Const.RULES_USER_SESSION_ATTR);

        if (rulesUserSession != null) {
            try {
                String path = rulesUserSession.getUserWorkspace().getLocalWorkspaceLocation().getAbsolutePath();
                session.setAttribute("studio", new WebStudio(path));
            } catch (WorkspaceException e) {
                Log.error("Failed to get user workspace", e);
            }
        }

        return Const.OUTCOME_SUCCESS;
    }
}
