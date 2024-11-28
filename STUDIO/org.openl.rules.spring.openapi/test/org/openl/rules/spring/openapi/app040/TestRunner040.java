package org.openl.rules.spring.openapi.app040;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import org.openl.rules.spring.openapi.AbstractSpringOpenApiTest;
import org.openl.rules.spring.openapi.MockConfiguration;

@ContextConfiguration(classes = {MockConfiguration.class, TestRunner040.TestConfig.class})
public class TestRunner040 extends AbstractSpringOpenApiTest {

    @Configuration
    @ComponentScan
    public static class TestConfig {
    }

}
