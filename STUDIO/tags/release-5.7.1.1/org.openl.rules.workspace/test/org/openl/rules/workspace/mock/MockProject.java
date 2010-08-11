package org.openl.rules.workspace.mock;

import java.util.Collection;
import java.util.LinkedList;

import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectDependency;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;

public class MockProject extends MockFolder implements Project {
    private Collection<ProjectDependency> dependencies = new LinkedList<ProjectDependency>();

    public MockProject(String name) {
        super(name, null);
    }

    public ProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException {
        throw new ProjectException("not implemented", null);
    }

    public Collection<ProjectDependency> getDependencies() {
        return dependencies;
    }

    public ProjectVersion getVersion() {
        return null;
    }

    public void setDependencies(Collection<ProjectDependency> dependencies) {
        this.dependencies = dependencies;
    }
}
