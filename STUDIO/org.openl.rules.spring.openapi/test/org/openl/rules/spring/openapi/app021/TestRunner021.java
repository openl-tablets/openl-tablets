package org.openl.rules.spring.openapi.app021;

import org.openl.rules.spring.openapi.AbstractSpringOpenApiTest;
import org.openl.rules.spring.openapi.MockConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = { MockConfiguration.class, TestRunner021.TestConfig.class })
public class TestRunner021 extends AbstractSpringOpenApiTest {

    @Configuration
    @ComponentScan(basePackages = "org.openl.rules.spring.openapi.app021")
    public static class TestConfig {
    }

}
