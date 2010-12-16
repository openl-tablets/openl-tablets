package org.openl.rules.webstudio.web.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectDependency;
import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.openl.rules.workspace.uw.UserWorkspace;

/**
 *
 * @author Aleh Bykhavets
 *
 */
public class DependencyChecker {
    private class VersionRange {
        private CommonVersion lower;
        private CommonVersion upper;

        private VersionRange(CommonVersion lower, CommonVersion upper) {
            this.lower = lower;
            this.upper = upper;
        }

        private boolean isInRange(CommonVersion version) {
            boolean a = (lower.compareTo(version) <= 0);
            boolean b = (upper == null) || (upper.compareTo(version) >= 0);

            return (a && b);
        }

        @Override
        public String toString() {
            if (upper == null) {
                return lower.getVersionName() + " - ...";
            } else {
                return lower.getVersionName() + " - " + upper.getVersionName();
            }
        }
    }

    private static final Log LOG = LogFactory.getLog(DependencyChecker.class);
    /**
     * project-name -> project-version
     * <p>
     * value can be <code>null</code> if such project wasn't found in DTR
     */
    private Map<String, CommonVersion> projectVersions;

    /**
     * project-name -> [dependent-project, version-range]
     */
    private Map<String, Map<String, VersionRange>> projectDependencies;

    public DependencyChecker() {
        projectVersions = new HashMap<String, CommonVersion>();
        projectDependencies = new HashMap<String, Map<String, VersionRange>>();
    }

    public void addProject(AProject project) {
        String projectName = project.getName();
        projectVersions.put(projectName, project.getVersion());

        Map<String, VersionRange> dependencies = new TreeMap<String, VersionRange>();
        projectDependencies.put(projectName, dependencies);

        for (ProjectDependency dependency : project.getDependencies()) {
            dependencies.put(dependency.getProjectName(), new VersionRange(dependency.getLowerLimit(), dependency
                    .getUpperLimit()));
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
                addProject(designRepository.getProject(projectName, projectVersion));
            } catch (RepositoryException e) {
                String msg = "Cannot get project '" + projectName + "' version " + projectVersion.getVersionName()
                        + "!";
                LOG.error(msg, e);

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

        Map<String, VersionRange> dependencies = projectDependencies.get(projectName);

        // check version conflicts
        for (Entry<String, VersionRange> entry : dependencies.entrySet()) {
            String dependentName = entry.getKey();
            VersionRange range = entry.getValue();

            CommonVersion version = projectVersions.get(dependentName);
            if (version == null) {
                // absence will be checked later
                // version conflicts have higher priority
                continue;
            }

            if (!range.isInRange(version)) {
                // version conflict
                if (item != null) {
                    item.setMessages("Conflicting with project <b>" + StringEscapeUtils.escapeHtml(dependentName)
                            + "</b>! Valid versions are " + range.toString());
                    item.setStyleForMessages(UiConst.STYLE_ERROR);
                }
                return false;
            }
        }

        // check whether all dependent projects are here
        for (String dependentProject : dependencies.keySet()) {
            if (!projectVersions.containsKey(dependentProject)) {
                // dependent project is absent
                if (item != null) {
                    item.setMessages("Dependent project <b>" + StringEscapeUtils.escapeHtml(dependentProject)
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
