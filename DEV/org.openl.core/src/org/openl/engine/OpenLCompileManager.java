package org.openl.engine;

import java.util.*;
import java.util.regex.Pattern;

import org.openl.CompiledOpenClass;
import org.openl.ICompileContext;
import org.openl.IOpenBinder;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundCode;
import org.openl.classloader.OpenLBundleClassLoader;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.IDependencyManager;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.code.ProcessedCode;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.util.CollectionUtils;
import org.openl.validation.IOpenLValidator;
import org.openl.validation.ValidationResult;

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

    private static ThreadLocal<Boolean> validationEnabled = new ThreadLocal<>(); // Workaroung
    private OpenL openl;

    /**
     * Construct new instance of manager.
     *
     * @param openl {@link OpenL} instance
     */
    public OpenLCompileManager(OpenL openl) {
        this.openl = openl;
    }

    public static boolean isValidationEnabled() {
        Boolean validationIsOn = validationEnabled.get();
        return validationIsOn == null || validationIsOn;
    }

    public static void turnOffValidation() {
        validationEnabled.set(Boolean.FALSE);
    }

    public static void turnOnValidation() {
        validationEnabled.remove();
    }

    /**
     * Compiles module. As a result a module open class will be returned by engine.
     *
     * @param source source
     * @param executionMode <code>true</code> if module should be compiled in memory optimized mode for only execution
     * @return {@link IOpenClass} instance
     */
    public IOpenClass compileModule(IOpenSourceCodeModule source,
            boolean executionMode,
            IDependencyManager dependencyManager) {
        ProcessedCode processedCode = getProcessedCode(source, executionMode, dependencyManager, false);

        return processedCode.getBoundCode().getTopNode().getType();
    }

    /**
     * Compiles module. As a result a module open class will be returned by engine. All errors that occurred during
     * compilation are suppressed.
     *
     * @param source source
     * @param executionMode <code>true</code> if module should be compiled in memory optimized mode for only execution
     * @return {@link CompiledOpenClass} instance
     */
    public CompiledOpenClass compileModuleWithErrors(IOpenSourceCodeModule source,
            boolean executionMode,
            IDependencyManager dependencyManager) {
        ProcessedCode processedCode = getProcessedCode(source, executionMode, dependencyManager, true);
        IOpenClass openClass = processedCode.getBoundCode().getTopNode().getType();
        Collection<OpenLMessage> messages = new LinkedHashSet<>();
        if (!executionMode) {
            // for WebStudio
            List<ValidationResult> validationResults = validate(openClass);
            for (ValidationResult result : validationResults) {
                messages.addAll(result.getMessages());
            }
        }

        messages.addAll(processedCode.getMessages());

        return new CompiledOpenClass(openClass, messages);
    }

    private ProcessedCode getProcessedCode(IOpenSourceCodeModule source,
            boolean executionMode,
            IDependencyManager dependencyManager,
            boolean ignoreErrors) {
        ProcessedCode processedCode;
        IBindingContext bindingContext = null;
        if (executionMode) {
            bindingContext = openl.getBinder().makeBindingContext();
            bindingContext.setExecutionMode(true);
        }
        processedCode = processSource(source, bindingContext, ignoreErrors, dependencyManager);
        return processedCode;
    }

    /**
     * Invokes validation process for each registered validator.
     *
     * @param openClass openClass to validate
     * @return list of validation results
     */
    private List<ValidationResult> validate(IOpenClass openClass) {
        if (OpenLCompileManager.isValidationEnabled()) {
            List<ValidationResult> results = new ArrayList<>();

            ICompileContext context = openl.getCompileContext();

            // Check that compile context initialized. If context is null or
            // validation switched off then skip validation process.
            //
            if (context != null) {

                Set<IOpenLValidator> validators = context.getValidators();

                for (IOpenLValidator validator : validators) {

                    ValidationResult result = validator.validate(openl, openClass);

                    results.add(result);
                }

            }
            return results;
        } else {
            return Collections.emptyList();
        }
    }

    private Collection<IDependency> getDependencies(IDependencyManager dependencyManager, IDependency[] dependencies) {
        Set<IDependency> result = new HashSet<>();
        if (dependencyManager == null) {
            return Arrays.asList(dependencies);
        }
        Set<String> dependencyNames;
        Collection<String> deps = dependencyManager.getAllDependencies();
        if (deps.isEmpty()) {
            return Arrays.asList(dependencies);
        } else {
            dependencyNames = new HashSet<>(deps);
        }
        for (IDependency dependency : dependencies) {
            String value = dependency.getNode().getIdentifier();
            value = ASTERIX_SIGN.matcher(value).replaceAll("\\\\E.*\\\\Q");
            value = QUESTION_SIGN.matcher(value).replaceAll("\\\\E.\\\\Q");
            value = "\\Q" + value + "\\E";

            boolean found = false;
            for (String dependencyName : dependencyNames) {
                if (Pattern.matches(value, dependencyName)) {
                    found = true;
                    result.add(new Dependency(dependency.getType(),
                        new IdentifierNode(dependency.getNode().getType(), null, dependencyName, null)));
                }
            }

            if (!found) {
                // Needed to create error message "Dependency wasn't found" later
                result.add(dependency);
            }

        }
        return result;
    }

    private static final Comparator<IDependency> COMP = Comparator.comparing(a -> a.getNode().getIdentifier());

    /**
     * Parses and binds source.
     *
     * @param source source
     * @param bindingContext binding context
     * @param ignoreErrors define a flag that indicates to suppress errors or break source processing when an error has
     *            occurred
     * @return processed code descriptor
     */
    private ProcessedCode processSource(IOpenSourceCodeModule source,
            IBindingContext bindingContext,
            boolean ignoreErrors,
            IDependencyManager dependencyManager) {

        IParsedCode parsedCode = openl.getParser().parseAsModule(source);
        SyntaxNodeException[] parsingErrors = parsedCode.getErrors();

        if (!ignoreErrors && parsingErrors.length > 0) {
            throw new CompositeSyntaxNodeException("Parsing Error:", parsingErrors);
        }

        Collection<OpenLMessage> messages = new LinkedHashSet<>();

        // compile source dependencies

        Collection<IDependency> dependencyManagerDependencies = getDependencies(dependencyManager,
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
                        OpenLBundleClassLoader currentClassLoader = (OpenLBundleClassLoader) Thread.currentThread()
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
        messages.addAll(boundCode.getMessages());

        SyntaxNodeException[] bindingErrors = boundCode.getErrors();

        if (!ignoreErrors && bindingErrors.length > 0) {
            throw new CompositeSyntaxNodeException("Binding Error:", bindingErrors);
        } else {
            messages.addAll(OpenLMessagesUtils.newErrorMessages(bindingErrors));
        }

        ProcessedCode processedCode = new ProcessedCode();
        processedCode.setParsedCode(parsedCode);
        processedCode.setBoundCode(boundCode);
        processedCode.setMessages(messages);

        return processedCode;
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
