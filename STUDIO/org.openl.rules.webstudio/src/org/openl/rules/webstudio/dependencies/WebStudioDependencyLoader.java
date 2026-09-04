package org.openl.rules.webstudio.dependencies;

import java.util.LinkedHashSet;

import org.openl.CompiledOpenClass;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.DependencyType;
import org.openl.exception.OpenLCompilationException;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.rules.lang.xls.load.LazyWorkbookLoaderFactory;
import org.openl.rules.lang.xls.load.WorkbookLoaders;
import org.openl.rules.project.instantiation.AbstractDependencyManager;
import org.openl.rules.project.instantiation.IDependencyLoader;
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
        var classLoader = dependencyManager.getExternalJarsClassLoader(getProject());
        return createFailedCompiledDependency(classLoader, ex);
    }

    @Override
    protected boolean isActualDependency() {
        final var currentThreadVersion = webStudioWorkspaceRelatedDependencyManager.getThreadVersion().get();
        final var version = webStudioWorkspaceRelatedDependencyManager.getVersion().get();
        return currentThreadVersion >= version;
    }

    private CompiledDependency createFailedCompiledDependency(ClassLoader classLoader, Exception ex) {
        var messages = new LinkedHashSet<OpenLMessage>();
        for (OpenLMessage openLMessage : OpenLMessagesUtils.newErrorMessages(ex)) {
            var message = "Failed to load dependent module '%s': %s"
                    .formatted(getDependency(), openLMessage.getSummary());
            messages.add(new OpenLMessage(message, Severity.ERROR));
        }

        var oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);

        try {
            return new CompiledDependency(getDependency(),
                    new CompiledOpenClass(NullOpenClass.the, messages),
                    isProjectLoader() ? DependencyType.PROJECT : DependencyType.MODULE);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    protected void onCompilationComplete(IDependencyLoader dependencyLoader, CompiledDependency compiledDependency) {
        super.onCompilationComplete(dependencyLoader, compiledDependency);
        webStudioWorkspaceRelatedDependencyManager.fireOnCompilationCompleteListeners(dependencyLoader,
                compiledDependency);
    }

    @Override
    protected void onResetComplete(IDependencyLoader dependencyLoader, CompiledDependency compiledDependency) {
        super.onResetComplete(dependencyLoader, compiledDependency);
        webStudioWorkspaceRelatedDependencyManager.fireOnResetCompleteListeners(dependencyLoader, compiledDependency);
    }

    @Override
    protected CompiledDependency compileDependency() throws OpenLCompilationException {
        try {
            var factory = new LazyWorkbookLoaderFactory(
                    ((WebStudioWorkspaceRelatedDependencyManager) getDependencyManager()).isCanUnload());
            WorkbookLoaders.setCurrentFactory(factory);
            return super.compileDependency();
        } finally {
            WorkbookLoaders.resetCurrentFactory();
        }
    }
}
