package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Binder for constructions like <code>'Name(driver)'</code> and the array analog <code>'Name(drivers)'</code>.
 * Is binded as access to the field with name equal to method name.
 *
 * @author DLiauchuk
 */
public class FieldAccessMethodBinder extends ANodeBinder {

    private String methodName;
    private IBoundNode[] children;

    private final Logger log = LoggerFactory.getLogger(FieldAccessMethodBinder.class);

    public FieldAccessMethodBinder(String methodName, IBoundNode[] children) {
        this.methodName = methodName;
        this.children = children.clone();
    }

    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        ISyntaxNode argumentNode = node.getChild(0);
        IOpenClass argumentType = getArgumentType();

        String fieldName = getAsFieldName(methodName);

        IBoundNode accessorChain;

        if (argumentType.isArray()) {
            accessorChain = bindArrayArgument(fieldName, bindingContext, argumentNode, argumentType.getComponentClass());
        } else {
            accessorChain = bindSingleArgument(fieldName, argumentNode, bindingContext);
        }

        if (accessorChain == null) {
            // It just means that another binder must be used. It's not an error
            log.debug("Can`t bind as field access method the method with name {}", methodName);
        }

        return accessorChain;
    }

    private IOpenClass getArgumentType() {
        // only one child, as there are 2 nodes, one of them is the function itself.
        //
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

        return new MultiCallFieldAccessMethodBoundNode(argumentNode.getParent(), containerField, field);
    }

    private IBoundNode bindSingleArgument(String fieldName, ISyntaxNode argumentNode, IBindingContext bindingContext) {
        // gets the bound node for argument syntax node.
        //
        IBoundNode target = bindChildNode(argumentNode, bindingContext);

        IOpenField field = bindingContext.findFieldFor(target.getType(), fieldName, false);

        if (field != null) {
            return new FieldBoundNode(argumentNode.getParent(), field, target);
        }
        return null;

    }

    private String getAsFieldName(String methodName) {
        return String.format("%s%s", methodName.substring(0, 1).toLowerCase(), methodName.substring(1));
    }


}
