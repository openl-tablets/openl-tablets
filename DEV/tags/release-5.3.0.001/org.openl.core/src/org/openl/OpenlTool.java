/*
 * Created on Oct 6, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.IBoundCode;
import org.openl.binding.IBoundMethodHeader;
import org.openl.binding.IBoundMethodNode;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.ANodeBinder;
import org.openl.binding.impl.Binder;
import org.openl.binding.impl.BindingContext;
import org.openl.binding.impl.BindingContextDelegator;
import org.openl.binding.impl.BoundError;
import org.openl.binding.impl.CastNode;
import org.openl.binding.impl.TypeBoundNode;
import org.openl.binding.impl.module.MethodBindingContext;
import org.openl.syntax.IParsedCode;
import org.openl.syntax.ISyntaxError;
import org.openl.syntax.SyntaxErrorException;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenCast;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 *
 */
public class OpenlTool {

    static class MethodCastNode extends CastNode implements IBoundMethodNode {

        /**
         * @param bnode
         * @param cast
         * @param castedType
         */
        public MethodCastNode(IBoundNode bnode, IOpenCast cast, IOpenClass castedType) {
            super(null, bnode, cast, castedType);
        }

        /**
         *
         */

        public int getLocalFrameSize() {
            return ((IBoundMethodNode) children[0]).getLocalFrameSize();
        }

        /**
         *
         */

        public int getParametersSize() {
            return ((IBoundMethodNode) children[0]).getParametersSize();
        }

    }

    static IBoundMethodNode bindMethodType(IBoundMethodNode bnode, IBindingContext bindingContext, IOpenClass type)
            throws Exception

    {

        // if (bnode.getType().equals(type))
        // return bnode;

        if (type == JavaOpenClass.VOID || type == NullOpenClass.the) {
            return bnode;
        }

        IOpenCast cast = ANodeBinder.getCast(bnode, type, bindingContext);

        if (cast == null) {
            return bnode;
        }

        return new MethodCastNode(bnode, cast, type);

    }

    static public void compileMethod(IOpenSourceCodeModule src, OpenL openl, CompositeMethod comp, IBindingContext cxt) {
        try {
            cxt.pushErrors();
            IOpenParser parser = openl.getParser();

            IParsedCode pc = parser.parseAsMethodBody(src);
            ISyntaxError[] error = pc.getError();
            if (error.length > 0) {
                throw new SyntaxErrorException("Parsing Error:", error);
            }

            IOpenBinder binder = openl.getBinder();

            MethodBindingContext mbc = new MethodBindingContext(comp.getHeader(), cxt);

            IBoundCode bc = binder.bind(pc, mbc);
            error = bc.getError();
            if (error.length > 0) {
                throw new SyntaxErrorException("Binding Error:", error);
            }

            IBoundMethodNode ibmn = null;
            try {
                ibmn = bindMethodType((IBoundMethodNode) bc.getTopNode(), cxt, comp.getHeader().getType());
            } catch (Exception ex) {

                BoundError be = new BoundError(bc.getTopNode().getSyntaxNode(), "", ex);
                throw new SyntaxErrorException("", new ISyntaxError[] { be });
            }

            comp.setMethodBodyBoundNode(ibmn);

        } finally {
            cxt.popErrors();
        }
    }

    static public IOpenMethodHeader getMethodHeader(IOpenSourceCodeModule src, OpenL openl, IBindingContextDelegator cxt) {
        if (cxt == null) {
            cxt = new BindingContextDelegator(new BindingContext((Binder) openl.getBinder(), JavaOpenClass.VOID, openl));
        }

        try {
            cxt.pushErrors();
            IOpenParser parser = openl.getParser();

            IParsedCode pc = parser.parseAsMethodHeader(src);
            ISyntaxError[] error = pc.getError();
            if (error.length > 0) {
                throw new SyntaxErrorException("Parsing Error:", error);
            }

            IOpenBinder binder = openl.getBinder();

            IBoundCode bc = binder.bind(pc, cxt);
            error = bc.getError();
            if (error.length > 0) {
                throw new SyntaxErrorException("Binding Error:", error);
            }

            return ((IBoundMethodHeader) bc.getTopNode()).getMethodHeader();
        } finally {
            cxt.popErrors();
        }

    }

    static public IOpenClass getType(String typeName, OpenL openl) {
        IOpenBinder binder = openl.getBinder();
        return binder.makeBindingContext().findType(ISyntaxConstants.THIS_NAMESPACE, typeName);
    }

    static public CompositeMethod makeMethod(IOpenSourceCodeModule src, OpenL openl, IOpenMethodHeader header,
            IBindingContext cxt) {
        return makeMethod(src, openl, header, MethodBindingContext.DEFAULT_CONTEXT_LEVEL, cxt);
    }

    static public CompositeMethod makeMethod(IOpenSourceCodeModule src, OpenL openl, IOpenMethodHeader header,
            int depthParameterSearchLevel, IBindingContext cxt) {
        try {
            cxt.pushErrors();
            IOpenParser parser = openl.getParser();

            IParsedCode pc = parser.parseAsMethodBody(src);
            ISyntaxError[] error = pc.getError();
            if (error.length > 0) {
                throw new SyntaxErrorException("Parsing Error:", error);
            }

            IOpenBinder binder = openl.getBinder();

            boolean searchInContext = depthParameterSearchLevel >= 0;
            MethodBindingContext mbc = new MethodBindingContext(header, cxt, searchInContext, depthParameterSearchLevel);

            IBoundCode bc = binder.bind(pc, mbc);
            error = bc.getError();
            if (error.length > 0) {
                throw new SyntaxErrorException("Binding Error:", error);
            }

            IBoundMethodNode ibmn = null;
            try {
                ibmn = bindMethodType((IBoundMethodNode) bc.getTopNode(), cxt, header.getType());
            } catch (Exception ex) {

                BoundError be = new BoundError(bc.getTopNode().getSyntaxNode(), "", ex);
                throw new SyntaxErrorException("", new ISyntaxError[] { be });
            }

            return new CompositeMethod(header,
            // (IBoundMethodNode) bc.getTopNode()
                    ibmn);
        } finally {
            cxt.popErrors();
        }
    }

    static public CompositeMethod makeMethodWithUnknownType(IOpenSourceCodeModule src, OpenL openl, String name,
            IMethodSignature signature, IOpenClass declaringClass, IBindingContext cxt) {
        return makeMethodWithUnknownType(src, openl, name, signature, declaringClass,
                MethodBindingContext.DEFAULT_CONTEXT_LEVEL, cxt);
    }

    static public CompositeMethod makeMethodWithUnknownType(IOpenSourceCodeModule src, OpenL openl, String name,
            IMethodSignature signature, IOpenClass declaringClass, int depthParameterSearchLevel, IBindingContext cxt) {
        OpenMethodHeader header = new OpenMethodHeader(name, NullOpenClass.the, signature, declaringClass);
        try {
            cxt.pushErrors();
            IOpenParser parser = openl.getParser();

            IParsedCode pc = parser.parseAsMethodBody(src);
            ISyntaxError[] error = pc.getError();
            if (error.length > 0) {
                throw new SyntaxErrorException("Parsing Error:", error);
            }

            IOpenBinder binder = openl.getBinder();

            boolean searchInContext = depthParameterSearchLevel >= 0;
            MethodBindingContext mbc = new MethodBindingContext(header, cxt, searchInContext, depthParameterSearchLevel);

            IBoundCode bc = binder.bind(pc, mbc);
            error = bc.getError();
            if (error.length > 0) {
                throw new SyntaxErrorException("Binding Error:", error);
            }

            IOpenClass retType = mbc.getReturnType();
            if (retType == NullOpenClass.the) {
                retType = bc.getTopNode().getType();
            }
            header.setTypeClass(retType);

            IBoundMethodNode ibmn = null;
            try {
                ibmn = bindMethodType((IBoundMethodNode) bc.getTopNode(), cxt, header.getType());
            } catch (Exception ex) {

                BoundError be = new BoundError(bc.getTopNode().getSyntaxNode(), "", ex);
                throw new SyntaxErrorException("", new ISyntaxError[] { be });
            }

            return new CompositeMethod(header, ibmn);
        } finally {
            cxt.popErrors();
        }
    }

    static public IOpenClass makeType(IOpenSourceCodeModule src, OpenL openl, IBindingContextDelegator cxt) {
        try {

            if (cxt == null) {
                cxt = new BindingContextDelegator(openl.getBinder().makeBindingContext());
            }
            cxt.pushErrors();
            IOpenParser parser = openl.getParser();

            IParsedCode pc = parser.parseAsType(src);
            ISyntaxError[] error = pc.getError();
            if (error.length > 0) {
                throw new SyntaxErrorException("Parsing Error:", error);
            }

            IOpenBinder binder = openl.getBinder();

            IBoundCode bc = binder.bind(pc, cxt);
            error = bc.getError();
            if (error.length > 0) {
                throw new SyntaxErrorException("Binding Error:", error);
            }

            return ((TypeBoundNode) bc.getTopNode()).getType();

        } finally {
            cxt.popErrors();
        }

    }

    /**
     *
     */
    public OpenlTool() {
        super();
        // TODO Auto-generated constructor stub
    }
}
