package org.openl.engine;

import org.openl.IOpenBinder;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundCode;
import org.openl.binding.IBoundMethodNode;
import org.openl.binding.impl.ANodeBinder;
import org.openl.binding.impl.MethodCastNode;
import org.openl.binding.impl.TypeCastException;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.NullOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.StringUtils;

/**
 * Class that defines OpenL engine manager implementation for binding operations.
 * 
 */
public class OpenLBindManager extends OpenLHolder {

    /**
     * Construct new instance of manager.
     * 
     * @param openl {@link OpenL} instance
     */
    public OpenLBindManager(OpenL openl) {
        super(openl);
    }

    /**
     * Binds parsed code.
     * 
     * @param bindingContextDelegator binding context
     * @param parsedCode parsed code
     * @return bound code
     */
    public IBoundCode bindCode(IBindingContext bindingContext, IParsedCode parsedCode) {

        IOpenBinder binder = getOpenL().getBinder();

        if (bindingContext == null) {
            return binder.bind(parsedCode);
        }

        return binder.bind(parsedCode, bindingContext);
    }

    /**
     * Binds method which defines by header descriptor.
     * 
     * @param boundCode bound code that contains method bound code
     * @param header method header descriptor
     * @param bindingContext binding context
     * @return node of bound code that contains information about method
     */
    public IBoundMethodNode bindMethod(IBoundCode boundCode, IOpenMethodHeader header, IBindingContext bindingContext) {

        IBoundMethodNode boundMethodNode = null;

        try {
            boundMethodNode = bindMethodType((IBoundMethodNode) boundCode.getTopNode(),
                    bindingContext, header.getType());
        } catch (TypeCastException ex) {
            throw new CompositeSyntaxNodeException(StringUtils.EMPTY, new SyntaxNodeException[] { ex });
        }

        return boundMethodNode;
    }

    private IBoundMethodNode bindMethodType(IBoundMethodNode boundMethodNode,
                                            IBindingContext bindingContext,
                                            IOpenClass type) throws TypeCastException {

        if (type == JavaOpenClass.VOID || type == NullOpenClass.the) {
            return boundMethodNode;
        }

        IOpenCast cast = ANodeBinder.getCast(boundMethodNode, type, bindingContext);

        if (cast == null) {
            return boundMethodNode;
        }

        return new MethodCastNode(boundMethodNode, cast, type);
    }

}
