package org.openl.spring.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@TestPropertySource(properties = {
        "feature.enabled=true",
        "feature.disabled=false",
        "feature.blank=  ",
        "feature.numeric=42",
        "feature.mode=advanced",
        "feature.FALSE=FALSE"
})
@SpringJUnitConfig(EnableConditionTest.Config.class)
class EnableConditionTest {

    @Configuration
    static class Config {

        @Bean
        @ConditionalOnEnable("feature.enabled")
        String simpleEnabled() {
            return "simpleEnabled";
        }

        @Bean
        @ConditionalOnEnable("feature.disabled")
        String simpleDisabled() {
            return "simpleDisabled";
        }

        @Bean
        @ConditionalOnEnable("feature.missing")
        String simpleMissing() {
            return "simpleMissing";
        }

        @Bean
        @ConditionalOnEnable("feature.blank")
        String simpleBlank() {
            return "simpleBlank";
        }

        @Bean
        @ConditionalOnEnable({"feature.enabled", "feature.numeric"})
        String multipleAllMatch() {
            return "multipleAllMatch";
        }

        @Bean
        @ConditionalOnEnable({"feature.enabled", "feature.disabled"})
        String multipleOneFalse() {
            return "multipleOneFalse";
        }

        @Bean
        @ConditionalOnEnable("feature.mode==advanced")
        String equalsMatch() {
            return "equalsMatch";
        }

        @Bean
        @ConditionalOnEnable("feature.mode==basic")
        String equalsMismatch() {
            return "equalsMismatch";
        }

        @Bean
        @ConditionalOnEnable("feature.mode!=basic")
        String notEqualsMatch() {
            return "notEqualsMatch";
        }

        @Bean
        @ConditionalOnEnable("feature.mode!=advanced")
        String notEqualsMismatch() {
            return "notEqualsMismatch";
        }

        @Bean
        @ConditionalOnEnable("feature.missing==value")
        String equalsMissingProp() {
            return "equalsMissingProp";
        }

        @Bean
        @ConditionalOnEnable("feature.missing!=value")
        String notEqualsMissingProp() {
            return "notEqualsMissingProp";
        }

        @Bean
        @ConditionalOnEnable({"feature.enabled", "feature.mode == advanced"})
        String mixedConditionsMatch() {
            return "mixedConditionsMatch";
        }

        @Bean
        @ConditionalOnEnable({"feature.enabled", "feature.mode != advanced"})
        String mixedConditionsMismatch() {
            return "mixedConditionsMismatch";
        }

        @Bean
        @ConditionalOnEnable("feature.numeric")
        String numericValue() {
            return "numericValue";
        }

        @Bean
        @ConditionalOnEnable("feature.FALSE")
        String uppercaseFalse() {
            return "uppercaseFalse";
        }
    }

    @Autowired
    ApplicationContext ctx;

    @Test
    void simpleProperty_true_shouldEnable() {
        assertTrue(ctx.containsBean("simpleEnabled"));
    }

    @Test
    void simpleProperty_false_shouldDisable() {
        assertFalse(ctx.containsBean("simpleDisabled"));
    }

    @Test
    void simpleProperty_missing_shouldDisable() {
        assertFalse(ctx.containsBean("simpleMissing"));
    }

    @Test
    void simpleProperty_blank_shouldDisable() {
        assertFalse(ctx.containsBean("simpleBlank"));
    }

    @Test
    void simpleProperty_numeric_shouldEnable() {
        assertTrue(ctx.containsBean("numericValue"));
    }

    @Test
    void simpleProperty_uppercaseFalse_shouldDisable() {
        assertFalse(ctx.containsBean("uppercaseFalse"));
    }

    @Test
    void multipleProperties_allMatch_shouldEnable() {
        assertTrue(ctx.containsBean("multipleAllMatch"));
    }

    @Test
    void multipleProperties_oneFalse_shouldDisable() {
        assertFalse(ctx.containsBean("multipleOneFalse"));
    }

    @Test
    void equalsExpression_match_shouldEnable() {
        assertTrue(ctx.containsBean("equalsMatch"));
    }

    @Test
    void equalsExpression_mismatch_shouldDisable() {
        assertFalse(ctx.containsBean("equalsMismatch"));
    }

    @Test
    void notEqualsExpression_match_shouldEnable() {
        assertTrue(ctx.containsBean("notEqualsMatch"));
    }

    @Test
    void notEqualsExpression_mismatch_shouldDisable() {
        assertFalse(ctx.containsBean("notEqualsMismatch"));
    }

    @Test
    void equalsExpression_missingProperty_shouldDisable() {
        assertFalse(ctx.containsBean("equalsMissingProp"));
    }

    @Test
    void notEqualsExpression_missingProperty_shouldEnable() {
        assertTrue(ctx.containsBean("notEqualsMissingProp"));
    }

    @Test
    void mixedSimpleAndExpression_allTrue_shouldEnable() {
        assertTrue(ctx.containsBean("mixedConditionsMatch"));
    }

    @Test
    void mixedSimpleAndExpression_oneFalse_shouldDisable() {
        assertFalse(ctx.containsBean("mixedConditionsMismatch"));
    }
}
