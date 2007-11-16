package org.openl.rules.workspace.deploy.impl.jcr;

import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.deploy.DeploymentException;
import org.openl.rules.workspace.deploy.ProductionDeployer;
import org.openl.SmartProps;

import java.util.Collection;
import java.io.File;

public class JcrProductionDeployer implements ProductionDeployer {
    public static final String PROPNAME_ZIPFOLDER = "temp.zip.location";
    public static final String DEFAULT_ZIPFOLDER = "/tmp/rules-deployment/";

    private final File tempFolder;

    public JcrProductionDeployer(WorkspaceUser user, SmartProps props) throws DeploymentException {
        String location = props.getStr(PROPNAME_ZIPFOLDER, DEFAULT_ZIPFOLDER);
        tempFolder = new File(location, user.getUserId());
        if (!FolderHelper.checkOrCreateFolder(tempFolder)) {
            throw new DeploymentException("can not create temp folder: {0}", null, location);
        }
    }

    public DeployID deploy(Collection<Project> projects) throws DeploymentException {
        return deploy(null, projects);
    }

    public synchronized DeployID deploy(DeployID id, Collection<Project> projects)
            throws DeploymentException {

        return id;
    }
}
