package org.openl.extension.xmlrules.model.single.node.expression;

public final class ExpressionResolverFactory {
    private ExpressionResolverFactory() {
    }

    public static ExpressionResolver getExpressionResolver(Operator operator) {
        if (operator == null) {
            return null;
        }

        switch (operator) {
            case Concatenate:
                return new ConcatenateExpressionResolver();
            case Addition:
                return new AdditionExpressionResolver();
            default:
                return new SimpleExpressionResolver();
        }
    }
}
