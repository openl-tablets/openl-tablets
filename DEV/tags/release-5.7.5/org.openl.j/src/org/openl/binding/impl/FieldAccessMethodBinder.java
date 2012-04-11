package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

/**
 * Binder for constructions like <code>'Name(driver)'</code> and the array analog <code>'Name(drivers)'</code>.
 * Is binded as access to the field with name equal to method name.
 * 
 * @author DLiauchuk
 *
 */
public class FieldAccessMethodBinder extends ANodeBinder {

    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {
        
        int childrenCount = node.getNumberOfChildren();
                
        if (childrenCount != 2) {
            // only to children are possible, the function itself and the parameter.
            //
            BindHelper.processError("Field access method node should have 2 subnodes", node, bindingContext, false);

            return new ErrorBoundNode(node);
        }

        ISyntaxNode methodNode = node.getChild(childrenCount - 1);
        String methodName = ((IdentifierNode) methodNode).getIdentifier();
        
        ISyntaxNode argumentNode = node.getChild(0);
        IOpenClass argumentType = getArgumentType(node, bindingContext, childrenCount);
        
        String fieldName = getAsFieldName(methodName);
        
        IBoundNode accessorChain = null;
        
        if (argumentType.isArray()) {
            accessorChain = bindArrayArgument(fieldName, bindingContext, argumentNode, argumentType.getComponentClass());
        } else {
            accessorChain = bindSingleArgument(fieldName, argumentNode, bindingContext);
        }
        
        if (accessorChain == null) {           
            BindHelper.processError("Can`t bind as field access method", node, bindingContext, false);

            return new ErrorBoundNode(node);
        }
        
        return accessorChain;
    }

    private IOpenClass getArgumentType(ISyntaxNode node, IBindingContext bindingContext, int childrenCount) 
            throws SyntaxNodeException {        
        // only one child, as there are 2 nodes, one of them is the function itself.
        IBoundNode[] children = bindChildren(node, bindingContext, 0, childrenCount - 1);
        IOpenClass[] types = getTypes(children);
        return types[0];        
    }
   
    private IBoundNode bindArrayArgument(String fieldName, IBindingContext bindingContext, ISyntaxNode argumentNode, 
            IOpenClass argumentComponentType) {
        
        IBoundNode containerField = bindChildNode(argumentNode, bindingContext);
        
        IOpenField field = bindingContext.findFieldFor(argumentComponentType, fieldName, false);
        
        if (field == null) {
            // Appropriate error will be processed later.
            //
            return null;
        }
            
        return new MultiCallFieldAccessMethodBoundNode(argumentNode, containerField, field);
    }

    private IBoundNode bindSingleArgument(String fieldName, ISyntaxNode argumentNode, IBindingContext bindingContext) {
        // gets the bound node for argument syntax node.
        //
        IBoundNode target = bindChildNode(argumentNode, bindingContext);
        
        // bind the access to field.
        return BindHelper.bindAsField(fieldName, argumentNode.getParent(), bindingContext, target);
    }

    private String getAsFieldName(String methodName) {
        return String.format("%s%s", methodName.substring(0, 1).toLowerCase(), methodName.substring(1));
    }
    
    
}
