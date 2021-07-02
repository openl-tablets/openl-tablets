package org.openl.rules.project.validation;

import org.openl.CompiledOpenClass;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.model.ProjectDescriptor;

public interface ProjectValidator {
    CompiledOpenClass validate(ProjectDescriptor projectDescriptor,
            RulesInstantiationStrategy rulesInstantiationStrategy) throws RulesInstantiationException;
}
