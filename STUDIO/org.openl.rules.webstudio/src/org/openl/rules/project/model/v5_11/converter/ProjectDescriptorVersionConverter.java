package org.openl.rules.project.model.v5_11.converter;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ObjectVersionConverter;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.v5_11.Module_v5_11;
import org.openl.rules.project.model.v5_11.ProjectDescriptor_v5_11;

/**
 * @author nsamatov.
 */
public class ProjectDescriptorVersionConverter
        implements ObjectVersionConverter<ProjectDescriptor, ProjectDescriptor_v5_11> {
    private final ModuleVersionConverter moduleVersionConverter = new ModuleVersionConverter();

    @Override
    public ProjectDescriptor fromOldVersion(ProjectDescriptor_v5_11 oldVersion) {
        ProjectDescriptor descriptor = new ProjectDescriptor();
        descriptor.setName(oldVersion.getName());
        descriptor.setComment(oldVersion.getComment());
        descriptor.setClasspath(oldVersion.getClasspath());

        List<Module> modules = new ArrayList<Module>(
                CollectionUtils.collect(oldVersion.getModules(),
                        new Transformer<Module_v5_11, Module>() {
                            @Override public Module transform(Module_v5_11 input) {
                                return moduleVersionConverter.fromOldVersion(input);
                            }
                        })
        );
        descriptor.setModules(modules);

        return descriptor;
    }

    @Override
    public ProjectDescriptor_v5_11 toOldVersion(ProjectDescriptor currentVersion) {
        ProjectDescriptor_v5_11 descriptor = new ProjectDescriptor_v5_11();

        descriptor.setId(currentVersion.getName());
        descriptor.setName(currentVersion.getName());
        descriptor.setComment(currentVersion.getComment());
        descriptor.setClasspath(currentVersion.getClasspath());

        List<Module_v5_11> modules = new ArrayList<Module_v5_11>(
                CollectionUtils.collect(currentVersion.getModules(),
                        new Transformer<Module, Module_v5_11>() {
                            @Override public Module_v5_11 transform(Module input) {
                                return moduleVersionConverter.toOldVersion(input);
                            }
                        })
        );
        descriptor.setModules(modules);

        return descriptor;
    }
}
