package org.openl.rules.ruleservice.loader;

import java.io.Serializable;

import org.openl.rules.repository.CommonVersionImpl;
import org.openl.rules.workspace.deploy.DeployID;

/**
 * A class that represents info on a deployment: its name and its version.
 */
public class DeploymentInfo implements Serializable {
    private static final long serialVersionUID = 5284876042162952913L;

    /**
     * Separator between deployment name and its version in
     * <code>DeployID</code> object.
     */
    private static final char SEPARATOR = '#';
    
    /**
     * Deployment name.
     */
    private String name;

    /**
     * Deployment version.
     */
    private CommonVersionImpl version;

    /**
     * Parses string representation of deployment.
     *
     * @param deployment string representing deployment
     * @return DeploymentInfo or <code>null</code> if <code>deployment</code>
     *         has invalid format.
     */
    public static DeploymentInfo valueOf(String deployment) {
        DeploymentInfo parsedDeploymentInfo = null;

        if (deployment != null) {
            int separatorPosition = deployment.lastIndexOf(SEPARATOR);

            if (separatorPosition < 0) {
                parsedDeploymentInfo = new DeploymentInfo(deployment, null);
            } else {
                parsedDeploymentInfo = new DeploymentInfo(deployment.substring(0, separatorPosition), deployment
                        .substring(separatorPosition + 1));
            }
        }

        return parsedDeploymentInfo;
    }

    /**
     * Creates a deployment info from name and string representation of version.
     *
     * @param name deployment name
     * @param version deployment version
     */
    private DeploymentInfo(String name, String version) {
        this.name = name;
        this.version = new CommonVersionImpl(version);
    }

    /**
     * Returns <code>DeployID</code> object corresponding to this
     * <code>DeploymentInfo</code>.
     *
     * @return DeployID instance
     */
    public DeployID getDeployID() {
        return new DeployID(name + SEPARATOR + version.getVersionName());
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
     * Returns deployment version.
     * 
     * @return deployment version
     */
    public CommonVersionImpl getVersion() {
        return version;
    }
    
    /**
     * <code>DeploymentInfo</code>s are equal if their names and versions are
     * equal.
     *
     * @param o object to compare with
     * @return if objects are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DeploymentInfo)) {
            return false;
        }

        DeploymentInfo that = (DeploymentInfo) o;

        if (!name.equals(that.name)) {
            return false;
        }
        if (version != null && !version.equals(that.version)) {
            return false;
        }
        if (version == null && that.version != null) {
            return false;
        }

        return true;
    }

    /**
     * Computing hash code consistent with {@link #equals(Object)}.
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
}
