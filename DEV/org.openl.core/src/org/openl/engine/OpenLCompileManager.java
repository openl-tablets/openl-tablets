package org.openl.engine;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundCode;
import org.openl.binding.IBoundMethodNode;
import org.openl.binding.impl.ANodeBinder;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.binding.impl.module.MethodBindingContext;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.message.OpenLMessage;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.SourceType;
import org.openl.syntax.code.ProcessedCode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenClass;
import org.openl.types.impl.CompositeMethod;
import org.openl.validation.ValidationResult;

/**
 * Class that defines OpenL engine manager implementation for compilation operations.
 * 
 */
public class OpenLCompileManager extends OpenLHolder {

    private OpenLSourceManager sourceManager;
    private OpenLValidationManager validationManager;

    /**
     * Construct new instance of manager.
     * 
     * @param openl {@link OpenL} instance
     */
    public OpenLCompileManager(OpenL openl) {
        super(openl);
        sourceManager = new OpenLSourceManager(openl);
        validationManager = new OpenLValidationManager(openl);
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
        ProcessedCode processedCode;
        if (executionMode) {
            IBindingContext bindingContext = sourceManager.getOpenL().getBinder().makeBindingContext();
            bindingContext.setExecutionMode(true);
            processedCode = sourceManager.processSource(source, SourceType.MODULE, bindingContext, false, dependencyManager);
        } else {
            processedCode = sourceManager.processSource(source, SourceType.MODULE, dependencyManager);
        }

        IOpenClass openClass = processedCode.getBoundCode().getTopNode().getType();
        if (executionMode) {
            ((ModuleOpenClass) openClass).clearOddDataForExecutionMode();
        }

        return openClass;
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
        ProcessedCode processedCode; 
        if (executionMode) {
            IBindingContext bindingContext = sourceManager.getOpenL().getBinder().makeBindingContext();
            bindingContext.setExecutionMode(true);
            processedCode = sourceManager.processSource(source, SourceType.MODULE, bindingContext, true, dependencyManager);
        } else {
            processedCode = sourceManager.processSource(source, SourceType.MODULE, null, true, dependencyManager);
        }
        IOpenClass openClass = processedCode.getBoundCode().getTopNode().getType();
        SyntaxNodeException[] parsingErrors = processedCode.getParsingErrors();
        SyntaxNodeException[] bindingErrors = processedCode.getBindingErrors();
        Collection<OpenLMessage> messages = new LinkedHashSet<>();
        if (!executionMode) {
            // for WebStudio
            List<ValidationResult> validationResults = validationManager.validate(openClass);

            for (ValidationResult result : validationResults) {
                messages.addAll(result.getMessages());
            }
        }

        messages.addAll(processedCode.getMessages());

        if (executionMode && openClass instanceof ComponentOpenClass) {
            ((ComponentOpenClass) openClass).clearOddDataForExecutionMode();
        }

        return new CompiledOpenClass(openClass, messages, parsingErrors, bindingErrors);
    }

    /**
     * Compiles a method.
     * 
     * @param source method source
     * @param compositeMethod {@link CompositeMethod} instance
     * @param bindingContext binding context
     */
    public void compileMethod(IOpenSourceCodeModule source,
            CompositeMethod compositeMethod,
            IBindingContext bindingContext) {

        try {

            bindingContext.pushErrors();

            MethodBindingContext methodBindingContext = new MethodBindingContext(compositeMethod.getHeader(),
                bindingContext);

            ProcessedCode processedCode = sourceManager
                .processSource(source, SourceType.METHOD_BODY, methodBindingContext, false, null);

            IBoundCode boundCode = processedCode.getBoundCode();

            IBoundMethodNode boundMethodNode = ANodeBinder
                .bindMethod(boundCode, compositeMethod.getHeader(), bindingContext);

            compositeMethod.setMethodBodyBoundNode(boundMethodNode);
        } finally {
            bindingContext.popErrors();
        }
    }
}
