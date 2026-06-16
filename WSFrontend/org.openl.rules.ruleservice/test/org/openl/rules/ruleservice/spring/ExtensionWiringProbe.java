package org.openl.rules.ruleservice.spring;

import jakarta.annotation.PreDestroy;

/**
 * Test probe that mirrors how the git module contributes its cleanup bean: a plain class declared in a
 * {@code META-INF/openl/extension-*.xml} whose lifecycle is driven by {@link PreDestroy}. Used to verify that
 * {@link ExtensionsConfiguration} both instantiates such beans eagerly and runs their {@code @PreDestroy} method on
 * context close.
 */
public class ExtensionWiringProbe {

    static volatile boolean created;
    static volatile boolean destroyed;

    public ExtensionWiringProbe() {
        created = true;
    }

    @PreDestroy
    public void markDestroyed() {
        destroyed = true;
    }
}
