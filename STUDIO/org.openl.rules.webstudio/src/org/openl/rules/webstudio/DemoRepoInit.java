package org.openl.rules.webstudio;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.repository.DeploymentManager;
import org.openl.rules.webstudio.web.repository.project.ExcelFilesProjectCreator;
import org.openl.rules.webstudio.web.repository.project.PredefinedTemplatesResolver;
import org.openl.rules.webstudio.web.repository.project.ProjectFile;
import org.openl.rules.webstudio.web.repository.project.TemplatesResolver;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.spring.env.DynamicPropertySource;

/**
 * Creates demo projects in a repository.
 *
 * @author Yury Molchan
 */
public class DemoRepoInit {
    private static final Logger LOG = LoggerFactory.getLogger(DemoRepoInit.class);

    private final TemplatesResolver templatesResolver = new PredefinedTemplatesResolver();

    private final PathFilter zipFilter;
    private final MultiUserWorkspaceManager workspaceManager;
    private final DeploymentManager deploymentManager;
    private final SingleUserModeInit singleUserModeInit;

    @Autowired
    public DemoRepoInit(@Qualifier("zipFilter") PathFilter zipFilter,
                        MultiUserWorkspaceManager workspaceManager,
                        DeploymentManager deploymentManager,
                        SingleUserModeInit singleUserModeInit) {
        this.zipFilter = zipFilter;
        this.workspaceManager = workspaceManager;
        this.deploymentManager = deploymentManager;
        this.singleUserModeInit = singleUserModeInit;
    }

    @Autowired
    private AclProjectsHelper aclProjectsHelper;

    public void init() {
        if (!Props.bool("demo.init")) {
            return;
        }

        try {
            var config = new HashMap<String, String>();
            config.put("demo.init", null);
            DynamicPropertySource.get().save(config);
        } catch (IOException ex) {
            LOG.error("Could not clean demo.init property", ex);
        }

        WorkspaceUserImpl user = new WorkspaceUserImpl(singleUserModeInit.getUsername(),
                (x) -> new UserInfo(singleUserModeInit.getUsername(),
                        singleUserModeInit.getEmail(),
                        singleUserModeInit.getDisplayName()));
        UserWorkspace userWorkspace = workspaceManager.getUserWorkspace(user);

        createProject(userWorkspace, "examples", "Example 1 - Bank Rating", true, false);
        createProject(userWorkspace, "examples", "Example 2 - Corporate Rating", true, false);
        createProject(userWorkspace, "examples", "Example 3 - Auto Policy Calculation", false, true);
        createProject(userWorkspace, "tutorials", "Tutorial 1 - Introduction to Decision Tables", true, false);
        createProject(userWorkspace, "tutorials", "Tutorial 2 - Introduction to Data Tables", true, false);
        createProject(userWorkspace, "tutorials", "Tutorial 3 - More Advanced Decision and Data Tables", true, false);
        createProject(userWorkspace, "tutorials", "Tutorial 4 - Introduction to Column Match Tables", false, false);
        createProject(userWorkspace, "tutorials", "Tutorial 5 - Introduction to TBasic Tables", false, false);
        createProject(userWorkspace, "tutorials", "Tutorial 6 - Introduction to Spreadsheet Tables", false, false);
        createProject(userWorkspace, "tutorials", "Tutorial 7 - Introduction to Table Properties", false, false);
        createProject(userWorkspace, "tutorials", "Tutorial 8 - Introduction to Smart Rules and Smart Lookup Tables", true, false);
    }

    private void createProject(UserWorkspace userWorkspace, String part, String projectName, boolean open, boolean deploy) {
        ProjectFile[] templateFiles = templatesResolver.getProjectFiles(part, projectName);
        String repositoryId = "design";

        ExcelFilesProjectCreator projectCreator = new ExcelFilesProjectCreator(repositoryId,
                projectName,
                "",
                userWorkspace,
                "Project " + projectName + " is created.",
                zipFilter,
                Collections.emptyMap(),
                templateFiles);
        try {
            try {
                projectCreator.createRulesProject();
            } catch (ProjectException e) {
                LOG.error("Project: {}. Message: {}", projectName, e.getMessage(), e);
                return;
            }
            String technicalName = projectCreator.getCreatedProjectName();
            RulesProject createdProject = userWorkspace.getProject(repositoryId, technicalName);

            if (open) {
                createdProject.open();
            }

            if (deploy) {
                if (!aclProjectsHelper.hasCreateDeployConfigProjectPermission()) {
                    LOG.error("There is no permission to create deploy configuration project: {}", projectName);
                    return;
                }
                var deployConfiguration = userWorkspace.createDDProject(projectName);
                deployConfiguration.open();
                var branch = createdProject.getBranch();
                deployConfiguration.addProjectDescriptor(createdProject.getRepository()
                        .getId(), createdProject.getBusinessName(), createdProject.getRealPath(), branch, createdProject.getVersion());

                deployConfiguration.getFileData().setComment("Deploy configuration " + projectName + " is created.");

                deployConfiguration.save();
                var repositoryConfigName = deploymentManager.getRepositoryConfigNames().iterator().next();
                deploymentManager.deploy(deployConfiguration, repositoryConfigName, userWorkspace.getUser(), null);
            }
        } catch (Exception ex) {
            LOG.error("Project: {}", projectName, ex);
        } finally {
            projectCreator.destroy();
        }
    }
}
