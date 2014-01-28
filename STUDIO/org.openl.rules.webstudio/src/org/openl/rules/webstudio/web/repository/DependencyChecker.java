package org.openl.rules.webstudio.web.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.resolving.DependencyResolverForRevision;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.openl.rules.workspace.uw.UserWorkspace;

/**
 *
 * @author Aleh Bykhavets
 *
 */
public class DependencyChecker {

    private final Log log = LogFactory.getLog(DependencyChecker.class);
    /**
     * project-name -> project-version
     * <p>
     * value can be <code>null</code> if such project wasn't found in DTR
     */
    private Map<String, CommonVersion> projectVersions = new HashMap<String, CommonVersion>();

    /**
     * project name -> dependencies list
     */
    private Map<String, List<ProjectDependencyDescriptor>> projectDependencies = new HashMap<String, List<ProjectDependencyDescriptor>>();

    private final DependencyResolverForRevision dependencyResolverForRevision;

    public DependencyChecker(DependencyResolverForRevision dependencyResolverForRevision) {
        this.dependencyResolverForRevision = dependencyResolverForRevision;
    }

    public void addProject(AProject project) {
        String projectName = project.getName();
        try {
            projectDependencies.put(projectName, dependencyResolverForRevision.getDependencies(project));
            projectVersions.put(projectName, project.getVersion());
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
            projectVersions.put(projectName, null);
        }
    }

    public void addProjects(ADeploymentProject deploymentProject) {
        UserWorkspace workspace = RepositoryUtils.getWorkspace();
        if (workspace == null) {
            return; // must never happen
        }
        DesignTimeRepository designRepository = workspace.getDesignTimeRepository();

        for (ProjectDescriptor descriptor : deploymentProject.getProjectDescriptors()) {
            String projectName = descriptor.getProjectName();
            CommonVersion projectVersion = descriptor.getProjectVersion();

            try {
                if (designRepository.hasProject(projectName)) {
                    addProject(designRepository.getProject(projectName, projectVersion));
                }else{
                    projectVersions.put(projectName, null);
                }
            } catch (RepositoryException e) {
                String msg = "Cannot get project '" + projectName + "' version " + projectVersion.getVersionName()
                        + "!";
                log.error(msg, e);

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
                item.setMessages("Cannot find project <b>" + StringEscapeUtils.escapeHtml(projectName)
                        + "</b> in the repository!");
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
                    item.setMessages("Dependent project <b>" + StringEscapeUtils.escapeHtml(dependentProject.getName())
                            + "</b> should be added too!");
                    item.setStyleForMessages(UiConst.STYLE_WARNING);
                }
                return false;
            }
        }

        // no problems, seems OK
        return true;
    }
}
