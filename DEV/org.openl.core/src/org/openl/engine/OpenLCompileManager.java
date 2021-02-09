package org.openl.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.openl.CompiledOpenClass;
import org.openl.IOpenBinder;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundCode;
import org.openl.classloader.OpenLClassLoader;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.IDependencyManager;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.OpenLWarnMessage;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.code.ProcessedCode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IModuleInfo;
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
    private static final Pattern ASTERIX_SIGN = Pattern.compile("\\*");
    private static final Pattern QUESTION_SIGN = Pattern.compile("\\?");
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
        return new CompiledOpenClass(openClass, processedCode.getMessages());
    }

    private ProcessedCode getProcessedCode(IOpenSourceCodeModule source,
            boolean executionMode,
            IDependencyManager dependencyManager) {
        ProcessedCode processedCode;
        IBindingContext bindingContext = null;
        if (executionMode) {
            bindingContext = openl.getBinder().makeBindingContext();
            bindingContext.setExecutionMode(true);
        }
        processedCode = processSource(source, bindingContext, dependencyManager);
        return processedCode;
    }

    private Collection<IDependency> getDependencies(IOpenSourceCodeModule source,
            IDependencyManager dependencyManager,
            IDependency[] dependencies) {
        Set<IDependency> resolvedDependencies = new HashSet<>();
        if (dependencyManager == null) {
            return Arrays.asList(dependencies);
        }
        Set<String> dependencyNames;
        Collection<String> deps = dependencyManager.getAvailableDependencies();
        if (deps.isEmpty()) {
            return Arrays.asList(dependencies);
        } else {
            dependencyNames = new HashSet<>(deps);
        }
        String currentModule = null;
        if (source instanceof IModuleInfo) {
            currentModule = ((IModuleInfo) source).getModuleName();
        }
        for (IDependency dependency : dependencies) {
            String value = dependency.getNode().getIdentifier();
            value = ASTERIX_SIGN.matcher(value).replaceAll("\\\\E.*\\\\Q");
            value = QUESTION_SIGN.matcher(value).replaceAll("\\\\E.\\\\Q");
            value = "\\Q" + value + "\\E";

            boolean found = false;
            for (String dependencyName : dependencyNames) {
                if (!Objects.equals(currentModule, dependencyName) && Pattern.matches(value, dependencyName)) {
                    found = true;
                    resolvedDependencies.add(new Dependency(dependency.getType(),
                        new IdentifierNode(dependency.getNode().getType(), null, dependencyName, null)));
                }
            }

            if (!found) {
                // Needed to create error message "Dependency wasn't found" later
                resolvedDependencies.add(dependency);
            }
        }
        return resolvedDependencies;
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

        Collection<OpenLMessage> messages = new LinkedHashSet<>();

        // compile source dependencies

        Collection<IDependency> dependencyManagerDependencies = getDependencies(source,
            dependencyManager,
            parsedCode.getDependencies());
        Collection<IDependency> externalDependencies = getExternalDependencies(source);

        List<IDependency> dependencies = new ArrayList<>(dependencyManagerDependencies);
        dependencies.addAll(externalDependencies);

        dependencies.sort(COMP);

        Set<CompiledDependency> compiledDependencies = new LinkedHashSet<>();
        if (CollectionUtils.isNotEmpty(dependencies)) {
            if (dependencyManager != null) {
                for (IDependency dependency : dependencies) {
                    try {
                        CompiledDependency loadedDependency = dependencyManager.loadDependency(dependency);
                        OpenLClassLoader currentClassLoader = (OpenLClassLoader) Thread.currentThread()
                            .getContextClassLoader();
                        if (loadedDependency.getClassLoader() != currentClassLoader) {
                            currentClassLoader.addClassLoader(loadedDependency.getClassLoader());
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
                        messages.addAll(compiledOpenClass.getMessages());

                    } catch (Exception e) {
                        messages.addAll(OpenLMessagesUtils.newErrorMessages(e));
                    }
                }
            } else {
                messages.add(
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

        // Requires to support java packages. BEX grammar does not support to use binding context to define java
        // packages.
        FullClassnameSupport.transformIdentifierBindersWithBindingContextInfo(bindingContext, parsedCode);

        IOpenBinder binder = openl.getBinder();
        IBoundCode boundCode = binder.bind(parsedCode, bindingContext);
        messages.addAll(bindingContext != null && bindingContext.isExecutionMode()
                                                                                   ? clearOpenLMessagesForExecutionMode(
                                                                                       boundCode.getMessages())
                                                                                   : boundCode.getMessages());

        SyntaxNodeException[] bindingErrors = boundCode.getErrors();

        messages.addAll(OpenLMessagesUtils.newErrorMessages(bindingErrors));

        ProcessedCode processedCode = new ProcessedCode();
        processedCode.setParsedCode(parsedCode);
        processedCode.setBoundCode(boundCode);
        processedCode.setMessages(messages);

        return processedCode;
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
