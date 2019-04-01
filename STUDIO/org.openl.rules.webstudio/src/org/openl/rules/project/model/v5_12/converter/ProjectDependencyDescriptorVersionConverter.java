package org.openl.rules.project.model.v5_12.converter;

import org.openl.rules.project.model.ObjectVersionConverter;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.v5_12.ProjectDependencyDescriptor_v5_12;

public class ProjectDependencyDescriptorVersionConverter implements ObjectVersionConverter<ProjectDependencyDescriptor, ProjectDependencyDescriptor_v5_12> {
    @Override
    public ProjectDependencyDescriptor fromOldVersion(ProjectDependencyDescriptor_v5_12 oldVersion) {
        ProjectDependencyDescriptor dependency = new ProjectDependencyDescriptor();
        dependency.setName(oldVersion.getName());
        dependency.setAutoIncluded(oldVersion.isAutoIncluded());
        return dependency;
    }

    @Override
    public ProjectDependencyDescriptor_v5_12 toOldVersion(ProjectDependencyDescriptor currentVersion) {
        ProjectDependencyDescriptor_v5_12 dependency = new ProjectDependencyDescriptor_v5_12();
        dependency.setName(currentVersion.getName());
        dependency.setAutoIncluded(currentVersion.isAutoIncluded());
        return dependency;
    }
}
