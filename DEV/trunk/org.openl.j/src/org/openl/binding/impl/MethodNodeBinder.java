/*
 * Created on May 19, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.MethodUtil;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.syntax.impl.BinaryNode;

/**
 * @author snshor
 */

public class MethodNodeBinder extends ANodeBinder {    

    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        int childrenCount = node.getNumberOfChildren();

        if (childrenCount < 1) {
            BindHelper.processError("Method node should have at least one subnode", node, bindingContext, false);

            return new ErrorBoundNode(node);
        }

        ISyntaxNode lastNode = node.getChild(childrenCount - 1);

        String methodName = ((IdentifierNode) lastNode).getIdentifier();

        IBoundNode[] children = bindChildren(node, bindingContext, 0, childrenCount - 1);
        IOpenClass[] types = getTypes(children);

        IMethodCaller methodCaller = bindingContext
                                                   .findMethodCaller(ISyntaxConstants.THIS_NAMESPACE, methodName, types);

        if (methodCaller == null) {
            // try to bind method call Name(driver) as driver.name;
            if (childrenCount == 2) {
                IBoundNode accessorChain = bindAsAccessorChain(node.getModule(), bindingContext, methodName, node.getChild(0));
                if (accessorChain != null) {
                    return accessorChain;
                }
            }

            String message = String.format("Method '%s' not found", MethodUtil.printMethod(methodName, types));
            BindHelper.processError(message, node, bindingContext, false);

            return new ErrorBoundNode(node);
        }

        return new MethodBoundNode(node, children, methodCaller);
    }
    
    /**
     * 
     * @param sourceCodeModule
     * @param bindingContext
     * @param methodName
     * @param leftSide
     * @return
     */
    private IBoundNode bindAsAccessorChain(IOpenSourceCodeModule sourceCodeModule, IBindingContext bindingContext, String methodName, ISyntaxNode leftSide) {
        IdentifierNode rightNode = createIdentifierNode(sourceCodeModule, methodName);
        
        ISyntaxNode dotNode = new BinaryNode("chain.suffix.dot.identifier", null, leftSide, rightNode, sourceCodeModule);

        return bindChildNode(dotNode, bindingContext);
    }
    
    /**
     * 
     * @param sourceCodeModule
     * @param methodName
     * @return
     */
    private IdentifierNode createIdentifierNode(IOpenSourceCodeModule sourceCodeModule, String methodName) {
        String identifier = String.format("%s%s", methodName.substring(0, 1).toLowerCase(), methodName.substring(1));
        
        return new IdentifierNode("identifier", null, identifier, sourceCodeModule);        
    }

    @Override
    public IBoundNode bindTarget(ISyntaxNode node, IBindingContext bindingContext, IBoundNode target) throws Exception {

        int childrenCount = node.getNumberOfChildren();

        if (childrenCount < 1) {
            BindHelper.processError("New node should have at least one subnode", node, bindingContext);

            return new ErrorBoundNode(node);
        }

        ISyntaxNode lastNode = node.getChild(childrenCount - 1);

        String methodName = ((IdentifierNode) lastNode).getIdentifier();

        IBoundNode[] children = bindChildren(node, bindingContext, 0, childrenCount - 1);
        IOpenClass[] types = getTypes(children);

        IMethodCaller methodCaller = MethodSearch.getMethodCaller(methodName, types, bindingContext, target.getType());

        if (methodCaller == null) {

            StringBuffer buf = new StringBuffer("Method ");
            MethodUtil.printMethod(methodName, types, buf);
            buf.append(" not found in '" + target.getType().getName() + "'");

            BindHelper.processError(buf.toString(), node, bindingContext, false);

            return new ErrorBoundNode(node);
        }

        if (target.isStaticTarget() != methodCaller.getMethod().isStatic()) {

            if (methodCaller.getMethod().isStatic()) {
                BindHelper.processWarn("Access of a static method from non-static object", node, bindingContext);
            } else {
                BindHelper.processError("Access of a non-static method from a static object", node, bindingContext);

                return new ErrorBoundNode(node);
            }
        }

        MethodBoundNode result = new MethodBoundNode(node, children, methodCaller, target);
        result.setTargetNode(target);

        return result;
    }

}
