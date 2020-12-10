package org.openl.rules.project.model.v5_12.converter;

import java.util.List;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ObjectVersionConverter;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.v5_11.Module_v5_11;
import org.openl.rules.project.model.v5_11.converter.ModuleVersionConverter;
import org.openl.rules.project.model.v5_12.ProjectDependencyDescriptor_v5_12;
import org.openl.rules.project.model.v5_12.ProjectDescriptor_v5_12;
import org.openl.util.CollectionUtils;

/**
 * @author nsamatov.
 */
public class ProjectDescriptorVersionConverter implements ObjectVersionConverter<ProjectDescriptor, ProjectDescriptor_v5_12> {
    private final ModuleVersionConverter moduleVersionConverter = new ModuleVersionConverter();
    private final ProjectDependencyDescriptorVersionConverter dependencyConverter = new ProjectDependencyDescriptorVersionConverter();

    @Override
    public ProjectDescriptor fromOldVersion(ProjectDescriptor_v5_12 oldVersion) {
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

        descriptor.setPropertiesFileNamePatterns(new String[]{oldVersion.getPropertiesFileNamePattern()});
        descriptor.setPropertiesFileNameProcessor(oldVersion.getPropertiesFileNameProcessor());

        return descriptor;
    }

    @Override
    public ProjectDescriptor_v5_12 toOldVersion(ProjectDescriptor currentVersion) {
        ProjectDescriptor_v5_12 descriptor = new ProjectDescriptor_v5_12();

        descriptor.setName(currentVersion.getName());
        descriptor.setComment(currentVersion.getComment());
        descriptor.setClasspath(currentVersion.getClasspath());

        List<Module_v5_11> modules = CollectionUtils.map(currentVersion.getModules(),
                moduleVersionConverter::toOldVersion);
        descriptor.setModules(modules);

        List<ProjectDependencyDescriptor_v5_12> dependencies = CollectionUtils.map(currentVersion.getDependencies(),
                dependencyConverter::toOldVersion);
        if (CollectionUtils.isNotEmpty(dependencies)) {
            descriptor.setDependencies(dependencies);
        }

        String[] patterns = currentVersion.getPropertiesFileNamePatterns();
        if (CollectionUtils.isNotEmpty(patterns)) {
            descriptor.setPropertiesFileNamePattern(patterns[0]);
        }
        descriptor.setPropertiesFileNameProcessor(currentVersion.getPropertiesFileNameProcessor());

        return descriptor;
    }
}
