package org.openl.rules.workspace;

import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceProject;
import org.openl.rules.workspace.uw.UserWorkspaceProjectFolder;

public class TestMUWM {
    public static void main(String[] args) throws WorkspaceException, ProjectException {
        MultiUserWorkspaceManager muwm = new MultiUserWorkspaceManager();

        WorkspaceUser wu = new WorkspaceUserImpl("127.0.0.1");
        UserWorkspace uw = muwm.getUserWorkspace(wu);
        uw.activate();

        System.out.println(uw.getProjects().size());

        String name = "p1";
//        uw.createProject(name);
        UserWorkspaceProject uwp = uw.getProject(name);
        uwp.checkOut();
        
        UserWorkspaceProjectFolder uwpf = uwp.addFolder("F1");
        uwpf.addFolder("F1-1");
        
        uwp.checkIn();
    }
}
