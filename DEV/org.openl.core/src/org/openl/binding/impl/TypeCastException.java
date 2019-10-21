/**
 * Created Jul 21, 2007
 */
package org.openl.binding.impl;

import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public class TypeCastException extends SyntaxNodeException {

    private static final long serialVersionUID = 5570752529258476343L;

    private IOpenClass from;
    private IOpenClass to;

    public TypeCastException(ISyntaxNode node, IOpenClass from, IOpenClass to) {
        super("Cannot convert from " + from.getName() + " to " + to.getName(), null, node);

        this.from = from;
        this.to = to;
    }

    public IOpenClass getFrom() {
        return from;
    }

    public IOpenClass getTo() {
        return to;
    }

}
