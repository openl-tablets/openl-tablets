package org.openl.rules.webstudio.web.studio;

import java.util.HashSet;
import java.util.Set;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.lw.LocalWorkspaceManager;
import org.openl.rules.workspace.lw.impl.LocalWorkspaceManagerImpl;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceProject;
import org.openl.util.Log;

public class StudioFromLWSController {
    private LocalWorkspaceManager localWorkspaceManager;

    public void setLocalWorkspaceManager(LocalWorkspaceManager localWorkspaceManager) {
        this.localWorkspaceManager = localWorkspaceManager;
    }

    public String openStudio() {
        HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(true);
        RulesUserSession rulesUserSession = WebStudioUtils.getRulesUserSession(session);
        LocalWorkspaceManagerImpl lwsm = (LocalWorkspaceManagerImpl) localWorkspaceManager;

        if (rulesUserSession != null) {
            try {
                UserWorkspace userWorkspace = rulesUserSession.getUserWorkspace();
                String path = lwsm.getWorkpacePath(rulesUserSession.getUserName()).getAbsolutePath();

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
