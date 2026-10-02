/*
 * Created on May 19, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding;

import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 */
public interface INodeBinder {

    // Implemented by every node binder, whose binding steps throw many different checked exceptions.
    @SuppressWarnings("java:S112")
    IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception;

    // Implemented by every node binder, whose binding steps throw many different checked exceptions.
    @SuppressWarnings("java:S112")
    IBoundNode bindTarget(ISyntaxNode node, IBindingContext bindingContext, IBoundNode targetNode) throws Exception;

    // Implemented by every node binder, whose binding steps throw many different checked exceptions.
    @SuppressWarnings("java:S112")
    IBoundNode bindType(ISyntaxNode node, IBindingContext bindingContext, IOpenClass type) throws Exception;

}
