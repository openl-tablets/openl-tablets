package org.openl.rules.ruleservice.publish.lazy;

import java.util.Objects;

import org.openl.rules.ruleservice.core.DeploymentDescription;

public class Key {
    final String dependencyName;
    final DeploymentDescription deploymentDescription;

    public DeploymentDescription getDeploymentDescription() {
        return deploymentDescription;
    }

    public Key(DeploymentDescription deploymentDescription, String dependencyName) {
        this.deploymentDescription = Objects.requireNonNull(deploymentDescription, "deploymentDescription can't be null.");
        this.dependencyName = Objects.requireNonNull(dependencyName, "dependencyName can't be null.");
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 31;
        result = prime * result + ((deploymentDescription == null) ? 0 : deploymentDescription.hashCode());
        result = prime * result + ((dependencyName == null) ? 0 : dependencyName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Key other = (Key) obj;
        if (deploymentDescription == null) {
            if (other.deploymentDescription != null) {
                return false;
            }
        } else if (!deploymentDescription.equals(other.deploymentDescription)) {
            return false;
        }
        if (dependencyName == null) {
            if (other.dependencyName != null) {
                return false;
            }
        } else if (!dependencyName.equals(other.dependencyName)) {
            return false;
        }
        return true;
    }

}
