package org.openl.rules.ruleservice;

import org.openl.rules.workspace.deploy.DeployID;

public class DeploymentInfo {
    private String name;
    private String version;
    private final static char SEPARATOR = '#';

    public DeploymentInfo(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeploymentInfo)) return false;

        DeploymentInfo that = (DeploymentInfo) o;

        if (!name.equals(that.name)) return false;
        if (version != null ? !version.equals(that.version) : that.version != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = name.hashCode();
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }

    public DeployID getDeployID() {
        return new DeployID(name + SEPARATOR + version);
    }

    public static DeploymentInfo valueOf(String deployment) {
        if (deployment == null) {
            throw new NullPointerException();
        }

        int pos = deployment.lastIndexOf(SEPARATOR);
        if (pos < 0) {
            return new DeploymentInfo(deployment, null);
        }
        return new DeploymentInfo(deployment.substring(0, pos), deployment.substring(pos + 1));
    }
}
