package org.openl.binding.symbolic;

import org.openl.syntax.ISyntaxNode;

public interface ISymbolicBinder {

    void bindSymbolic(ISyntaxNode node, ISymbolicBindingContext cxt);

}
