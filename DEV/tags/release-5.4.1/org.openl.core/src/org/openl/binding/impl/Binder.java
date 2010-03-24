/*
 * Created on May 19, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

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
import org.openl.message.OpenLMessagesUtils;
import org.openl.syntax.ISyntaxError;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.SyntaxErrorException;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.impl.SyntaxError;
import org.openl.types.java.JavaOpenClass;

/**
 * Default implementation of {@link IOpenBinder}.
 * 
 * @author snshor
 */
public class Binder implements IOpenBinder {

    private INodeBinderFactory nodeBinderFactory;
    private INameSpacedMethodFactory methodFactory;
    private ICastFactory castFactory;
    private INameSpacedVarFactory varFactory;
    private INameSpacedTypeFactory typeFactory;
    private OpenL openl;

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

        if (delegator != null) {
            delegator.setTopDelegate(bindingContext);
            bindingContext = delegator;
        }

        ISyntaxNode syntaxNode = parsedCode.getTopNode();

        try {
            bindingContext.pushLocalVarContext();

            INodeBinder nodeBinder = bindingContext.findBinder(syntaxNode);

            if (nodeBinder == null) {
                
                String message = String.format("Binder not found for node '%s'", syntaxNode.getType());
                OpenLMessagesUtils.addError(message);
                
                throw new NullPointerException(message);
            }

            IBoundNode topnode = nodeBinder.bind(syntaxNode, bindingContext);

            bindingContext.popLocalVarContext();

            return new BoundCode(parsedCode, topnode, bindingContext.getError(), bindingContext.getLocalVarFrameSize());

        } catch (SyntaxErrorException see) {

            for (int i = 0; i < see.getSyntaxErrors().length; i++) {
                ISyntaxError err = see.getSyntaxErrors()[i];
                bindingContext.addError(err);

                OpenLMessagesUtils.addError(err.getMessage());
            }

            return new BoundCode(parsedCode, new ErrorBoundNode(syntaxNode), bindingContext.getError(), bindingContext.getLocalVarFrameSize());

        } catch (ProblemsWithChildrenError pwce) {
            OpenLMessagesUtils.addError(pwce.getMessage());

            return new BoundCode(parsedCode, new ErrorBoundNode(syntaxNode), bindingContext.getError(), bindingContext.getLocalVarFrameSize());
        } catch (SyntaxError se) {
            OpenLMessagesUtils.addError(se.getMessage());
            bindingContext.addError(se);

            return new BoundCode(parsedCode, new ErrorBoundNode(syntaxNode), bindingContext.getError(), bindingContext.getLocalVarFrameSize());
        }

        catch (Throwable t) {
            BoundError error = new BoundError(syntaxNode, "", t);
            bindingContext.addError(error);
            OpenLMessagesUtils.addError(error.getMessage());

            return new BoundCode(parsedCode, new ErrorBoundNode(syntaxNode), bindingContext.getError(), bindingContext.getLocalVarFrameSize());
        }
    }
}
