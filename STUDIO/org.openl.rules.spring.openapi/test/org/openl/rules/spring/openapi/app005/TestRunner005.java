package org.openl.rules.spring.openapi.app005;

import org.openl.rules.spring.openapi.AbstractSpringOpenApiTest;
import org.openl.rules.spring.openapi.MockConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = { MockConfiguration.class, TestRunner005.TestConfig.class })
public class TestRunner005 extends AbstractSpringOpenApiTest {

    @Configuration
    @ComponentScan
    public static class TestConfig {
    }

}
