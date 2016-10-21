package org.openl.rules.webstudio.dependencies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openl.CompiledOpenClass;
import org.openl.dependency.CompiledDependency;
import org.openl.exception.OpenLCompilationException;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.rules.project.instantiation.AbstractProjectDependencyManager;
import org.openl.rules.project.instantiation.SimpleProjectDependencyLoader;
import org.openl.rules.project.model.Module;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.NullOpenClass;

final class WebStudioDependencyLoader extends SimpleProjectDependencyLoader {

    WebStudioDependencyLoader(String dependencyName, Collection<Module> modules, boolean singleModuleMode) {
        super(dependencyName, modules, singleModuleMode, false);
    }

    @Override
    protected CompiledDependency onCompilationFailure(Exception ex, AbstractProjectDependencyManager dependencyManager) throws OpenLCompilationException {
        ClassLoader classLoader = dependencyManager.getClassLoader(getModules().iterator().next().getProject());
        return createFailedCompiledDependency(getDependencyName(), classLoader, ex);
    }

    private CompiledDependency createFailedCompiledDependency(String dependencyName,
            ClassLoader classLoader,
            Exception ex) {
        List<OpenLMessage> messages = new ArrayList<OpenLMessage>();
        for (OpenLMessage openLMessage : OpenLMessagesUtils.newMessages(ex)) {
            String message = String.format("Can't load dependent module '%s': %s",
                dependencyName,
                openLMessage.getSummary());
            messages.add(new OpenLMessage(message, Severity.ERROR));
        }

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);

        try {
            return new CompiledDependency(dependencyName, new CompiledOpenClass(NullOpenClass.the,
                messages,
                new SyntaxNodeException[0],
                new SyntaxNodeException[0]));
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }
}
