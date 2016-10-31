package org.openl.rules.workspace;

import org.junit.Ignore;
import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.workspace.uw.UserWorkspace;

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
            ADeploymentProject ddp = uw.createDDProject(name);
            ddp.edit(wu);

            ddp.addProjectDescriptor("prj1", new CommonVersionImpl(100));
            ddp.addProjectDescriptor("prj2", new CommonVersionImpl(200));

            ddp.save(wu);
        } catch (Exception e) {
            System.out.println("Cannot create new DDP " + name);
        }

        System.out.println("Listing DDProject");
        ADeploymentProject ddp = uw.getDDProject(name);
        System.out.println("  " + ddp.getName());

        for (ProjectDescriptor pd : ddp.getProjectDescriptors()) {
            System.out.println("    " + pd.getProjectName() + " " + pd.getProjectVersion().getVersionName());
        }

        uw.passivate();

        System.out.println("Done.");
    }
}
