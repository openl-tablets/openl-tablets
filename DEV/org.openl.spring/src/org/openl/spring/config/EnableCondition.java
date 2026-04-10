package org.openl.spring.config;

import java.util.regex.Pattern;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import org.openl.util.StringUtils;

class EnableCondition implements Condition {
    private static final Pattern EXPRESSION = Pattern.compile("^(?<prop>[^!= ]+) *(?<cond>[!=])=(?<val>[^=]+)$");

    @Override
    public boolean matches(@NonNull ConditionContext context, AnnotatedTypeMetadata metadata) {
        var attrs = metadata.getAllAnnotationAttributes(ConditionalOnEnable.class.getName());
        if (attrs != null) {
            var env = context.getEnvironment();
            for (var value : attrs.get("value")) {
                for (var property : (String[]) value) {
                    if (property.contains("=")) {
                        // Conditional on equality
                        var m = EXPRESSION.matcher(property);

                        if (m.matches()) {
                            String actual = env.getProperty(m.group("prop"));
                            String expected = m.group("val").trim();

                            boolean isMatchValid = switch (m.group("cond")) {
                                case "=" -> expected.equals(actual);
                                case "!" -> !expected.equals(actual);
                                default -> throw new IllegalStateException("Unexpected condition: " + m.group("cond"));
                            };

                            if (!isMatchValid) return false;
                        } else {
                            // Let's fail instead of silent ignoring and falling to false
                            throw new IllegalArgumentException("'%s' is not valid expression".formatted(property));
                        }
                    } else {
                        // Conditional on existence
                        var propValue = env.getProperty(property);
                        if ("false".equalsIgnoreCase(propValue) || StringUtils.isBlank(propValue)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;

    }
}
