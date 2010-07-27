package org.openl.rules.webstudio.web.repository;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectVersion;

import java.util.Comparator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceDeploymentProject;

/**
 * Repository Utilities
 *
 * @author Aleh Bykhavets
 *
 */
public class RepositoryUtils {
    private static final Log LOG = LogFactory.getLog(RepositoryUtils.class);

    public static final Comparator<ProjectVersion> VERSIONS_REVERSE_COMPARATOR = new Comparator<ProjectVersion>() {
        public int compare(ProjectVersion o1, ProjectVersion o2) {
            return o2.compareTo(o1);
        }
    };
    public static final Comparator<ProjectArtefact> ARTEFACT_COMPARATOR = new Comparator<ProjectArtefact>() {
        public int compare(ProjectArtefact o1, ProjectArtefact o2) {
            if (o1.isFolder() == o2.isFolder()) {
                return o1.getName().compareTo(o2.getName());
            } else {
                return (o1.isFolder() ? (-1) : 1);
            }
        }
    };

    public static DeployID getDeployID(UserWorkspaceDeploymentProject ddProject) {
        StringBuilder sb = new StringBuilder(ddProject.getName());
        ProjectVersion projectVersion = ddProject.getVersion();
        if (projectVersion != null) {
            sb.append('#').append(projectVersion.getVersionName());
        }
        return new DeployID(sb.toString());
    }

    public static RulesUserSession getRulesUserSession() {
        return (RulesUserSession) FacesUtils.getSessionParam(Constants.RULES_USER_SESSION);
    }

    /**
     * @return user's workspace or <code>null</code>
     */
    public static UserWorkspace getWorkspace() {
        try {
            return getRulesUserSession().getUserWorkspace();
        } catch (Exception e) {
            LOG.error("Error obtaining user workspace", e);
        }
        return null;
    }
}
