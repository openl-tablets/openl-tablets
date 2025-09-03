package org.openl.rules.webstudio;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Component;

import org.openl.rules.common.ProjectException;
import org.openl.rules.common.impl.ProjectDescriptorImpl;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.rest.exception.ConflictException;
import org.openl.rules.rest.exception.ForbiddenException;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.web.repository.DeploymentManager;
import org.openl.rules.webstudio.web.repository.DeploymentRequest;
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
@Component
@ConditionalOnProperty(name = "demo.init", havingValue = "true")
@DependsOn("singleUserModeInit")
public class DemoInit {
    private static final Logger LOG = LoggerFactory.getLogger(DemoInit.class);

    private final TemplatesResolver templatesResolver = new PredefinedTemplatesResolver();

    private final PathFilter zipFilter;
    private final MultiUserWorkspaceManager workspaceManager;
    private final DeploymentManager deploymentManager;
    private final UserManagementService userManagementService;
    private final AclProjectsHelper aclProjectsHelper;

    @Autowired
    public DemoInit(@Qualifier("zipFilter") PathFilter zipFilter,
                    MultiUserWorkspaceManager workspaceManager,
                    DeploymentManager deploymentManager,
                    UserManagementService userManagementService,
                    AclProjectsHelper aclProjectsHelper) {
        this.zipFilter = zipFilter;
        this.workspaceManager = workspaceManager;
        this.deploymentManager = deploymentManager;
        this.userManagementService = userManagementService;
        this.aclProjectsHelper = aclProjectsHelper;
    }

    @PostConstruct
    public void init() {
        try {
            var config = new HashMap<String, String>();
            config.put("demo.init", null);
            DynamicPropertySource.get().save(config);
        } catch (IOException ex) {
            LOG.error("Could not clean demo.init property", ex);
        }

        var usr = userManagementService.getAllUsers().getFirst();

        initUser("admin", "admin@example.com", "Admin", "Administrators");
        initUser("a1", "a1@example.com", "A1", "Administrators");
        initUser("u0", "u0@example.com", "U0", "Testers");
        initUser("u1", "u1@example.com", "U1", "Developers", "Analysts");
        initUser("u2", "u2@example.com", "U2", "Viewers");
        initUser("u3", "u3@example.com", "U3", "Viewers");
        initUser("u4", "u4@example.com", "U4", "Deployers");
        initUser("user", "user@example.com", "User", "Viewers");

        WorkspaceUserImpl user = new WorkspaceUserImpl(usr.getUsername(),
                (x) -> new UserInfo(usr.getUsername(),
                        usr.getEmail(),
                        usr.getDisplayName()));
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

    private void initUser(String user, String email, String displayName, String... groups) {
        userManagementService.addUser(user,
                null,
                null,
                user,
                email,
                displayName
        );
        userManagementService.updateAuthorities(user, new HashSet<>(Arrays.asList(groups)));

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
                var projectDescriptor = ProjectDescriptorImpl.builder()
                        .repositoryId(createdProject.getRepository().getId())
                        .projectName(createdProject.getBusinessName())
                        .path(createdProject.getRealPath())
                        .branch(createdProject.getBranch())
                        .projectVersion(createdProject.getVersion())
                        .build();

                var repositoryConfigName = deploymentManager.getRepositoryConfigNames().iterator().next();
                var request = DeploymentRequest.builder()
                        .name(projectName)
                        .productionRepositoryId(repositoryConfigName)
                        .projectDescriptors(List.of(projectDescriptor))
                        .currentUser(userWorkspace.getUser())
                        .build();
                var validBranchName = deploymentManager.validateOnMainBranch(request);
                if (validBranchName != null) {
                    throw new ConflictException("project.deploy.restricted.message", validBranchName);
                }
                if (!aclProjectsHelper.hasPermission(request, BasePermission.WRITE)) {
                    throw new ForbiddenException("default.message");
                }
                deploymentManager.deploy(request);
            }
        } catch (Exception ex) {
            LOG.error("Project: {}", projectName, ex);
        } finally {
            projectCreator.destroy();
        }
    }
}
