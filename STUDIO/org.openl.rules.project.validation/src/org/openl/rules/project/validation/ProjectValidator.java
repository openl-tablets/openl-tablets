package org.openl.rules.project.validation;

import org.openl.CompiledOpenClass;
import org.openl.rules.project.model.ProjectDescriptor;

public interface ProjectValidator {
    CompiledOpenClass validate(ProjectDescriptor projectDescriptor, CompiledOpenClass compiledOpenClass);
}
