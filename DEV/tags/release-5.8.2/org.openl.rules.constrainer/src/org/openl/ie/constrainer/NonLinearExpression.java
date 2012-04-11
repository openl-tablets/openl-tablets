package org.openl.ie.constrainer;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2002
 * </p>
 * <p>
 * Company:
 * </p>
 *
 * @author unascribed
 * @version 1.0
 */

public class NonLinearExpression extends Failure {
    Expression _exp;

    public NonLinearExpression(Expression exp) {
        _exp = exp;
    }

    public Expression getExpression() {
        return _exp;
    }
}