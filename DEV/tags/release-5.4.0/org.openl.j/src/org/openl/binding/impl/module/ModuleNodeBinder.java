/*
 * Created on Jul 24, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl.module;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.ANodeBinder;
import org.openl.syntax.ISyntaxNode;

/**
 * @author snshor
 *
 */
public class ModuleNodeBinder extends ANodeBinder {

    static final int TYPE_NODE = 0, METHOD_NAME_NODE = 1, PARAMETERS_NODE = 2, BODY_NODE = 3;

    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        // children should all have type IMemberBoundNode
        IBoundNode[] children = bindChildren(node, bindingContext);
        // TODO fix schema, name
        ModuleOpenClass module = new ModuleOpenClass(null, "UndefinedType", bindingContext.getOpenL());
        ModuleBindingContext moduleContext = new ModuleBindingContext(bindingContext, module);

        for (int i = 0; i < children.length; i++) {
            ((IMemberBoundNode) children[i]).addTo(moduleContext.getModule());
        }

        for (int i = 0; i < children.length; i++) {
            ((IMemberBoundNode) children[i]).finalizeBind(moduleContext);
        }

        return new ModuleNode(node, moduleContext.getModule());

    }

    // /* (non-Javadoc)
    // * @see org.openl.binding.INodeBinder#bind(org.openl.syntax.ISyntaxNode,
    // org.openl.binding.IBindingContext)
    // */
    // public IBoundNode bind2(ISyntaxNode node, IBindingContext bindingContext)
    // throws Exception
    // {
    //
    //
    // ModuleBindingContext moduleContext = new
    // ModuleBindingContext(bindingContext);
    //
    // int nMethods = node.getNumberOfChildren();
    //
    //
    //
    //
    // DeferredMethod[] deferredMethods =
    // new DeferredMethod[nMethods];
    //
    //
    //
    //
    //
    // MethodBindingContext[] methodContexts = new
    // MethodBindingContext[nMethods];
    //
    // for (int i = 0; i < node.getNumberOfChildren(); i++)
    // {
    // MethodBindingContext mbc = new MethodBindingContext(moduleContext);
    //
    // mbc.pushLocalVarContext();
    //
    // DeferredMethod dm = getMethodDescriptor(node.getChild(i), mbc);
    // deferredMethods[i] = dm;
    // methodContexts[i] = mbc;
    // moduleContext.addMethod(dm);
    // }
    //
    //
    // MethodNode[] children = new MethodNode[deferredMethods.length];
    //
    // for (int i = 0; i < deferredMethods.length; i++)
    // {
    // DeferredMethod dm = deferredMethods[i];
    // ISyntaxNode bodyNode = dm.methodBodyNode;
    //
    // IBoundNode boundBodyNode = bindChildNode(bodyNode, methodContexts[i]);
    // dm.setMethodBodyBoundNode((IBoundMethodNode)boundBodyNode);
    //
    // children[i] = new MethodNode(node.getChild(i), new
    // IBoundNode[]{boundBodyNode},
    // methodContexts[i].getLocalVarFrameSize(),
    // methodContexts[i].getParamFrameSize(), dm);
    // }
    //
    // return new ModuleNode(node, children);
    // }

    // DeferredMethod getMethodDescriptor(
    // ISyntaxNode methodNode,
    // MethodBindingContext methodBindingContext)
    // {
    // IBoundNode typeNode =
    // bindChildNode(methodNode.getChild(TYPE_NODE), methodBindingContext);
    //
    //
    // methodBindingContext.setReturnType(typeNode.getType());
    //
    // String methodName =
    // ((IdentifierNode)methodNode.getChild(METHOD_NAME_NODE)).getIdentifier();
    //
    // ISyntaxNode methodBodyNode = methodNode.getChild(BODY_NODE);
    //
    // ISyntaxNode parametersNode = methodNode.getChild(PARAMETERS_NODE);
    //
    // MethodParametersNode boundParametersNode =
    // (MethodParametersNode)bindChildNode(parametersNode,
    // methodBindingContext);
    //
    // DeferredMethod dm =
    // new DeferredMethod(
    // methodName,
    // typeNode.getType(),
    // boundParametersNode.getParameterTypes(),
    // null,
    // methodBodyNode);
    //
    //
    //
    //
    //
    // return dm;
    //
    // }

}
