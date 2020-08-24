package org.openl.rules.webstudio.web.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.resolving.ProjectDescriptorArtefactResolver;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Aleh Bykhavets
 */
public class DependencyChecker {

    private final Logger log = LoggerFactory.getLogger(DependencyChecker.class);
    /**
     * project-name -> project-version
     * <p/>
     * value can be <code>null</code> if such project wasn't found in DTR
     */
    private final Map<String, CommonVersion> projectVersions = new HashMap<>();

    /**
     * project name -> dependencies list
     */
    private final Map<String, List<ProjectDependencyDescriptor>> projectDependencies = new HashMap<>();

    private final ProjectDescriptorArtefactResolver projectDescriptorArtefactResolver;

    public DependencyChecker(ProjectDescriptorArtefactResolver projectDescriptorArtefactResolver) {
        this.projectDescriptorArtefactResolver = projectDescriptorArtefactResolver;
    }

    public void addProject(AProject project) {
        String projectName = project.getName();
        try {
            projectDependencies.put(projectName, projectDescriptorArtefactResolver.getDependencies(project));
            projectVersions.put(projectName, project.getVersion());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            projectVersions.put(projectName, null);
        }
    }

    void addProjects(ADeploymentProject deploymentProject) {
        UserWorkspace workspace = WebStudioUtils.getUserWorkspace(WebStudioUtils.getSession());
        if (workspace == null) {
            return; // must never happen
        }
        DesignTimeRepository designRepository = workspace.getDesignTimeRepository();

        for (ProjectDescriptor descriptor : deploymentProject.getProjectDescriptors()) {
            String projectName = descriptor.getProjectName();
            CommonVersion projectVersion = descriptor.getProjectVersion();

            try {
                String repositoryId = descriptor.getRepositoryId();
                if (repositoryId == null) {
                    repositoryId = designRepository.getRepositories().get(0).getId();
                }
                if (designRepository.hasProject(repositoryId, projectName)) {
                    addProject(designRepository.getProject(repositoryId, projectName, projectVersion));
                } else {
                    projectVersions.put(projectName, null);
                }
            } catch (Exception e) {
                log.error("Cannot get project '{}' version {}.", projectName, projectVersion.getVersionName(), e);

                // WARNING: trick
                projectVersions.put(projectName, null);
            }
        }
    }

    public boolean check() {
        // iterate over all projects
        for (String projectName : projectVersions.keySet()) {
            if (!checkProject(projectName, null)) {
                // something is wrong
                return false;
            }
        }

        // no problems, seems OK
        return true;
    }

    public boolean check(List<? extends AbstractItem> items) {
        boolean result = true;

        // iterate over all projects
        for (AbstractItem item : items) {
            if (!checkProject(item.getName(), item)) {
                // something is wrong
                result = false;
            }
        }

        return result;
    }

    protected boolean checkProject(String projectName, AbstractItem item) {
        if (projectVersions.get(projectName) == null) {
            // project with such name wasn't found in the repository
            if (item != null) {
                item.setMessages(
                    "Cannot find project <b>" + StringEscapeUtils.escapeHtml4(projectName) + "</b> in the repository.");
                item.setStyleForMessages(UiConst.STYLE_ERROR);
            }
            return false;
        }

        List<ProjectDependencyDescriptor> dependencies = projectDependencies.get(projectName);
        if (dependencies == null) {
            return true;
        }

        // check whether all dependent projects are here
        for (ProjectDependencyDescriptor dependentProject : dependencies) {
            if (!projectVersions.containsKey(dependentProject.getName())) {
                // dependent project is absent
                if (item != null) {
                    item.setMessages("Dependent project <b>" + StringEscapeUtils
                        .escapeHtml4(dependentProject.getName()) + "</b> should be added too.");
                    item.setStyleForMessages(UiConst.STYLE_WARNING);
                }
                return false;
            }
        }

        // no problems, seems OK
        return true;
    }
}
