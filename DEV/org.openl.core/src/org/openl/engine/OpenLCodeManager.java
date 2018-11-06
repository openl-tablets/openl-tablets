package org.openl.engine;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundCode;
import org.openl.binding.IBoundMethodHeader;
import org.openl.binding.IBoundMethodNode;
import org.openl.binding.impl.ANodeBinder;
import org.openl.binding.impl.module.MethodBindingContext;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.SourceType;
import org.openl.syntax.code.ProcessedCode;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.OpenMethodHeader;

/**
 * Class that defines OpenL engine manager implementation for operations with
 * code such as make type, make method and etc.
 * 
 */
public class OpenLCodeManager extends OpenLHolder {

    private OpenLSourceManager sourceManager;
    private OpenLCompileManager compileManager;

    /**
     * Default constructor.
     * 
     * @param openl {@link OpenL} instance
     */
    public OpenLCodeManager(OpenL openl) {
        super(openl);
        sourceManager = new OpenLSourceManager(openl);
        compileManager = new OpenLCompileManager(openl);
    }

    /**
     * Makes open class that describes a type.
     * 
     * @param source source
     * @param bindingContext binding context
     * @return {@link IOpenClass} instance
     */
    public IOpenClass makeType(IOpenSourceCodeModule source, IBindingContext bindingContext) {
        try {
            if (bindingContext != null) {
                bindingContext.pushErrors();
            }

            ProcessedCode processedCode = sourceManager.processSource(source, SourceType.TYPE, bindingContext,
                    false, null);

            IBoundCode boundCode = processedCode.getBoundCode();

            return boundCode.getTopNode().getType();

        } finally {
            if (bindingContext != null) {
                bindingContext.popErrors();
            }
        }
    }

    /**
     * Makes a method from source using method header descriptor.
     * 
     * @param source source
     * @param methodHeader method header descriptor
     * @param bindingContext binding context
     * @return {@link CompositeMethod} instance
     */
    public CompositeMethod makeMethod(IOpenSourceCodeModule source, IOpenMethodHeader methodHeader,
            IBindingContext bindingContext) {

        CompositeMethod compositeMethod = new CompositeMethod(methodHeader, null);

        compileManager.compileMethod(source, compositeMethod, bindingContext);

        return compositeMethod;
    }

    /**
     * Makes a method header from source.
     * 
     * @param source source
     * @param bindingContext binding context
     * @return {@link IOpenMethodHeader} instance
     */
    public IOpenMethodHeader makeMethodHeader(IOpenSourceCodeModule source, IBindingContext bindingContext) {
        try {
            if (bindingContext != null) {
                bindingContext.pushErrors();
            }

            ProcessedCode processedCode = sourceManager.processSource(source, SourceType.METHOD_HEADER,
                bindingContext, false, null);

            IBoundCode boundCode = processedCode.getBoundCode();

            return ((IBoundMethodHeader) boundCode.getTopNode()).getMethodHeader();

        } finally {
            if (bindingContext != null) {
                bindingContext.popErrors();
            }
        }
    }

    /**
     * Makes method with unknown return type from source using method name and
     * method signature. This method used to create open class that hasn't
     * information of return type at compile time. Return type can be recognized
     * at runtime time.
     * 
     * @param source source
     * @param methodName method name
     * @param signature method signature
     * @param declaringClass open class that declare method
     * @param bindingContext binding context
     * @return {@link IOpenMethodHeader} instance
     */
    public CompositeMethod makeMethodWithUnknownType(IOpenSourceCodeModule source, String methodName,
            IMethodSignature signature, IOpenClass declaringClass, IBindingContext bindingContext) {

        OpenMethodHeader header = new OpenMethodHeader(methodName, NullOpenClass.the, signature, declaringClass);

        try {
            bindingContext.pushErrors();

            MethodBindingContext methodBindingContext = new MethodBindingContext(header, bindingContext);

            ProcessedCode processedCode = sourceManager.processSource(source, SourceType.METHOD_BODY,
                    methodBindingContext, false, null);

            IBoundCode boundCode = processedCode.getBoundCode();

            IOpenClass retType = methodBindingContext.getReturnType();

            if (retType == NullOpenClass.the) {
                retType = boundCode.getTopNode().getType();
            }

            header.setTypeClass(retType);

            IBoundMethodNode boundMethodNode = ANodeBinder.bindMethod(boundCode, header, bindingContext);
            
            if (bindingContext.getErrors().length > 0) {
                throw new CompositeSyntaxNodeException("Parsing Error:", bindingContext.getErrors());
            }
            
            return new CompositeMethod(header, boundMethodNode);
        } finally {
            bindingContext.popErrors();
        }
    }

}
