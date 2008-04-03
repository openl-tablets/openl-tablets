package org.openl.rules.ruleservice.publish;

import org.openl.rules.repository.CommonVersionImpl;
import org.openl.rules.workspace.deploy.DeployID;

/**
 * A class that represents info on a deployment: its name and its version.
 */
public class DeploymentInfo {
    /**
     * Deployment name.
     */
    private String name;
    /**
     * Deployment version.
     */
    private CommonVersionImpl version;
    private final static char SEPARATOR = '#';

    /**
     * Creates a deployment info from name and string representation of version.
     *
     * @param name    deployment name
     * @param version deployment version
     */
    private DeploymentInfo(String name, String version) {
        this.name = name;
        this.version = new CommonVersionImpl(version);
    }

    /**
     * Returns deployment name.
     *
     * @return deployment name
     */
    public String getName() {
        return name;
    }

    /**
     * @return deployment version
     */
    public CommonVersionImpl getVersion() {
        return version;
    }

    /**
     * <code>DeploymentInfo</code>s are equal iff their names and versions are equal.
     *
     * @param o object to compare with
     * @return if objects are equal
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeploymentInfo)) return false;

        DeploymentInfo that = (DeploymentInfo) o;

        if (!name.equals(that.name)) return false;
        if (version != null ? !version.equals(that.version) : that.version != null) return false;

        return true;
    }

    /**
     * Computing hash code consistent with {@link #equals(Object)}
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        int result;
        result = name.hashCode();
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }

    /**
     * Returns <code>DeployID</code> object corresponding to this <code>DeploymentInfo</code>
     *
     * @return DeployID instance
     */
    public DeployID getDeployID() {
        return new DeployID(name + SEPARATOR + version.getVersionName());
    }

    /**
     * Parses string representation of deployment.
     *
     * @param deployment string representing deployment
     * @return DeploymentInfo or <code>null</code> if <code>deployment</code> has invalid format.
     */
    public static DeploymentInfo valueOf(String deployment) {
        if (deployment == null) {
            throw new NullPointerException();
        }

        try {
            int pos = deployment.lastIndexOf(SEPARATOR);
            if (pos < 0) {
                return new DeploymentInfo(deployment, null);
            }
            return new DeploymentInfo(deployment.substring(0, pos), deployment.substring(pos + 1));
        } catch (Exception e) {
            return null;
        }
    }
}
