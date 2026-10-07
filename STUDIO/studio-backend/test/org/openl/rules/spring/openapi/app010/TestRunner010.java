package org.openl.rules.spring.openapi.app010;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import org.openl.rules.spring.openapi.AbstractSpringOpenApiTest;
import org.openl.rules.spring.openapi.MockConfiguration;

@ContextConfiguration(classes = {MockConfiguration.class, TestRunner010.TestConfig.class})
public class TestRunner010 extends AbstractSpringOpenApiTest {

    @Configuration
    @ComponentScan
    public static class TestConfig {
    }

}
