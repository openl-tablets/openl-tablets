package org.openl.rules.dt2.algorithm;

import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;

public class CanNotIndexConditionsException extends SyntaxNodeException {

	private static final long serialVersionUID = -4891241884177613595L;

	public CanNotIndexConditionsException(
			ISyntaxNode syntaxNode) {
		super("Can not index conditions with formulas", null, syntaxNode);
	}

}
