package org.openl.rules.project.model.v5_13.converter;

import java.util.List;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ObjectVersionConverter;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.v5_12.ProjectDependencyDescriptor_v5_12;
import org.openl.rules.project.model.v5_12.converter.ProjectDependencyDescriptorVersionConverter;
import org.openl.rules.project.model.v5_13.Module_v5_13;
import org.openl.rules.project.model.v5_13.ProjectDescriptor_v5_13;
import org.openl.util.CollectionUtils;

/**
 * @author nsamatov.
 */
public class ProjectDescriptorVersionConverter implements ObjectVersionConverter<ProjectDescriptor, ProjectDescriptor_v5_13> {
    private final ModuleVersionConverter moduleVersionConverter = new ModuleVersionConverter();
    private final ProjectDependencyDescriptorVersionConverter dependencyConverter = new ProjectDependencyDescriptorVersionConverter();

    @Override
    public ProjectDescriptor fromOldVersion(ProjectDescriptor_v5_13 oldVersion) {
        ProjectDescriptor descriptor = new ProjectDescriptor();
        descriptor.setName(oldVersion.getName());
        descriptor.setComment(oldVersion.getComment());
        descriptor.setClasspath(oldVersion.getClasspath());

        List<Module> modules = CollectionUtils.map(oldVersion.getModules(), moduleVersionConverter::fromOldVersion);
        descriptor.setModules(modules);

        List<ProjectDependencyDescriptor> dependencies = CollectionUtils.map(oldVersion.getDependencies(),
            dependencyConverter::fromOldVersion);

        if (CollectionUtils.isNotEmpty(dependencies)) {
            descriptor.setDependencies(dependencies);
        }

        descriptor.setPropertiesFileNamePattern(oldVersion.getPropertiesFileNamePattern());
        descriptor.setPropertiesFileNameProcessor(oldVersion.getPropertiesFileNameProcessor());

        return descriptor;
    }

    @Override
    public ProjectDescriptor_v5_13 toOldVersion(ProjectDescriptor currentVersion) {
        ProjectDescriptor_v5_13 descriptor = new ProjectDescriptor_v5_13();

        descriptor.setName(currentVersion.getName());
        descriptor.setComment(currentVersion.getComment());
        descriptor.setClasspath(currentVersion.getClasspath());

        List<Module_v5_13> modules = CollectionUtils.map(currentVersion.getModules(),
            moduleVersionConverter::toOldVersion);
        descriptor.setModules(modules);

        List<ProjectDependencyDescriptor_v5_12> dependencies = CollectionUtils.map(currentVersion.getDependencies(),
            dependencyConverter::toOldVersion);
        if (CollectionUtils.isNotEmpty(dependencies)) {
            descriptor.setDependencies(dependencies);
        }

        descriptor.setPropertiesFileNamePattern(currentVersion.getPropertiesFileNamePattern());
        descriptor.setPropertiesFileNameProcessor(currentVersion.getPropertiesFileNameProcessor());

        return descriptor;
    }
}
