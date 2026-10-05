package org.openl.rules.cmatch.algorithm;

import org.openl.binding.IBindingContext;
import org.openl.rules.cmatch.ColumnMatch;
import org.openl.syntax.exception.SyntaxNodeException;

public interface IMatchAlgorithmCompiler {
    void compile(IBindingContext bindingContext, ColumnMatch columnMatch) throws SyntaxNodeException;

    /**
     * How many rows of a ColumnMatch table, under its ids and its titles, give what the table returns or scores rather
     * than check a condition. The conditions start on the row after them.
     *
     * @return the count of the rows
     */
    int getSpecialRowCount();
}
