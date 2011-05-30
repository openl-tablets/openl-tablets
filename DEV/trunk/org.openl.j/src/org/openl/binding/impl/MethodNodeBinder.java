/*
 * Created on May 19, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.MethodUtil;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;

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
        IOpenClass[] parameterTypes = getTypes(children);

        IMethodCaller methodCaller = bindingContext
                                                   .findMethodCaller(ISyntaxConstants.THIS_NAMESPACE, methodName, parameterTypes);
        
        // can`t find directly the method with given name and parameters. so, try to bind it some additional ways
        //
        if (methodCaller == null) {   
            
            // try to bind method with one of the array arguments, as the method without array argument
            // (but the component type of it on the same place). and call it several times on runtime for collecting results.
            //
            IBoundNode arrayParametersMethod = new ArrayArgumentsMethodBinder(methodName, parameterTypes, children).bind(node, bindingContext);
            
            if (arrayParametersMethod != null) {
                return arrayParametersMethod;
            }   
            
            // try to bind method call Name(driver) as driver.name;
            //
            if (childrenCount == 2) {
                IBoundNode accessorChain = new FieldAccessMethodBinder(methodName, children).bind(node, bindingContext);
                if (accessorChain != null && !(accessorChain instanceof ErrorBoundNode)) {
                    return accessorChain;
                } 
            }
            
            // try to bind method call as method with variable length of arguments
            //
            if (parameterTypes.length >= 1) {
                IBoundNode varArgsMethod = new VariableLengthArgumentsMethodBinder(methodName, parameterTypes, children).bind(node, bindingContext);            
                if (varArgsMethod != null) {
                    return varArgsMethod;
                }
            }

            return cantFindMethodError(node, bindingContext, methodName, parameterTypes);
        }

        return new MethodBoundNode(node, children, methodCaller);
    }

    private IBoundNode cantFindMethodError(ISyntaxNode node, IBindingContext bindingContext, String methodName,
            IOpenClass[] parameterTypes) {
        String message = String.format("Method '%s' not found", MethodUtil.printMethod(methodName, parameterTypes));
        BindHelper.processError(message, node, bindingContext, false);

        return new ErrorBoundNode(node);
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
