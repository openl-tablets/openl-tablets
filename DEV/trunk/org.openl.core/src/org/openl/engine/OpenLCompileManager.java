package org.openl.engine;

import java.util.List;

import org.openl.CompiledOpenClass;
import org.openl.IOpenSourceCodeModule;
import org.openl.OpenL;
import org.openl.ProcessedCode;
import org.openl.SourceType;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundCode;
import org.openl.binding.IBoundMethodNode;
import org.openl.binding.impl.module.MethodBindingContext;
import org.openl.message.OpenLMessage;
import org.openl.syntax.ISyntaxError;
import org.openl.types.IOpenClass;
import org.openl.types.impl.CompositeMethod;
import org.openl.validation.ValidationResult;
import org.openl.validation.ValidationUtils;

/**
 * Class that defines OpenL engine manager implementation for compilation
 * operations.
 * 
 */
public class OpenLCompileManager extends OpenLHolder {

    private OpenLSourceManager sourceManager;
    private OpenLBindManager bindManager;
    private OpenLValidationManager validationManager;

    /**
     * Construct new instance of manager.
     * 
     * @param openl {@link OpenL} instance
     */
    public OpenLCompileManager(OpenL openl) {
        super(openl);
        sourceManager = new OpenLSourceManager(openl);
        bindManager = new OpenLBindManager(openl);
        validationManager = new OpenLValidationManager(openl);
    }

    /**
     * Compiles module. As a result a module open class will be returned by
     * engine.
     * 
     * @param source source
     * @return {@link IOpenClass} instance
     */
    public IOpenClass compileModule(IOpenSourceCodeModule source) {

        ProcessedCode processedCode = sourceManager.processSource(source, SourceType.MODULE);

        return processedCode.getBoundCode().getTopNode().getType();
    }

    /**
     * Compiles module. As a result a module open class will be returned by
     * engine. All errors that occurred during compilation are suppressed.
     * 
     * @param source source
     * @return {@link CompiledOpenClass} instance
     */
    public CompiledOpenClass compileModuleWithErrors(IOpenSourceCodeModule source) {

        ProcessedCode processedCode = sourceManager.processSource(source, SourceType.MODULE, null, true);

        IOpenClass openClass = processedCode.getBoundCode().getTopNode().getType();
        ISyntaxError[] parsingErrors = processedCode.getParsingErrors();
        ISyntaxError[] bindingErrors = processedCode.getBindingErrors();
        
        List<ValidationResult> validationResults = validationManager.validate(openClass);
        List<OpenLMessage> validationMessages = ValidationUtils.getValidationMessages(validationResults);
        
        getOpenL().addMessages(validationMessages);

        return new CompiledOpenClass(openClass, parsingErrors, bindingErrors);
    }

    /**
     * Compiles a method.
     * 
     * @param source method source
     * @param compositeMethod {@link CompositeMethod} instance
     * @param bindingContext binding context
     */
    public void compileMethod(IOpenSourceCodeModule source, CompositeMethod compositeMethod,
            IBindingContext bindingContext) {

        try {

            bindingContext.pushErrors();

            MethodBindingContext methodBindingContext = new MethodBindingContext(compositeMethod.getHeader(),
                    bindingContext);

            ProcessedCode processedCode = sourceManager.processSource(source, SourceType.METHOD_BODY,
                    methodBindingContext, false);

            IBoundCode boundCode = processedCode.getBoundCode();

            IBoundMethodNode boundMethodNode = bindManager.bindMethod(boundCode, compositeMethod.getHeader(),
                    bindingContext);

            compositeMethod.setMethodBodyBoundNode(boundMethodNode);

        } finally {
            bindingContext.popErrors();
        }
    }
}
