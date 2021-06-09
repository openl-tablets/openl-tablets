package org.openl.rules.deploy;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.project.abstraction.IDeployment;
import org.openl.rules.project.abstraction.IProject;

public class LocalDeployment implements IDeployment {

    private final String deploymentName;
    private final  CommonVersion version;
    private final Map<String, IProject> projects;

    public LocalDeployment(String deploymentName, CommonVersion version, Map<String, IProject> projects) {
        this.deploymentName = deploymentName;
        this.version = version;
        this.projects = Collections.unmodifiableMap(projects);
    }

    @Override
    public String getDeploymentName() {
        return deploymentName;
    }

    @Override
    public CommonVersion getCommonVersion() {
        return version;
    }

    @Override
    public Collection<IProject> getProjects() {
        return projects.values();
    }

    @Override
    public IProject getProject(String name) {
        return projects.get(name);
    }
}
