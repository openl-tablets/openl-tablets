package org.openl.rules.workspace.deploy.impl.jcr;

import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.deploy.DeploymentException;
import org.openl.rules.workspace.deploy.ProductionDeployer;

import java.util.Collection;

public class JcrProductionDeployer implements ProductionDeployer {
    public DeployID deploy(Collection<Project> projects) throws DeploymentException {
        return deploy(generateID(), projects);
    }

    public DeployID deploy(DeployID id, Collection<Project> projects) throws DeploymentException {
        return id;
    }

    private DeployID generateID() {
        return new DeployID(String.valueOf(System.currentTimeMillis()));
    }
}
