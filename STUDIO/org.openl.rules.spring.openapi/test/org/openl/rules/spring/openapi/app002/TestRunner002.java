package org.openl.rules.spring.openapi.app002;

import org.openl.rules.spring.openapi.AbstractSpringOpenApiTest;
import org.openl.rules.spring.openapi.MockConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = { MockConfiguration.class, TestRunner002.TestConfig.class })
public class TestRunner002 extends AbstractSpringOpenApiTest {

    @Configuration
    @ComponentScan(basePackages = "org.openl.rules.spring.openapi.app002")
    public static class TestConfig {
    }

}
