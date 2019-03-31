package org.openl.rules.project.model.v5_16.converter;

import java.util.List;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ObjectVersionConverter;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.v5_12.ProjectDependencyDescriptor_v5_12;
import org.openl.rules.project.model.v5_12.converter.ProjectDependencyDescriptorVersionConverter;
import org.openl.rules.project.model.v5_16.Module_v5_16;
import org.openl.rules.project.model.v5_16.ProjectDescriptor_v5_16;
import org.openl.util.CollectionUtils;

/**
 * @author nsamatov.
 */
public class ProjectDescriptorVersionConverter implements ObjectVersionConverter<ProjectDescriptor, ProjectDescriptor_v5_16> {
    private final ModuleVersionConverter moduleVersionConverter = new ModuleVersionConverter();
    private final ProjectDependencyDescriptorVersionConverter dependencyConverter = new ProjectDependencyDescriptorVersionConverter();

    @Override
    public ProjectDescriptor fromOldVersion(ProjectDescriptor_v5_16 oldVersion) {
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
    public ProjectDescriptor_v5_16 toOldVersion(ProjectDescriptor currentVersion) {
        ProjectDescriptor_v5_16 descriptor = new ProjectDescriptor_v5_16();

        descriptor.setName(currentVersion.getName());
        descriptor.setComment(currentVersion.getComment());
        descriptor.setClasspath(currentVersion.getClasspath());

        List<Module_v5_16> modules = CollectionUtils.map(currentVersion.getModules(),
            new CollectionUtils.Mapper<Module, Module_v5_16>() {
                @Override
                public Module_v5_16 map(Module input) {
                    return moduleVersionConverter.toOldVersion(input);
                }
            });
        descriptor.setModules(modules);

        List<ProjectDependencyDescriptor_v5_12> dependencies = CollectionUtils.map(currentVersion.getDependencies(),
            new CollectionUtils.Mapper<ProjectDependencyDescriptor, ProjectDependencyDescriptor_v5_12>() {
                @Override
                public ProjectDependencyDescriptor_v5_12 map(ProjectDependencyDescriptor input) {
                    return dependencyConverter.toOldVersion(input);
                }
            });
        if (CollectionUtils.isNotEmpty(dependencies)) {
            descriptor.setDependencies(dependencies);
        }

        descriptor.setPropertiesFileNamePattern(currentVersion.getPropertiesFileNamePattern());
        descriptor.setPropertiesFileNameProcessor(currentVersion.getPropertiesFileNameProcessor());

        return descriptor;
    }
}
