/*
 * Created on Jan 7, 2004
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.error.BoundError;
import org.openl.syntax.ISyntaxNode;

/**
 * @author snshor
 *
 */
public class ProblemsWithChildrenError extends BoundError {

    private static final long serialVersionUID = 1296510383411584060L;

    public ProblemsWithChildrenError(ISyntaxNode node) {
        super("Some Problems With Children", null, node);
    }

}
