package org.open.rules.project.validation.openapi;

import org.openl.CompiledOpenClass;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.test.RulesInFolderTestRunner;

final class RulesInFolderTestRunnerWithOpenApiValidator extends RulesInFolderTestRunner {
    public RulesInFolderTestRunnerWithOpenApiValidator(boolean allTestsMustFails, boolean executionMode) {
        super(allTestsMustFails, executionMode);
    }

    @Override
    protected CompiledOpenClass validate(CompiledOpenClass compiledOpenClass,
            ProjectDescriptor projectDescriptor,
            RulesInstantiationStrategy rulesInstantiationStrategy) {
        try {
            OpenApiProjectValidator openApiProjectValidator = new OpenApiProjectValidator();
            return openApiProjectValidator.validate(projectDescriptor, rulesInstantiationStrategy);
        } catch (RulesInstantiationException e) {
            return compiledOpenClass;
        }
    }
}
