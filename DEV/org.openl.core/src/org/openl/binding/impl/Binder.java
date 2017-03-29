/*
 * Created on May 19, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import java.util.HashMap;
import java.util.Map;

import org.openl.IOpenBinder;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.IBoundCode;
import org.openl.binding.IBoundNode;
import org.openl.binding.ICastFactory;
import org.openl.binding.INameSpacedMethodFactory;
import org.openl.binding.INameSpacedTypeFactory;
import org.openl.binding.INameSpacedVarFactory;
import org.openl.binding.INodeBinder;
import org.openl.binding.INodeBinderFactory;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.impl.MethodKey;
import org.openl.types.java.JavaOpenClass;

/**
 * Default implementation of {@link IOpenBinder}.
 * 
 * @author snshor
 */
public class Binder implements IOpenBinder {

    private OpenL openl;

    private INodeBinderFactory nodeBinderFactory;

    private ICastFactory castFactory;

    private INameSpacedVarFactory varFactory;
    private INameSpacedTypeFactory typeFactory;
    private INameSpacedMethodFactory methodFactory;

    public Binder(INodeBinderFactory nodeBinderFactory,
        INameSpacedMethodFactory methodFactory,
        ICastFactory castFactory,
        INameSpacedVarFactory varFactory,
        INameSpacedTypeFactory typeFactory,
        OpenL openl) {

        this.nodeBinderFactory = nodeBinderFactory;
        this.methodFactory = methodFactory;
        this.castFactory = castFactory;
        this.varFactory = varFactory;
        this.typeFactory = typeFactory;
        this.openl = openl;
    }

    public ICastFactory getCastFactory() {
        return castFactory;
    }

    public INameSpacedMethodFactory getMethodFactory() {
        return methodFactory;
    }

    public INodeBinderFactory getNodeBinderFactory() {
        return nodeBinderFactory;
    }

    public INameSpacedTypeFactory getTypeFactory() {
        return typeFactory;
    }

    public INameSpacedVarFactory getVarFactory() {
        return varFactory;
    }

    public IBindingContext makeBindingContext() {
        return new BindingContext(this, JavaOpenClass.VOID, openl);
    }

    public IBoundCode bind(IParsedCode parsedCode) {
        return bind(parsedCode, null);
    }

    /*
     * (non-Javadoc)
     * @see org.openl.IOpenBinder#bind(org.openl.syntax.IParsedCode)
     */
    public IBoundCode bind(IParsedCode parsedCode, IBindingContextDelegator delegator) {

        IBindingContext bindingContext = makeBindingContext();
        bindingContext = BindHelper.delegateContext(bindingContext, delegator);

        ISyntaxNode syntaxNode = parsedCode.getTopNode();

        try {
            bindingContext.pushLocalVarContext();

            // Bound syntax node.
            //
            IBoundNode topNode = bindNode(syntaxNode, parsedCode, bindingContext);

            bindingContext.popLocalVarContext();

            return new BoundCode(parsedCode, topNode, bindingContext.getErrors(), bindingContext.getLocalVarFrameSize());

        } catch (Throwable cause) {
            // Process error/exception at first.
            //
            bindingContext.addError(SyntaxNodeExceptionUtils.createError(cause, syntaxNode));

            // Return bound code with errors.
            //
            return BindHelper.makeInvalidCode(parsedCode, syntaxNode, bindingContext);
        }
    }

    private IBoundNode bindNode(ISyntaxNode syntaxNode, IParsedCode parsedCode, IBindingContext bindingContext)
                                                                                                               throws Exception {
        INodeBinder nodeBinder = bindingContext.findBinder(syntaxNode);

        if (nodeBinder == null) {
            throw new OpenlNotCheckedException(String.format("Binder is not found for node '%s'", syntaxNode.getType()));
        }

        return nodeBinder.bind(syntaxNode, bindingContext);
    }
    
    
    
    Map<MethodKey, Object> methodCache = new HashMap<MethodKey, Object>();
    

}
