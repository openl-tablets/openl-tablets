package org.openl.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openl.CompiledOpenClass;
import org.openl.IOpenBinder;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundCode;
import org.openl.classloader.OpenLClassLoader;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.DependencyBindingContext;
import org.openl.dependency.IDependencyManager;
import org.openl.dependency.ResolvedDependency;
import org.openl.exception.OpenLCompilationException;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.OpenLWarnMessage;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.code.ProcessedCode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenClass;
import org.openl.util.CollectionUtils;

/**
 * Class that defines OpenL engine manager implementation for compilation operations.
 *
 */
public class OpenLCompileManager {
    public static final String EXTERNAL_DEPENDENCIES_KEY = "external-dependencies";
    public static final String ADDITIONAL_WARN_MESSAGES_KEY = "additional-warn-messages";
    public static final String ADDITIONAL_ERROR_MESSAGES_KEY = "additional-error-messages";

    private final OpenL openl;

    /**
     * Construct new instance of manager.
     *
     * @param openl {@link OpenL} instance
     */
    OpenLCompileManager(OpenL openl) {
        this.openl = openl;
    }

    /**
     * Compiles module. As a result a module open class will be returned by engine. All errors that occurred during
     * compilation are suppressed.
     *
     * @param source source
     * @param executionMode <code>true</code> if module should be compiled in memory optimized mode for only execution
     * @return {@link CompiledOpenClass} instance
     */
    CompiledOpenClass compileModuleWithErrors(IOpenSourceCodeModule source,
            boolean executionMode,
            IDependencyManager dependencyManager) {
        ProcessedCode processedCode = getProcessedCode(source, executionMode, dependencyManager);
        IOpenClass openClass = processedCode.getBoundCode().getTopNode().getType();
        return new CompiledOpenClass(openClass, processedCode.getAllMessages(), processedCode.getMessages());
    }

    private ProcessedCode getProcessedCode(IOpenSourceCodeModule source,
            boolean executionMode,
            IDependencyManager dependencyManager) {
        IBindingContext bindingContext = null;
        if (executionMode) {
            bindingContext = openl.getBinder().makeBindingContext();
            bindingContext.setExecutionMode(true);
        }
        return processSource(source, bindingContext, dependencyManager);
    }

    private static final Comparator<IDependency> COMP = Comparator.comparing(a -> a.getNode().getIdentifier());

    /**
     * Parses and binds source.
     *
     * @param source source
     * @param bindingContext binding context
     * @return processed code descriptor
     */
    private ProcessedCode processSource(IOpenSourceCodeModule source,
            IBindingContext bindingContext,
            IDependencyManager dependencyManager) {

        IParsedCode parsedCode = openl.getParser().parseAsModule(source);

        Collection<OpenLMessage> allMessages = new LinkedHashSet<>();
        Collection<OpenLMessage> messages = new LinkedHashSet<>();

        // compile source dependencies
        Set<ResolvedDependency> dependencies = new LinkedHashSet<>();
        if (dependencyManager != null) {
            Collection<IDependency> allDeps = new LinkedHashSet<>(Arrays.asList(parsedCode.getDependencies()));
            allDeps.addAll(getExternalDependencies(source));
            for (IDependency dependency : allDeps) {
                try {
                    dependencies.addAll(dependencyManager.resolveDependency(dependency, true));
                } catch (OpenLCompilationException e) {
                    allMessages.add(OpenLMessagesUtils.newErrorMessage(e));
                }
            }
        }

        List<ResolvedDependency> sortedResolvedDependencies = new ArrayList<>(dependencies);
        sortedResolvedDependencies.sort(COMP);

        Set<CompiledDependency> compiledDependencies = new LinkedHashSet<>();
        if (CollectionUtils.isNotEmpty(sortedResolvedDependencies)) {
            if (dependencyManager != null) {
                for (ResolvedDependency dependency : sortedResolvedDependencies) {
                    try {
                        CompiledDependency loadedDependency = dependencyManager.loadDependency(dependency);
                        OpenLClassLoader currentClassLoader = (OpenLClassLoader) Thread.currentThread()
                            .getContextClassLoader();
                        ClassLoader dependencyClassLoader = loadedDependency.getClassLoader();
                        if (dependencyClassLoader != currentClassLoader
                                && !(dependencyClassLoader instanceof OpenLClassLoader
                                    && ((OpenLClassLoader) dependencyClassLoader)
                                        .containsClassLoader(currentClassLoader))) {

                            currentClassLoader.addClassLoader(dependencyClassLoader);
                        }
                        compiledDependencies.add(loadedDependency);

                        CompiledOpenClass compiledOpenClass = loadedDependency.getCompiledOpenClass();
                        IOpenClass openClass = compiledOpenClass.getOpenClassWithErrors();
                        if (openClass instanceof ExtendableModuleOpenClass) {
                            ExtendableModuleOpenClass extendableModuleOpenClass = (ExtendableModuleOpenClass) openClass;
                            extendableModuleOpenClass.applyToDependentParsedCode(parsedCode);
                        }

                        // Save
                        // messages
                        // from
                        // dependencies
                        allMessages.addAll(compiledOpenClass.getAllMessages());

                    } catch (Exception e) {
                        allMessages.addAll(OpenLMessagesUtils.newErrorMessages(e));
                    }
                }
            } else {
                allMessages.add(
                    OpenLMessagesUtils.newErrorMessage("Cannot load dependencies. Dependency manager is not defined."));
            }
        }

        parsedCode.setCompiledDependencies(compiledDependencies);

        Map<String, Object> externalParams = source.getParams();

        if (externalParams != null) {
            parsedCode.setExternalParams(externalParams);
            if (externalParams.containsKey(ADDITIONAL_WARN_MESSAGES_KEY)) {
                @SuppressWarnings("unchecked")
                Set<String> warnMessages = (Set<String>) externalParams.get(ADDITIONAL_WARN_MESSAGES_KEY);
                for (String message : warnMessages) {
                    messages.add(OpenLMessagesUtils.newWarnMessage(message));
                }
            }
            if (externalParams.containsKey(ADDITIONAL_ERROR_MESSAGES_KEY)) {
                @SuppressWarnings("unchecked")
                Set<String> errorMessage = (Set<String>) externalParams.get(ADDITIONAL_ERROR_MESSAGES_KEY);
                for (String message : errorMessage) {
                    messages.add(OpenLMessagesUtils.newErrorMessage(message));
                }
            }
        }

        IOpenBinder binder = openl.getBinder();
        if (bindingContext == null) {
            bindingContext = binder.makeBindingContext();
        }
        if (dependencyManager != null) {
            bindingContext = new DependencyBindingContext(bindingContext, dependencyManager);
        }

        // Requires to support java packages. BEX grammar does not support to use binding context to define java
        // packages.
        FullClassnameSupport.transformIdentifierBindersWithBindingContextInfo(bindingContext, parsedCode);

        IBoundCode boundCode = binder.bind(parsedCode, bindingContext);
        allMessages
            .addAll(bindingContext != null && bindingContext.isExecutionMode()
                                                                               ? clearOpenLMessagesForExecutionMode(
                                                                                   boundCode.getMessages())
                                                                               : boundCode.getMessages());

        SyntaxNodeException[] bindingErrors = boundCode.getErrors();

        messages.addAll(OpenLMessagesUtils.newErrorMessages(bindingErrors));
        allMessages.addAll(messages);

        return new ProcessedCode(parsedCode, boundCode, allMessages, messages);
    }

    private Collection<OpenLMessage> clearOpenLMessagesForExecutionMode(Collection<OpenLMessage> messages) {
        // OpenLWarnMessage has a ref to TableSyntaxNode, this is workaround to clean this data in execution mode
        Collection<OpenLMessage> ret = new ArrayList<>();
        for (OpenLMessage message : messages) {
            if (message instanceof OpenLWarnMessage) {
                ret.add(OpenLMessagesUtils.newWarnMessage(message.getSummary()));
            } else {
                ret.add(message);
            }
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    private Collection<IDependency> getExternalDependencies(IOpenSourceCodeModule source) {
        List<IDependency> dependencies = null;
        Map<String, Object> params = source.getParams();
        if (params != null) {
            dependencies = (List<IDependency>) params.get(EXTERNAL_DEPENDENCIES_KEY);

        }
        return dependencies == null ? Collections.emptyList() : dependencies;
    }
}
