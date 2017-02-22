package org.openl.rules.webstudio.web.repository;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;

/**
 * Repository Utilities
 *
 * @author Aleh Bykhavets
 */
public final class RepositoryUtils {
    public static final Comparator<AProjectArtefact> ARTEFACT_COMPARATOR = new Comparator<AProjectArtefact>() {
        public int compare(AProjectArtefact o1, AProjectArtefact o2) {
            if (o1.isFolder() == o2.isFolder()) {
                return o1.getName().compareTo(o2.getName());
            } else {
                return (o1.isFolder() ? -1 : 1);
            }
        }
    };

    private RepositoryUtils() {
    }

    public static DeployID getDeployID(ADeploymentProject ddProject) {
        StringBuilder sb = new StringBuilder(ddProject.getName());
        ProjectVersion projectVersion = ddProject.getVersion();
        if (projectVersion != null) {
            sb.append('#').append(projectVersion.getVersionName());
        }
        return new DeployID(sb.toString());
    }

    /**
     * @deprecated
     */
    public static RulesUserSession getRulesUserSession() {
        return (RulesUserSession) FacesUtils.getSessionParam(Constants.RULES_USER_SESSION);
    }

    /**
     * @return user's workspace or <code>null</code>
     * @deprecated
     */
    public static UserWorkspace getWorkspace() {
        final Logger log = LoggerFactory.getLogger(RepositoryUtils.class);
        try {
            return getRulesUserSession().getUserWorkspace();
        } catch (Exception e) {
            log.error("Error obtaining user workspace", e);
        }
        return null;
    }

    public static String getTreeNodeId(String name) {
        if (StringUtils.isNotBlank(name)) {
            // FIXME name.hashCode() can produce collisions. Not good for id.
            return String.valueOf(name.hashCode());
        }
        return null;
    }

}
