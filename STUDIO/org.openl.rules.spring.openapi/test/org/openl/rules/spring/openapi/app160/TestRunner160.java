package org.openl.rules.spring.openapi.app160;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import org.openl.rules.spring.openapi.AbstractSpringOpenApiTest;
import org.openl.rules.spring.openapi.MockConfiguration;

@ContextConfiguration(classes = {MockConfiguration.class, TestRunner160.TestConfig.class})
public class TestRunner160 extends AbstractSpringOpenApiTest {

    @Configuration
    @ComponentScan
    public static class TestConfig {
    }

}
