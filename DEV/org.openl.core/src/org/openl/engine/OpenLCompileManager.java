package org.openl.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.openl.CompiledOpenClass;
import org.openl.ICompileContext;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.dependency.IDependencyManager;
import org.openl.message.OpenLMessage;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.code.ProcessedCode;
import org.openl.types.IOpenClass;
import org.openl.validation.IOpenLValidator;
import org.openl.validation.ValidationResult;

/**
 * Class that defines OpenL engine manager implementation for compilation operations.
 *
 */
public class OpenLCompileManager {

    private static ThreadLocal<Boolean> validationEnabled = new ThreadLocal<>(); // Workaroung
    private OpenLSourceManager sourceManager;
    private OpenL openl;

    /**
     * Construct new instance of manager.
     *
     * @param openl {@link OpenL} instance
     */
    public OpenLCompileManager(OpenL openl) {
        this.openl = openl;
        sourceManager = new OpenLSourceManager(openl);
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
        processedCode = sourceManager
            .processSource(source, bindingContext, ignoreErrors, dependencyManager);
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
}
