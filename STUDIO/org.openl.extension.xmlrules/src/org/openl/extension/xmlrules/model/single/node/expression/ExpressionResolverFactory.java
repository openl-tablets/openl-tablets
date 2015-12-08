package org.openl.extension.xmlrules.model.single.node.expression;

public final class ExpressionResolverFactory {
    private ExpressionResolverFactory() {
    }

    public static ExpressionResolver getExpressionResolver(String operator) {
        Operator op = Operator.findOperator(operator);
        if (op == null) {
            throw new UnsupportedOperationException("Operator '" + operator + "' isn't supported");
        }

        return getExpressionResolver(op);
    }

    public static ExpressionResolver getExpressionResolver(Operator operator) {
        if (operator == null) {
            return null;
        }

        switch (operator) {
            case Concatenate:
                return new ConcatenateExpressionResolver();
            case Addition:
            case Subtraction:
            case Multiplication:
            case Division:
                return new ArithmeticExpressionResolver();
            case Range:
                return new RangeExpressionResolver();
            default:
                return new SimpleExpressionResolver();
        }
    }
}
