package org.openl.rules.webstudio.dependencies;

import java.util.Collection;
import java.util.LinkedHashSet;

import org.openl.CompiledOpenClass;
import org.openl.dependency.CompiledDependency;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.rules.project.instantiation.AbstractDependencyManager;
import org.openl.rules.project.instantiation.SimpleDependencyLoader;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.types.NullOpenClass;

final class WebStudioDependencyLoader extends SimpleDependencyLoader {

    private final WebStudioWorkspaceRelatedDependencyManager webStudioWorkspaceRelatedDependencyManager;

    public WebStudioDependencyLoader(ProjectDescriptor project,
            Module module,
            WebStudioWorkspaceRelatedDependencyManager dependencyManager) {
        super(project, module, false, dependencyManager);
        this.webStudioWorkspaceRelatedDependencyManager = dependencyManager;
    }

    @Override
    protected CompiledDependency onCompilationFailure(Exception ex, AbstractDependencyManager dependencyManager) {
        ClassLoader classLoader = dependencyManager.getExternalJarsClassLoader(getProject());
        return createFailedCompiledDependency(getDependencyName(), classLoader, ex);
    }

    @Override
    protected boolean isActualDependency() {
        final Long currentThreadVersion = webStudioWorkspaceRelatedDependencyManager.getThreadVersion().get();
        final Long version = webStudioWorkspaceRelatedDependencyManager.getVersion().get();
        return currentThreadVersion >= version;
    }

    private CompiledDependency createFailedCompiledDependency(String dependencyName,
            ClassLoader classLoader,
            Exception ex) {
        Collection<OpenLMessage> messages = new LinkedHashSet<>();
        for (OpenLMessage openLMessage : OpenLMessagesUtils.newErrorMessages(ex)) {
            String message = String
                .format("Failed to load dependent module '%s': %s", dependencyName, openLMessage.getSummary());
            messages.add(new OpenLMessage(message, Severity.ERROR));
        }

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);

        try {
            return new CompiledDependency(dependencyName, new CompiledOpenClass(NullOpenClass.the, messages));
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }
}
