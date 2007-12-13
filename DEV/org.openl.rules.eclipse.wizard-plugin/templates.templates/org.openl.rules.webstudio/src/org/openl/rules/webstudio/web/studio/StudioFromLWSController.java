package org.openl.rules.webstudio.web.studio;

import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.jsf.util.Util;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceProject;
import org.openl.util.Log;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.Set;

public class StudioFromLWSController {
    public String openStudio() {
        HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(true);
        RulesUserSession rulesUserSession = Util.getRulesUserSession(session);

        if (rulesUserSession != null && !Util.isLocalRequest((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest())) {
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
