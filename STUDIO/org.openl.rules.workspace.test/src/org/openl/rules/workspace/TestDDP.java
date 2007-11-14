package org.openl.rules.workspace;

import org.openl.rules.workspace.abstracts.DeploymentDescriptorProject;
import org.openl.rules.workspace.abstracts.ProjectDescriptor;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.dtr.impl.RepositoryProjectVersionImpl;
import org.openl.rules.workspace.uw.UserWorkspace;

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

            DeploymentDescriptorProject ddp = uw.getDDProject(name);
            ProjectDescriptor pd1 = ddp.createProjectDescriptor("prj1");
            pd1.setProjectVersion(new RepositoryProjectVersionImpl(1, 10, 100, null));
            
            ProjectDescriptor pd2 = ddp.createProjectDescriptor("prj2");
            pd2.setProjectVersion(new RepositoryProjectVersionImpl(2, 20, 200, null));

            ddp.update();
        } catch (Exception e) {
            System.out.println("Cannot create new DDP " + name);
        }        

        DeploymentDescriptorProject ddp = uw.getDDProject(name);
        System.out.println("  " + ddp.getName());
        
        for (ProjectDescriptor pd : ddp.getProjectDescriptors()) {
            System.out.println("    " + pd.getProjectName() + " " + pd.getProjectVersion().getVersionName());
        }
        
        
        uw.passivate();
        
        System.out.println("Done.");
    }
}
