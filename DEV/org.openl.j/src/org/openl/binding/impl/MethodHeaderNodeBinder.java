package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.module.MethodParametersNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IMethodSignature;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.util.text.ILocation;

/*
 * Created on Sep 23, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

/**
 * @author snshor
 * 
 */
public class MethodHeaderNodeBinder extends ANodeBinder {

    private static final int TYPE_NODE = 0;
    private static final int METHOD_NAME_NODE = 1;
    private static final int PARAMETERS_NODE = 2;

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.binding.INodeBinder#bind(org.openl.syntax.ISyntaxNode, org.openl.binding.IBindingContext)
     */
    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        IBoundNode typeNode = bindChildNode(node.getChild(TYPE_NODE), bindingContext);

        String methodName = ((IdentifierNode) node.getChild(METHOD_NAME_NODE)).getIdentifier();

        ISyntaxNode parametersNode = node.getChild(PARAMETERS_NODE);
        MethodParametersNode boundParametersNode = (MethodParametersNode) bindChildNode(parametersNode, bindingContext);

        IMethodSignature signature = boundParametersNode.getSignature();

        ILocation typeLocation = null;
        ILocation[] paramTypeLocations = null;
        if (!bindingContext.isExecutionMode()) {
            ISyntaxNode syntaxNode = typeNode.getSyntaxNode();
            while (syntaxNode.getNumberOfChildren() == 1 && !(syntaxNode instanceof IdentifierNode)) {
                // Get type node for array
                syntaxNode = syntaxNode.getChild(0);
            }
            typeLocation = syntaxNode.getSourceLocation();

            paramTypeLocations = new ILocation[signature.getNumberOfParameters()];
            for (int i = 0; i < signature.getNumberOfParameters(); i++) {
                paramTypeLocations[i] = boundParametersNode.getParamTypeLocation(i);
            }
        }

        OpenMethodHeader header = new OpenMethodHeader(methodName,
            typeNode.getType(),
            signature,
            null,
            typeLocation,
            paramTypeLocations);

        return new MethodHeaderNode(node, header);
    }

}
