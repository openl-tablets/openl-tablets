package org.openl.studio.common.condition;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

/**
 * Evaluates the expression of {@link ConditionalOnExpression} against the environment of the context.
 */
class OnExpressionCondition implements Condition {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        var value = metadata.getAnnotations().get(ConditionalOnExpression.class).getString("value");
        var expression = context.getEnvironment().resolvePlaceholders(value);
        var result = PARSER.parseExpression(expression)
                .getValue(SimpleEvaluationContext.forReadOnlyDataBinding().build(), Boolean.class);
        return Boolean.TRUE.equals(result);
    }
}
