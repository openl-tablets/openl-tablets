package org.openl.rules.workspace.abstracts;

public interface ProjectDescriptor {
    String getProjectName();
    
    ProjectVersion getProjectVersion();
    
    void setProjectVersion(ProjectVersion version) throws ProjectException;
    void delete();
}
