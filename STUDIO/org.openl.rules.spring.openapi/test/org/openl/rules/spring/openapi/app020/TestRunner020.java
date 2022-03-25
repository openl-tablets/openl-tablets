package org.openl.rules.spring.openapi.app020;

import org.openl.rules.spring.openapi.AbstractSpringOpenApiTest;
import org.openl.rules.spring.openapi.MockConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = { MockConfiguration.class, TestRunner020.TestConfig.class })
public class TestRunner020 extends AbstractSpringOpenApiTest {

    @Configuration
    @ComponentScan(basePackages = "org.openl.rules.spring.openapi.app020")
    public static class TestConfig {
    }

}
