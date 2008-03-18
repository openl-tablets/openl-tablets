package demo;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;

import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectDescriptor;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.deploy.ProductionDeployer;
import org.openl.rules.workspace.deploy.ProductionDeployerManager;
import org.openl.rules.workspace.deploy.impl.ProductionDeployerManagerImpl;
import org.openl.rules.workspace.deploy.impl.jcr.JcrProductionDeployer;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.openl.rules.workspace.dtr.impl.DesignTimeRepositoryImpl;
import org.openl.rules.workspace.lw.impl.LocalWorkspaceManagerImpl;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceDeploymentProject;
import org.openl.rules.workspace.uw.UserWorkspaceProject;

/**
 * Update and Deploy.
 */
public class UpdateAndDeploy {
    private String rulesProjectName;

    private UserWorkspace userWorkspace;

    public void setRulesProjectName(String rulesProjectName) {
        this.rulesProjectName = rulesProjectName;
    }

    public void setUserWorkspace(UserWorkspace userWorkspace) {
        this.userWorkspace = userWorkspace;
    }

    public void updateAndRedeploy() {
        try {
            System.out.println("> Getting Rules Project '" + rulesProjectName + "'...");
            UserWorkspaceProject rulesProject = userWorkspace.getProject(rulesProjectName);
            System.out.println("> Checking Out...");
            rulesProject.checkOut();

            updateRulesProject();

            System.out.println("> Checking In...");
            rulesProject.checkIn();

            if (!userWorkspace.hasDDProject(rulesProjectName)) {
                // no Deployment Project with such name -- create one
                System.out.println("> Creating Deployment Project...");
                userWorkspace.createDDProject(rulesProjectName);
            }

            System.out.println("> Getting Deployment Project '" + rulesProjectName + "'...");
            UserWorkspaceDeploymentProject deploymentProject = userWorkspace.getDDProject(rulesProjectName);
            System.out.println("> Updating Deployment Project...");
            deploymentProject.checkOut();
            // rewrite project->version
            deploymentProject.addProjectDescriptor(rulesProject.getName(), rulesProject.getVersion());
            deploymentProject.checkIn();

            System.out.println("> Deploying...");
            DeployID id = userWorkspace.deploy(deploymentProject);
            System.out.println("> Project '" + deploymentProject.getName() + "' successfully deployed with id:"
                    + id.getName());
        } catch (ProjectException e) {
            e.printStackTrace();
        }
    }

    private void updateRulesProject() {
        // variant #1
        // UserWorkspaceProjectResource resource =
        // (UserWorkspaceProjectResource) rulesProject.getArtefactByPath(new
        // ArtefactPathImpl("/rules/main/Tutorial_4.xls"));
        // resource.setContent(inputStream);

        // variant #2
        // File localWorkspaceLocation = workspace.getLocalWorkspaceLocation();
        // File localProjectLocation = new File(localWorkspaceLocation,
        // rulesProjectName);
        // File xls = new File(localProjectLocation,
        // "rules/main/Tutorial_4.xls");
    }

    public static void main(String[] args) {
        UpdateAndDeploy o = new UpdateAndDeploy();

        WorkspaceUser user = new WorkspaceUserImpl("demo-tutorial7");

        MultiUserWorkspaceManager workspaceManager = new MultiUserWorkspaceManager();

        try {
            LocalWorkspaceManagerImpl localWorkspaceManager = new LocalWorkspaceManagerImpl();
            FileFilter filter = new FileFilter() {
                public boolean accept(File pathname) {
                    return !".studioProps".equalsIgnoreCase(pathname.getName());
                }
            };

            localWorkspaceManager.setLocalWorkspaceFolderFilter(filter);
            localWorkspaceManager.setLocalWorkspaceFileFilter(filter);
            workspaceManager.setLocalWorkspaceManager(localWorkspaceManager);
        } catch (WorkspaceException e) {
            System.err.println("* Failed to initialize Local Workspace Manager!");
            e.printStackTrace();
            System.exit(1);
        }

        ProductionDeployerManager deployerManager = new ProductionDeployerManagerImpl();
        workspaceManager.setProductionDeployerManager(deployerManager);

        try {
            DesignTimeRepository designTimeRepository = new DesignTimeRepositoryImpl();
            workspaceManager.setDesignTimeRepository(designTimeRepository);
        } catch (RepositoryException e) {
            System.err.println("* Failed to initialize Design Time Repository!");
            e.printStackTrace();
            System.exit(1);
        }

        try {
            UserWorkspace userWorkspace = workspaceManager.getUserWorkspace(user);
            userWorkspace.release();
            o.setUserWorkspace(userWorkspace);
        } catch (WorkspaceException e) {
            System.err.println("* Failed to get User Workspace!");
            e.printStackTrace();
            System.exit(1);
        }

        o.setRulesProjectName("org.openl.tablets.tutorial1");

        o.updateAndRedeploy();
    }
}
