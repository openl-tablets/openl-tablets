package org.openl.engine;

import org.openl.CompiledOpenClass;
import org.openl.IOpenBinder;
import org.openl.IOpenVM;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundCode;
import org.openl.binding.IBoundMethodHeader;
import org.openl.binding.IBoundMethodNode;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.ANodeBinder;
import org.openl.binding.impl.MethodCastNode;
import org.openl.binding.impl.TypeCastException;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.module.MethodBindingContext;
import org.openl.dependency.IDependencyManager;
import org.openl.exception.OpenLCompilationException;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.exception.CompositeOpenlException;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.java.JavaOpenClass;

/**
 * Helper class that encapsulates several OpenL engine methods.
 *
 */
public final class OpenLManager {

    private OpenLManager() {
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
            IBindingContext bindingContext) throws OpenLCompilationException {
        IParsedCode parsedCode = openl.getParser().parseAsMethodHeader(source);
        IBoundNode topNode = getBoundNode(openl, bindingContext, parsedCode);
        if (topNode instanceof IBoundMethodHeader) {
            return ((IBoundMethodHeader) topNode).getMethodHeader();
        } else {
            throw new OpenLCompilationException("Invalid method header.", null, null, source);
        }
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

        OpenMethodHeader header = new OpenMethodHeader(methodName, NullOpenClass.the, signature, declaringClass);
        CompositeMethod compositeMethod = new CompositeMethod(header, null);

        MethodBindingContext methodBindingContext = new MethodBindingContext(header, bindingContext);
        IParsedCode parsedCode = openl.getParser().parseAsMethodBody(source);
        IBoundNode topNode = getBoundNode(openl, methodBindingContext, parsedCode);

        IOpenClass retType = methodBindingContext.getReturnType();
        if (retType == NullOpenClass.the) {
            retType = topNode.getType();
        }

        header.setTypeClass(retType);

        if (topNode instanceof IBoundMethodNode) {
            IBoundMethodNode boundMethodNode = bindMethod((IBoundMethodNode) topNode, header, bindingContext);
            compositeMethod.setMethodBodyBoundNode(boundMethodNode);
        }

        return compositeMethod;
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

        IOpenMethodHeader header = compositeMethod.getHeader();
        MethodBindingContext methodBindingContext = new MethodBindingContext(header, bindingContext);
        IParsedCode parsedCode = openl.getParser().parseAsMethodBody(source);
        IBoundNode topNode = getBoundNode(openl, methodBindingContext, parsedCode);

        if (topNode instanceof IBoundMethodNode) {
            IBoundMethodNode boundMethodNode = bindMethod((IBoundMethodNode) topNode, header, bindingContext);
            compositeMethod.setMethodBodyBoundNode(boundMethodNode);
        }
    }

    public static CompiledOpenClass compileModuleWithErrors(OpenL openl,
            IOpenSourceCodeModule source,
            boolean executionMode,
            IDependencyManager dependencyManager) {
        OpenLCompileManager compileManager = new OpenLCompileManager(openl);
        return compileManager.compileModuleWithErrors(source, executionMode, dependencyManager);
    }

    /**
     * Compiles and runs OpenL script.
     *
     * @param openl OpenL engine context
     * @param source source
     * @return result of script execution
     * @throws CompositeOpenlException if errors in the source code
     */
    public static Object run(OpenL openl, IOpenSourceCodeModule source) throws CompositeOpenlException {

        IParsedCode parsedCode = openl.getParser().parseAsMethodBody(source);
        SyntaxNodeException[] parsingErrors = parsedCode.getErrors();
        if (parsingErrors.length > 0) {
            throw new CompositeOpenlException("Parsing Error:", parsingErrors, null);
        }
        IOpenBinder binder = openl.getBinder();
        IBoundCode boundCode = binder.bind(parsedCode, null);
        SyntaxNodeException[] bindingErrors = boundCode.getErrors();

        if (bindingErrors.length > 0) {
            throw new CompositeOpenlException("Binding Error:", bindingErrors, boundCode.getMessages());
        }

        IBoundNode boundNode = boundCode.getTopNode();

        IOpenVM vm = openl.getVm();

        return vm.getRunner().run((IBoundMethodNode) boundNode, new Object[0]);
    }

    /**
     * Binds method which defines by header descriptor.
     *
     * @param boundMethodNode method bound node
     * @param header method header descriptor
     * @param bindingContext binding context
     * @return node of bound code that contains information about method
     */
    private static IBoundMethodNode bindMethod(IBoundMethodNode boundMethodNode,
            IOpenMethodHeader header,
            IBindingContext bindingContext) {

        IOpenClass type = header.getType();

        if (type != JavaOpenClass.VOID && type != NullOpenClass.the) {

            IOpenCast cast = null;
            try {
                cast = ANodeBinder.getCast(boundMethodNode, type, bindingContext);
            } catch (TypeCastException e) {
                bindingContext.addError(e);
            }

            if (cast != null) {
                boundMethodNode = new MethodCastNode(boundMethodNode, cast, type);
            }
        }
        return boundMethodNode;
    }

    private static IBoundNode getBoundNode(OpenL openl, IBindingContext bindingContext, IParsedCode parsedCode) {
        SyntaxNodeException[] parsingErrors = parsedCode.getErrors();
        if (parsingErrors.length > 0) {
            for (SyntaxNodeException parsingError : parsingErrors) {
                bindingContext.addError(parsingError);
            }
        }

        FullClassnameSupport.transformIdentifierBindersWithBindingContextInfo(bindingContext, parsedCode);

        IOpenBinder binder = openl.getBinder();
        IBoundCode boundCode = binder.bind(parsedCode, bindingContext);

        return boundCode.getTopNode();
    }
}
