package org.openl.rules.webstudio;

import org.openl.rules.ui.WebStudio;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceProject;
import org.openl.util.Log;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;
import java.util.HashSet;
import java.util.Set;

public class StudioFromLWSController {
    public String openStudio() {
        HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(true);
        RulesUserSession rulesUserSession = (RulesUserSession) session.getAttribute(Const.RULES_USER_SESSION_ATTR);

        if (rulesUserSession != null) {
            try {
                UserWorkspace userWorkspace = rulesUserSession.getUserWorkspace();
                String path = userWorkspace.getLocalWorkspaceLocation().getAbsolutePath();

                WebStudio webStudio = new WebStudio(path);
                Set<String> writableProjects = new HashSet<String>();
                for (Project project : userWorkspace.getProjects()) {
                    UserWorkspaceProject workspaceProject = (UserWorkspaceProject) project;
                    if (workspaceProject.isCheckedOut() || workspaceProject.isLocalOnly()) {
                        writableProjects.add(workspaceProject.getName());
                    }
                }
                webStudio.setWritableProjects(writableProjects);

                session.setAttribute("studio", webStudio);
            } catch (WorkspaceException e) {
                Log.error("Failed to get user workspace", e);
            } catch (ProjectException e) {
                Log.error("Failed to get user workspace", e);
            }
        }

        return "webstudio";
    }
}
