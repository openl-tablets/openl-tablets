package org.openl.rules.ruleservice.loader;

import java.io.File;
import java.util.EventObject;

public class LoadingEventObject extends EventObject {
    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = -5989538358550371845L;

    private DeploymentInfo deploymentInfo;
    private File deploymentLocalFolder;

    public LoadingEventObject(Object eventSource, DeploymentInfo deploymentInfo) {
        this(eventSource, deploymentInfo, null);
    }

    public LoadingEventObject(Object eventSource, DeploymentInfo deploymentInfo, File deploymentLocalFolder) {
        super(eventSource);
        this.deploymentInfo = deploymentInfo;
        this.deploymentLocalFolder = deploymentLocalFolder;
    }

    public DeploymentInfo getDeploymentInfo() {
        return deploymentInfo;
    }

    public File getDeploymentLocalFolder() {
        return deploymentLocalFolder;
    }
}