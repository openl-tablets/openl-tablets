package org.openl.engine;

import org.openl.IOpenBinder;
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
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.StringUtils;

/**
 * Class that defines OpenL engine manager implementation for operations with code such as make type, make method and
 * etc.
 *
 */
public class OpenLCodeManager {

    private OpenL openl;

    /**
     * Default constructor.
     *
     * @param openl {@link OpenL} instance
     */
    public OpenLCodeManager(OpenL openl) {
        this.openl = openl;
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

        try {
            IOpenClass type = header.getType();

            if (type != JavaOpenClass.VOID && type != NullOpenClass.the) {

                IOpenCast cast = ANodeBinder.getCast(boundMethodNode, type, bindingContext);

                if (cast != null) {
                    boundMethodNode = new MethodCastNode(boundMethodNode, cast, type);
                }
            }
            return boundMethodNode;
        } catch (TypeCastException ex) {
            throw new CompositeSyntaxNodeException(StringUtils.EMPTY, new SyntaxNodeException[] { ex });
        }

    }

    /**
     * Makes open class that describes a type.
     *
     * @param source source
     * @param bindingContext binding context
     * @return {@link IOpenClass} instance
     */
    public IOpenClass makeType(IOpenSourceCodeModule source, IBindingContext bindingContext) {
        IParsedCode parsedCode = openl.getParser().parseAsType(source);
        IBoundNode topNode = getBoundNode(openl, bindingContext, parsedCode);
        return topNode.getType();
    }

    /**
     * Makes a method header from source.
     *
     * @param source source
     * @param bindingContext binding context
     * @return {@link IOpenMethodHeader} instance
     */
    public IOpenMethodHeader makeMethodHeader(IOpenSourceCodeModule source, IBindingContext bindingContext) {
        IParsedCode parsedCode = openl.getParser().parseAsMethodHeader(source);
        IBoundNode topNode = getBoundNode(openl, bindingContext, parsedCode);
        return ((IBoundMethodHeader) topNode).getMethodHeader();
    }

    private static IBoundNode getBoundNode(OpenL openl, IBindingContext bindingContext, IParsedCode parsedCode) {
        try {
            bindingContext.pushErrors();

            SyntaxNodeException[] parsingErrors = parsedCode.getErrors();
            if (parsingErrors.length > 0) {
                throw new CompositeSyntaxNodeException("Parsing Error:", parsingErrors);
            }

            FullClassnameSupport.transformIdentifierBindersWithBindingContextInfo(bindingContext, parsedCode);

            IOpenBinder binder = openl.getBinder();
            IBoundCode boundCode = binder.bind(parsedCode, bindingContext);

            SyntaxNodeException[] bindingErrors = boundCode.getErrors();
            if (bindingErrors.length > 0) {
                throw new CompositeSyntaxNodeException("Binding Error:", bindingErrors);
            }

            return boundCode.getTopNode();

        } finally {
            bindingContext.popErrors();
        }
    }

    /**
     * Makes method with unknown return type from source using method name and method signature. This method used to
     * create open class that hasn't information of return type at compile time. Return type can be recognized at
     * runtime time.
     *
     * @param source source
     * @param methodName method name
     * @param signature method signature
     * @param declaringClass open class that declare method
     * @param bindingContext binding context
     * @return {@link IOpenMethodHeader} instance
     */
    public CompositeMethod makeMethodWithUnknownType(IOpenSourceCodeModule source,
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

        IBoundMethodNode boundMethodNode = bindMethod((IBoundMethodNode) topNode, header, bindingContext);
        compositeMethod.setMethodBodyBoundNode(boundMethodNode);

        return compositeMethod;
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

        IOpenMethodHeader header = compositeMethod.getHeader();
        MethodBindingContext methodBindingContext = new MethodBindingContext(header, bindingContext);
        IParsedCode parsedCode = openl.getParser().parseAsMethodBody(source);
        IBoundNode topNode = getBoundNode(openl, methodBindingContext, parsedCode);

        IBoundMethodNode boundMethodNode = bindMethod((IBoundMethodNode) topNode, header, bindingContext);
        compositeMethod.setMethodBodyBoundNode(boundMethodNode);
    }
}
