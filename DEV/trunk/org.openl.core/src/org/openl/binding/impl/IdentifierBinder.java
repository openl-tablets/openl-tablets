/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.FieldNotFoundException;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

/**
 * @author snshor
 */

public class IdentifierBinder extends ANodeBinder {

    static boolean isStrictMatch(ISyntaxNode node) {
        return !node.getType().contains(".nostrict");
    }

    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        boolean strictMatch = isStrictMatch(node);

        String fieldName = ((IdentifierNode) node).getIdentifier();
        
        char first = fieldName.charAt(0);
        
        if (Character.isLetter(first) && Character.isUpperCase(first))
        {
            IOpenClass type = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, fieldName);

            if (type != null) {
                return new TypeBoundNode(node, type);
            }

            IOpenField om = bindingContext.findVar(ISyntaxConstants.THIS_NAMESPACE, fieldName, strictMatch);

            if (om != null) {
                return new FieldBoundNode(node, om);
            }
        	
        }
        else
        {
            IOpenField om = bindingContext.findVar(ISyntaxConstants.THIS_NAMESPACE, fieldName, strictMatch);

            if (om != null) {
                return new FieldBoundNode(node, om);
            }
        	
            IOpenClass type = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, fieldName);

            if (type != null) {
                return new TypeBoundNode(node, type);
            }
        	
        }	
        


        throw new BoundError(node, "Field not found: " + fieldName, null);

    }

    @Override
    public IBoundNode bindTarget(ISyntaxNode node, IBindingContext bindingContext, IBoundNode target) {

        try {
            // TODO define better use for strictmatch
            // boolean strictMatch = isStrictMatch(node);

            String fieldName = ((IdentifierNode) node).getIdentifier();

            IOpenField of = bindingContext.findFieldFor(target.getType(), fieldName, false);

            if (of == null) {
                throw new FieldNotFoundException("Identifier: ", fieldName, target.getType());
            }

            return new FieldBoundNode(node, of, target);

        } catch (Throwable t) {
            bindingContext.addError(new BoundError(node, "Identifier:", t));
            return new ErrorBoundNode(node);
        }

    }

}
