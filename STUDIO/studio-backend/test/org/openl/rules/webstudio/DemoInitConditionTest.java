package org.openl.rules.webstudio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

/**
 * When the demo projects are created, so that OpenL Studio starts in every user mode.
 *
 * @author Yury Molchan
 */
class DemoInitConditionTest {

    @ParameterizedTest
    @CsvSource({"true, single, true",
            "true, multi, false",
            "true, ad, false",
            "false, single, false"})
    void createsTheDemoInTheSingleUserModeOnly(String demoInit, String userMode, boolean created) {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment()
                    .getPropertySources()
                    .addFirst(new MapPropertySource("test", Map.of("demo.init", demoInit, "user.mode", userMode)));

            context.register(DemoInit.class);

            // The demo depends on the user of the single-user mode, which no other mode has: registered there, it
            // would stop OpenL Studio from starting.
            assertEquals(created, context.containsBeanDefinition("demoInit"));
        }
    }
}
