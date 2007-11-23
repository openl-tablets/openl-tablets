package org.openl.rules.workspace.deploy;

import org.openl.rules.workspace.abstracts.Project;

import java.util.Collection;

public interface ProductionDeployer {
    DeployID deploy(Collection<? extends Project> projects) throws DeploymentException;
    DeployID deploy(DeployID id, Collection<? extends Project> projects) throws DeploymentException;
}
