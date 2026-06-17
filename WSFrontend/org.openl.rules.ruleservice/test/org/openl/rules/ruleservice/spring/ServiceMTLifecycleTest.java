package org.openl.rules.ruleservice.spring;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class ServiceMTLifecycleTest {

    @Test
    void destroyShutsDownServiceMTWithoutError() {
        assertDoesNotThrow(() -> new ServiceMTLifecycle().destroy());
    }
}
