package org.openl.rules.ruleservice.spring;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class ExtensionsConfigurationTest {

    @Test
    void importsAndDestroysPlainExtensionBeans() {
        ExtensionWiringProbe.created = false;
        ExtensionWiringProbe.destroyed = false;

        try (var context = new AnnotationConfigApplicationContext(ExtensionsConfiguration.class)) {
            assertTrue(ExtensionWiringProbe.created,
                    "ExtensionsConfiguration must eagerly instantiate a plain bean from a META-INF/openl/extension-*.xml");
        }

        assertTrue(ExtensionWiringProbe.destroyed,
                "the extension bean's destroy-method must run when the context closes");
    }
}
