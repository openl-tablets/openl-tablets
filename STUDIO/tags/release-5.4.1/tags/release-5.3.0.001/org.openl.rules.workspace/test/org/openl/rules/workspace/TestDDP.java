package org.openl.rules.workspace;

import org.junit.Ignore;
import org.openl.rules.repository.CommonVersionImpl;
import org.openl.rules.workspace.abstracts.DeploymentDescriptorProject;
import org.openl.rules.workspace.abstracts.ProjectDescriptor;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceDeploymentProject;

@Ignore("Manual test")
public class TestDDP {
    public static void main(String[] args) throws WorkspaceException, ProjectException {
        MultiUserWorkspaceManager muwm = new MultiUserWorkspaceManager();

        WorkspaceUser wu = new WorkspaceUserImpl("127.0.0.1");
        UserWorkspace uw = muwm.getUserWorkspace(wu);
        uw.activate();

        System.out.println(uw.getDDProjects().size());

        String name = "ddp1";
        try {
            uw.createDDProject(name);

            UserWorkspaceDeploymentProject ddp = uw.getDDProject(name);
            ddp.checkOut();

            ddp.addProjectDescriptor("prj1", new CommonVersionImpl(1, 10, 100));
            ddp.addProjectDescriptor("prj2", new CommonVersionImpl(2, 20, 200));

            ddp.checkIn();
        } catch (Exception e) {
            System.out.println("Cannot create new DDP " + name);
        }

        System.out.println("Listing DDProject");
        DeploymentDescriptorProject ddp = uw.getDDProject(name);
        System.out.println("  " + ddp.getName());

        for (ProjectDescriptor pd : ddp.getProjectDescriptors()) {
            System.out.println("    " + pd.getProjectName() + " " + pd.getProjectVersion().getVersionName());
        }

        uw.passivate();

        System.out.println("Done.");
    }
}
