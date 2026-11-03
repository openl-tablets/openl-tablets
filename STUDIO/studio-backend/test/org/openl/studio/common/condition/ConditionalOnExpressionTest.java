package org.openl.studio.common.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.expression.spel.SpelEvaluationException;

class ConditionalOnExpressionTest {

    @Test
    void aBeanIsRegisteredOnlyWhenItsExpressionHoldsForTheProperties() {
        assertEquals(Set.of("notSingle", "multi", "adOrMulti"), beansFor(Map.of("user.mode", "multi")));
        assertEquals(Set.of("single"), beansFor(Map.of("user.mode", "single")));
        assertEquals(Set.of("single", "demo"), beansFor(Map.of("user.mode", "single", "demo.init", "true")));
    }

    @Test
    void anAbsentPropertyKeepsItsPlaceholderAndMatchesNoValue() {
        assertEquals(Set.of("notSingle"), beansFor(Map.of()));
    }

    @Test
    void anExpressionReachesNoType() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(TypeReference.class);
            assertThrows(SpelEvaluationException.class, context::refresh);
        }
    }

    private static Set<String> beansFor(Map<String, Object> properties) {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", properties));
            context.register(Modes.class);
            context.refresh();
            return Set.copyOf(context.getBeansOfType(String.class).keySet());
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class Modes {

        @Bean
        @ConditionalOnExpression("'${user.mode}' != 'single'")
        String notSingle() {
            return "";
        }

        @Bean
        @ConditionalOnExpression("'${user.mode}' == 'single'")
        String single() {
            return "";
        }

        @Bean
        @ConditionalOnExpression("'${user.mode}' == 'multi'")
        String multi() {
            return "";
        }

        @Bean
        @ConditionalOnExpression("'${user.mode}' == 'ad' || '${user.mode}' == 'multi'")
        String adOrMulti() {
            return "";
        }

        @Bean
        @ConditionalOnExpression("'${demo.init}' == 'true' and '${user.mode}' == 'single'")
        String demo() {
            return "";
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class TypeReference {

        @Bean
        @ConditionalOnExpression("T(java.lang.Boolean).TRUE")
        String typed() {
            return "";
        }
    }
}
