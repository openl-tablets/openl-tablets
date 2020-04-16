package org.openl.engine;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.dependency.IDependencyManager;
import org.openl.exception.OpenLRuntimeException;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.CompositeMethod;

/**
 * Helper class that encapsulates several OpenL engine methods.
 *
 */
public final class OpenLManager {

    private OpenLManager() {
    }

    /**
     * Makes open class that describes a type.
     *
     * @param openl OpenL engine context
     * @param source source
     * @param bindingContext binding context
     * @return {@link IOpenClass} instance
     */
    public static IOpenClass makeType(OpenL openl, IOpenSourceCodeModule source, IBindingContext bindingContext) {
        OpenLCodeManager codeManager = new OpenLCodeManager(openl);
        return codeManager.makeType(source, bindingContext);
    }

    /**
     * Makes a method from source using method header descriptor.
     *
     * @param openl OpenL engine context
     * @param source source
     * @param methodHeader method header descriptor
     * @param bindingContext binding context
     * @return {@link CompositeMethod} instance
     */
    public static CompositeMethod makeMethod(OpenL openl,
            IOpenSourceCodeModule source,
            IOpenMethodHeader methodHeader,
            IBindingContext bindingContext) {

        CompositeMethod compositeMethod = new CompositeMethod(methodHeader, null);

        compileMethod(openl, source, compositeMethod, bindingContext);

        return compositeMethod;
    }

    /**
     * Makes a method header from source.
     *
     * @param openl OpenL engine context
     * @param source source
     * @param bindingContext binding context
     * @return {@link IOpenMethodHeader} instance
     */
    public static IOpenMethodHeader makeMethodHeader(OpenL openl,
            IOpenSourceCodeModule source,
            IBindingContext bindingContext) {
        OpenLCodeManager codeManager = new OpenLCodeManager(openl);
        return codeManager.makeMethodHeader(source, bindingContext);
    }

    /**
     * Makes method with unknown return type from source using method name and method signature. This method used to
     * create open class that hasn't information of return type at compile time. Return type can be recognized at
     * runtime time.
     *
     * @param openl OpenL engine context
     * @param source source
     * @param methodName method name
     * @param signature method signature
     * @param declaringClass open class that declare method
     * @param bindingContext binding context
     * @return {@link IOpenMethodHeader} instance
     */
    public static CompositeMethod makeMethodWithUnknownType(OpenL openl,
            IOpenSourceCodeModule source,
            String methodName,
            IMethodSignature signature,
            IOpenClass declaringClass,
            IBindingContext bindingContext) {
        OpenLCodeManager codeManager = new OpenLCodeManager(openl);
        return codeManager.makeMethodWithUnknownType(source, methodName, signature, declaringClass, bindingContext);
    }

    /**
     * Compiles a method and sets meta info to the cells.
     *
     * @param openl OpenL engine context
     * @param source method source
     * @param compositeMethod {@link CompositeMethod} instance
     * @param bindingContext binding context
     */
    public static void compileMethod(OpenL openl,
            IOpenSourceCodeModule source,
            CompositeMethod compositeMethod,
            IBindingContext bindingContext) {

        OpenLCodeManager codeManager = new OpenLCodeManager(openl);

        codeManager.compileMethod(source, compositeMethod, bindingContext);
    }

    public static CompiledOpenClass compileModuleWithErrors(OpenL openl,
            IOpenSourceCodeModule source,
            boolean executionMode,
            IDependencyManager dependencyManager) {
        OpenLCompileManager compileManager = new OpenLCompileManager(openl);
        try {
            return compileManager.compileModuleWithErrors(source, executionMode, dependencyManager);
        } finally {
            if (dependencyManager != null) {
                dependencyManager.clearOddDataForExecutionMode();
            }
        }
    }

    /**
     * Compiles and runs OpenL script.
     *
     * @param openl OpenL engine context
     * @param source source
     * @return result of script execution
     * @throws OpenLRuntimeException
     */
    public static Object run(OpenL openl, IOpenSourceCodeModule source) {
        OpenLRunManager runManager = new OpenLRunManager(openl);
        return runManager.run(source);
    }

    /**
     * Compiles and runs specified method.
     *
     * @param openl OpenL engine context
     * @param source source
     * @param methodName method name
     * @param paramTypes parameters types
     * @param params parameters values
     * @return result of method execution
     */
    public static Object runMethod(OpenL openl,
            IOpenSourceCodeModule source,
            String methodName,
            IOpenClass[] paramTypes,
            Object[] params) {
        OpenLRunManager runManager = new OpenLRunManager(openl);
        return runManager.runMethod(source, methodName, paramTypes, params);
    }
}
