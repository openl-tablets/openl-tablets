/*
 * Created on Jan 7, 2004
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.syntax.ISyntaxNode;

/**
 * @author snshor
 *
 */
public class ProblemsWithChildrenError extends BoundError {

    /**
     *
     */
    private static final long serialVersionUID = 1296510383411584060L;

    /**
     * @param node
     * @param msg
     * @param t
     */
    public ProblemsWithChildrenError(ISyntaxNode node) {
        super(node, "Some Problems With Children", null);
    }

}
