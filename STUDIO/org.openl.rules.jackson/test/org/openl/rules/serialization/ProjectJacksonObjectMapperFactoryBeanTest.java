package org.openl.rules.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.RulesDeploy;

/**
 * @author Yury Molchan
 */
class ProjectJacksonObjectMapperFactoryBeanTest {

    @Test
    void readsTheDefaultTypingModeInAnyCase() throws Exception {
        var objectMapper = withDefaultTypingMode(" non_final_and_enums ").createJacksonObjectMapper();

        assertEquals("[\"java.util.concurrent.TimeUnit\",\"SECONDS\"]",
                objectMapper.writeValueAsString(TimeUnit.SECONDS));
    }

    @Test
    void rejectsAnUnknownDefaultTypingMode() {
        var factory = withDefaultTypingMode("EVERYTHING");

        var e = assertThrows(ObjectMapperConfigurationParsingException.class, factory::createJacksonObjectMapper);
        assertEquals("Expected JAVA_LANG_OBJECT/OBJECT_AND_NON_CONCRETE/NON_CONCRETE_AND_ARRAYS/NON_FINAL"
                        + "/NON_FINAL_AND_ENUMS/DISABLED value for 'jackson.defaultTypingMode' in the configuration"
                        + " for service 'Rules'.",
                e.getMessage());
    }

    private static ProjectJacksonObjectMapperFactoryBean withDefaultTypingMode(String defaultTypingMode) {
        var rulesDeploy = new RulesDeploy();
        rulesDeploy.setServiceName("Rules");
        rulesDeploy.setConfiguration(Map.of(ProjectJacksonObjectMapperFactoryBean.JACKSON_DEFAULT_TYPING_MODE,
                defaultTypingMode));
        var factory = new ProjectJacksonObjectMapperFactoryBean();
        factory.setRulesDeploy(rulesDeploy);
        return factory;
    }
}
