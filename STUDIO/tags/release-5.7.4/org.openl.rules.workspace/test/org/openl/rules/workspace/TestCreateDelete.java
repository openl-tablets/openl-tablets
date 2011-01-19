package org.openl.rules.workspace;

import org.junit.Ignore;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.workspace.uw.UserWorkspace;

@Ignore("Manual test")
public class TestCreateDelete {
    public static void main(String[] args) throws WorkspaceException, ProjectException {
        MultiUserWorkspaceManager muwm = new MultiUserWorkspaceManager();

        WorkspaceUser wu = new WorkspaceUserImpl("127.0.0.1");
        UserWorkspace uw = muwm.getUserWorkspace(wu);
        uw.activate();

        System.out.println(uw.getProjects().size());

        String name = "p1";
        if (!uw.hasProject(name)) {
            uw.createProject(name);
        }

        AProject p = uw.getProject(name);
        p.checkOut();

        try {
            AProjectFolder f = (AProjectFolder) p.getArtefact("F1");
            System.out.println("Deleting...");
            f.delete();
        } catch (Exception e) {
            AProjectFolder uwpf;
            uwpf = p.addFolder("F1");
            uwpf.addFolder("F1-1");
        }

        uw.refresh();
        p.checkIn();

        uw.passivate();

        System.out.println("Done.");
    }
}
