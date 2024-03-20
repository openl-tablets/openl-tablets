package org.openl.rules.ruleservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;

import org.openl.rules.ruleservice.simple.MethodInvocationException;

class OpenLServiceTest {

    @AfterEach
    void tearDown() {
        OpenLService.reset();
    }

    @Test
    void callNoService() {
        assertNull(OpenLService.rulesFrontend);
        assertThrows(MethodInvocationException.class, () -> {
            OpenLService.call("OpenLRulesService", "worldHello");
        });
        assertNotNull(OpenLService.rulesFrontend);
        assertTrue(OpenLService.rulesFrontend.getServiceNames().isEmpty());
        OpenLService.reset();
        assertNull(OpenLService.rulesFrontend);
    }

    @Test
    @SetSystemProperty(key = "production-repository.uri", value = "test-resources/RulesFrontendTest")
    @SetSystemProperty(key = "production-repository.factory", value = "repo-file")
    @SetSystemProperty(key = "ruleservice.isProvideRuntimeContext", value = "false")
    void callService() throws Exception {
        assertNull(OpenLService.rulesFrontend);

        assertEquals("World, Good Morning!", OpenLService.call("RulesFrontendTest_multimodule", "worldHello", 10));
        assertThrows(MethodInvocationException.class, () -> {
            OpenLService.call("RulesFrontendTest_multimodule", "worldHello", (Object) null);
        });

        assertEquals("i: 5 s: World", OpenLService.call("RulesFrontendTest_multimodule", "worldHello", 5, "World"));
        assertEquals("i: null s: World", OpenLService.call("RulesFrontendTest_multimodule", "worldHello", null, "World"));
        assertEquals("i: 5 s: null", OpenLService.call("RulesFrontendTest_multimodule", "worldHello", 5, null));
        assertEquals("i: null s: null", OpenLService.call("RulesFrontendTest_multimodule", "worldHello", null, null));

        assertNotNull(OpenLService.rulesFrontend);
        assertEquals(Arrays.asList("org.openl.rules.tutorial4.Tutorial4Interface", "RulesFrontendTest_multimodule", "simple/name"), OpenLService.rulesFrontend.getServiceNames());

        // Reset without changing properties
        OpenLService.reset();
        assertEquals("World, Good Morning!", OpenLService.call("RulesFrontendTest_multimodule", "worldHello", 10));

        // Reset with changing to another OpenL repository
        OpenLService.reset();
        System.setProperty("production-repository.uri", "test-resources/MultipleProjectsInDeploymentTest");
        assertThrows(MethodInvocationException.class, () -> {
            OpenLService.call("RulesFrontendTest_multimodule", "worldHello", 10);
        });
        assertEquals(Arrays.asList("first-hello", "second-hello", "third-hello"), OpenLService.rulesFrontend.getServiceNames());
        assertEquals("Hello First world", OpenLService.call("first-hello", "sayHello"));
        assertEquals("Hello Second world", OpenLService.call("second-hello", "sayHello"));
        assertEquals("Hello First world", OpenLService.call("third-hello", "sayHello"));

        // Reset with changing to absent OpenL repository
        OpenLService.reset();
        System.setProperty("production-repository.uri", "no repo");
        assertThrows(MethodInvocationException.class, () -> {
            OpenLService.call("RulesFrontendTest_multimodule", "worldHello", 10);
        });
        assertThrows(MethodInvocationException.class, () -> {
            OpenLService.call("first-hello", "sayHello");
        });
        assertTrue(OpenLService.rulesFrontend.getServiceNames().isEmpty());

        // Free resources
        OpenLService.reset();
        assertNull(OpenLService.rulesFrontend);
    }

    @Test
    @SetSystemProperty(key = "production-repository.uri", value = "test-resources/RulesFrontendTest")
    @SetSystemProperty(key = "production-repository.factory", value = "repo-file")
    @SetSystemProperty(key = "ruleservice.isProvideRuntimeContext", value = "false")
    void callJsonService() throws Exception {
        assertNull(OpenLService.rulesFrontend);

        assertEquals("Good Morning", OpenLService.callJSON("RulesFrontendTest_multimodule", "baseHello", "10"));
        var ex = assertThrows(IllegalArgumentException.class, () -> {
            OpenLService.callJSON("RulesFrontendTest_multimodule", "worldHello", "10");
        });
        assertEquals("Non-unique 'worldHello' method name in service 'RulesFrontendTest_multimodule'. There are 2 methods with the same name.", ex.getMessage());

        assertNull(OpenLService.callJSON("RulesFrontendTest_multimodule", "oneArg", null));
        assertEquals("{\"name\":\"Nick\",\"age\":25}", OpenLService.callJSON("RulesFrontendTest_multimodule", "oneArg", "{}"));
        assertEquals("{\"name\":\"Mike\",\"age\":80}", OpenLService.callJSON("RulesFrontendTest_multimodule", "oneArg", "{\"name\":\"Mike\",\"age\":80}"));

        assertEquals("i: null s: null", OpenLService.callJSON("RulesFrontendTest_multimodule", "twoArgs", null));
        assertEquals("i: null s: null", OpenLService.callJSON("RulesFrontendTest_multimodule", "twoArgs", "{}"));
        assertEquals("i: 80 s: Mike", OpenLService.callJSON("RulesFrontendTest_multimodule", "twoArgs", "{\"s\":\"Mike\",\"i\":80}"));

        assertNull(OpenLService.callJSON("RulesFrontendTest_multimodule", "str2str", null));
        assertEquals("", OpenLService.callJSON("RulesFrontendTest_multimodule", "str2str", ""));
        assertEquals("\"acd\"", OpenLService.callJSON("RulesFrontendTest_multimodule", "str2str", "\"acd\""));
        assertEquals("'\"\"'", OpenLService.callJSON("RulesFrontendTest_multimodule", "str2str", "'\"\"'"));
        assertEquals("{\"data\":\"text\"}", OpenLService.callJSON("RulesFrontendTest_multimodule", "str2str", "{\"data\":\"text\"}"));
        assertEquals("{\"s\":\"Mike\",\"i\":80}", OpenLService.callJSON("RulesFrontendTest_multimodule", "str2str", "{\"s\":\"Mike\",\"i\":80}"));

        assertNotNull(OpenLService.rulesFrontend);
        OpenLService.reset();
        System.setProperty("production-repository.uri", "no repo");

        ex = assertThrows(IllegalArgumentException.class, () -> {
            OpenLService.callJSON("RulesFrontendTest_multimodule", "worldHello", "10");
        });
        assertEquals("Service 'RulesFrontendTest_multimodule' is not found.", ex.getMessage());

        // Free resources
        OpenLService.reset();
        assertNull(OpenLService.rulesFrontend);
    }

    @Test
    @SetSystemProperty(key = "production-repository.uri", value = "test-resources/RulesFrontendTest")
    @SetSystemProperty(key = "production-repository.factory", value = "repo-file")
    @SetSystemProperty(key = "ruleservice.isProvideRuntimeContext", value = "false")
    void getService() throws Exception {
        assertNull(OpenLService.rulesFrontend);

        assertNull(OpenLService.get("absent"));
        assertNotNull(OpenLService.rulesFrontend);
        var serviceBean = OpenLService.get("RulesFrontendTest_multimodule");
        assertNotNull(serviceBean);
        assertSame(serviceBean, OpenLService.get("RulesFrontendTest_multimodule"));

        OpenLService.reset();
        assertNotSame(serviceBean, serviceBean = OpenLService.get("RulesFrontendTest_multimodule"));

        var worldHello = serviceBean.getClass().getMethod("worldHello", Integer.class, String.class);
        assertEquals("i: 8 s: Peace", worldHello.invoke(serviceBean, 8, "Peace"));

        OpenLService.reset();
        System.setProperty("production-repository.uri", "no repo");

        assertNull(OpenLService.get("RulesFrontendTest_multimodule"));
        assertEquals("i: 8 s: Peace", worldHello.invoke(serviceBean, 8, "Peace")); // stored instance in the memory

        // Free resources
        OpenLService.reset();
        assertNull(OpenLService.rulesFrontend);
    }

    @Test
    @SetSystemProperty(key = "production-repository.uri", value = "test-resources/RulesFrontendTest")
    @SetSystemProperty(key = "production-repository.factory", value = "repo-file")
    @SetSystemProperty(key = "ruleservice.isProvideRuntimeContext", value = "false")
    void proxyService() throws Exception {
        assertNull(OpenLService.rulesFrontend);

        assertNotNull(OpenLService.proxy("absent", Proxy.class));
        assertNull(OpenLService.rulesFrontend);

        var serviceBean = OpenLService.proxy("RulesFrontendTest_multimodule", Proxy.class);
        assertNotNull(serviceBean);

        assertEquals("i: 8 s: Peace", serviceBean.worldHello(8, "Peace"));
        assertNotNull(OpenLService.rulesFrontend);

        OpenLService.reset();
        System.setProperty("production-repository.uri", "no repo");

        assertThrows(MethodInvocationException.class, () -> {
            serviceBean.worldHello(8, "Peace");
        });

        // Free resources
        OpenLService.reset();
        assertNull(OpenLService.rulesFrontend);
    }

    @Test
    void reset() {
        assertNull(OpenLService.rulesFrontend);
        OpenLService.reset();
        assertNull(OpenLService.rulesFrontend);
    }

    private interface Proxy {
        String worldHello(Integer i, String s);
    }
}
