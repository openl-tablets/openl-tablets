package org.openl.rules.ruleservice.publish.lazy;

import java.util.Objects;

import org.openl.dependency.ResolvedDependency;
import org.openl.rules.ruleservice.core.DeploymentDescription;

public class Key {
    private final ResolvedDependency dependency;
    private final DeploymentDescription deploymentDescription;

    DeploymentDescription getDeploymentDescription() {
        return deploymentDescription;
    }

    Key(DeploymentDescription deploymentDescription, ResolvedDependency dependency) {
        this.deploymentDescription = Objects.requireNonNull(deploymentDescription,
            "deploymentDescription cannot be null");
        this.dependency = Objects.requireNonNull(dependency, "dependency cannot be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Key key = (Key) o;
        return dependency.equals(key.dependency) && deploymentDescription.equals(key.deploymentDescription);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dependency, deploymentDescription);
    }
}
