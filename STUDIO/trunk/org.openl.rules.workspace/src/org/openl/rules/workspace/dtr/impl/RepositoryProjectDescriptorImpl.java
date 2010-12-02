package org.openl.rules.workspace.dtr.impl;

import org.openl.rules.repository.CommonVersion;
import org.openl.rules.repository.CommonVersionImpl;
import org.openl.rules.repository.RProjectDescriptor;
import org.openl.rules.repository.RVersion;
import org.openl.rules.workspace.abstracts.ProjectDescriptor;
import org.openl.rules.workspace.abstracts.ProjectException;

public class RepositoryProjectDescriptorImpl implements ProjectDescriptor {
    private String projectName;
    private CommonVersion projectVersion;

    public RepositoryProjectDescriptorImpl(RProjectDescriptor rulesProjectDescr) {
        projectName = rulesProjectDescr.getProjectName();
        RVersion rv = rulesProjectDescr.getProjectVersion();
        projectVersion = new CommonVersionImpl(rv.getMajor(), rv.getMinor(), rv.getRevision());
    }

    public RepositoryProjectDescriptorImpl(String projectName, CommonVersion projectVersion) {
        this.projectName = projectName;
        this.projectVersion = projectVersion;
    }

    public String getProjectName() {
        return projectName;
    }

    public CommonVersion getProjectVersion() {
        return projectVersion;
    }

    public void setProjectVersion(CommonVersion version) throws ProjectException {
        projectVersion = new CommonVersionImpl(version);
    }
}
