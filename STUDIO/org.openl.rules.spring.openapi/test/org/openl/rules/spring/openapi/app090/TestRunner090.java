package org.openl.rules.spring.openapi.app090;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import org.openl.rules.spring.openapi.AbstractSpringOpenApiTest;
import org.openl.rules.spring.openapi.MockConfiguration;

/**
 * Test runner for automatic parameter name resolution from method signatures.
 * <p>
 * This test verifies that:
 * <ul>
 *   <li>@PathVariable parameters without explicit names are automatically discovered from method signature</li>
 *   <li>@RequestParam parameters without explicit names are automatically discovered from method signature</li>
 *   <li>Multiple path variables in the same method are correctly resolved</li>
 *   <li>Parameter names match the actual method parameter names (not "arg0", "arg1", etc.)</li>
 * </ul>
 * <p>
 * This requires the code to be compiled with the {@code -parameters} compiler flag.
 */
@ContextConfiguration(classes = {MockConfiguration.class, TestRunner090.TestConfig.class})
public class TestRunner090 extends AbstractSpringOpenApiTest {

    @Configuration
    @ComponentScan
    public static class TestConfig {
    }

}
