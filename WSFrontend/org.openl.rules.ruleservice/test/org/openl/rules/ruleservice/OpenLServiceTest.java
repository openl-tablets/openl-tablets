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

class OpenLServiceTest {

    @AfterEach
    void tearDown() {
        OpenLService.reset();
    }

    @Test
    void callNoService() {
        assertNull(OpenLService.rulesFrontend);
        var ex = assertThrows(IllegalArgumentException.class, () -> {
            OpenLService.call("OpenLRulesService", "worldHello");
        });
        assertEquals("Service 'OpenLRulesService' is not found.", ex.getMessage());
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
        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            OpenLService.call("RulesFrontendTest_multimodule", "worldHello", (Object) null);
        });
        assertEquals("Method 'worldHello(null-class)' is not found in service 'RulesFrontendTest_multimodule'.", ex.getMessage());
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
        ex = assertThrows(IllegalArgumentException.class, () -> {
            OpenLService.call("RulesFrontendTest_multimodule", "worldHello", 10);
        });
        assertEquals("Service 'RulesFrontendTest_multimodule' is not found.", ex.getMessage());
        assertEquals(Arrays.asList("first-hello", "second-hello", "third-hello"), OpenLService.rulesFrontend.getServiceNames());
        assertEquals("Hello First world", OpenLService.call("first-hello", "sayHello"));
        assertEquals("Hello Second world", OpenLService.call("second-hello", "sayHello"));
        assertEquals("Hello First world", OpenLService.call("third-hello", "sayHello"));

        // Reset with changing to absent OpenL repository
        OpenLService.reset();
        System.setProperty("production-repository.uri", "no repo");

        ex = assertThrows(IllegalArgumentException.class, () -> {
            OpenLService.call("RulesFrontendTest_multimodule", "worldHello", 10);
        });
        assertEquals("Service 'RulesFrontendTest_multimodule' is not found.", ex.getMessage());

        ex = assertThrows(IllegalArgumentException.class, () -> {
            OpenLService.call("first-hello", "sayHello");
        });
        assertEquals("Service 'first-hello' is not found.", ex.getMessage());

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

        assertEquals("org.openl.generated.interfaces.VirtualModule$proxy", OpenLService.callJSON("RulesFrontendTest_multimodule", "toString", null).substring(0, 50));

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
    void tryJsonService() {
        assertNull(OpenLService.rulesFrontend);

        assertEquals("{\"result\":\"Good Morning\",\"error\":null}", OpenLService.tryJSON("RulesFrontendTest_multimodule", "baseHello", "10"));
        assertEquals("{\"result\":null,\"error\":{\"message\":\"Non-unique 'worldHello' method name in service 'RulesFrontendTest_multimodule'. There are 2 methods with the same name.\",\"type\":\"SYSTEM\"}}", OpenLService.tryJSON("RulesFrontendTest_multimodule", "worldHello", "10"));

        assertEquals("{\"result\":null,\"error\":null}", OpenLService.tryJSON("RulesFrontendTest_multimodule", "oneArg", null));
        assertEquals("{\"result\":{\"name\":\"Nick\",\"age\":25},\"error\":null}", OpenLService.tryJSON("RulesFrontendTest_multimodule", "oneArg", "{}"));
        assertEquals("{\"result\":{\"name\":\"Mike\",\"age\":80},\"error\":null}", OpenLService.tryJSON("RulesFrontendTest_multimodule", "oneArg", "{\"name\":\"Mike\",\"age\":80}"));

        assertEquals("{\"result\":\"i: null s: null\",\"error\":null}", OpenLService.tryJSON("RulesFrontendTest_multimodule", "twoArgs", null));
        assertEquals("{\"result\":\"i: null s: null\",\"error\":null}", OpenLService.tryJSON("RulesFrontendTest_multimodule", "twoArgs", "{}"));
        assertEquals("{\"result\":\"i: 80 s: Mike\",\"error\":null}", OpenLService.tryJSON("RulesFrontendTest_multimodule", "twoArgs", "{\"s\":\"Mike\",\"i\":80}"));

        assertEquals("{\"result\":null,\"error\":null}", OpenLService.tryJSON("RulesFrontendTest_multimodule", "str2str", null));
        assertEquals("{\"result\":\"\",\"error\":null}", OpenLService.tryJSON("RulesFrontendTest_multimodule", "str2str", ""));
        assertEquals("{\"result\":\"\\\"acd\\\"\",\"error\":null}", OpenLService.tryJSON("RulesFrontendTest_multimodule", "str2str", "\"acd\""));
        assertEquals("{\"result\":\"'\\\"\\\"'\",\"error\":null}", OpenLService.tryJSON("RulesFrontendTest_multimodule", "str2str", "'\"\"'"));
        assertEquals("{\"result\":\"{\\\"data\\\":\\\"text\\\"}\",\"error\":null}", OpenLService.tryJSON("RulesFrontendTest_multimodule", "str2str", "{\"data\":\"text\"}"));
        assertEquals("{\"result\":\"{\\\"s\\\":\\\"Mike\\\",\\\"i\\\":80}\",\"error\":null}", OpenLService.tryJSON("RulesFrontendTest_multimodule", "str2str", "{\"s\":\"Mike\",\"i\":80}"));

        assertEquals("{\"result\":\"org.openl.generated.interfaces.VirtualModule$proxy", OpenLService.tryJSON("RulesFrontendTest_multimodule", "toString", null).substring(0, 61));

        assertEquals("{\"result\":null,\"error\":{\"message\":\"CA is not allowed\",\"type\":\"USER_ERROR\"}}", OpenLService.tryJSON("RulesFrontendTest_multimodule", "validate", "CA"));
        assertEquals("{\"result\":\"OK\",\"error\":null}", OpenLService.tryJSON("RulesFrontendTest_multimodule", "validate", "NY"));
        assertEquals("{\"result\":null,\"error\":{\"message\":\"Failure\",\"code\":\"CD1\"}}", OpenLService.tryJSON("RulesFrontendTest_multimodule", "validate", "MI"));
        assertEquals("{\"result\":null,\"error\":{\"message\":\"Object 'WW' is outside of valid domain 'State'. Valid values: [NY, CA, MI]\",\"type\":\"VALIDATION\"}}", OpenLService.tryJSON("RulesFrontendTest_multimodule", "validate", "WW"));
        assertEquals("{\"result\":null,\"error\":{\"message\":\"Object '' is outside of valid domain 'State'. Valid values: [NY, CA, MI]\",\"type\":\"VALIDATION\"}}", OpenLService.tryJSON("RulesFrontendTest_multimodule", "validate", ""));
        assertEquals("{\"result\":null,\"error\":{\"name\":\"Yura\",\"age\":1000}}", OpenLService.tryJSON("RulesFrontendTest_multimodule", "validate", null));
        assertEquals("{\"result\":null,\"error\":{\"message\":\"Unexpected character ('M' (code 77)): was expecting comma to separate Object entries\n" +
                " at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 8]\",\"type\":\"BAD_REQUEST\"}}", OpenLService.tryJSON("RulesFrontendTest_multimodule", "twoArgs", "{\"s\":\"\"Mike\",\"i\":80}"));

        assertNotNull(OpenLService.rulesFrontend);
        OpenLService.reset();
        System.setProperty("production-repository.uri", "no repo");

        assertEquals("{\"result\":null,\"error\":{\"message\":\"Service 'RulesFrontendTest_multimodule' is not found.\",\"type\":\"SYSTEM\"}}",OpenLService.tryJSON("RulesFrontendTest_multimodule", "worldHello", "10"));

        // Free resources
        OpenLService.reset();
        assertNull(OpenLService.rulesFrontend);
    }

    @Test
    @SetSystemProperty(key = "production-repository.uri", value = "test-resources/RulesFrontendTest")
    @SetSystemProperty(key = "production-repository.factory", value = "repo-file")
    @SetSystemProperty(key = "ruleservice.isProvideRuntimeContext", value = "false")
    void callJsonArrayService() throws Exception {
        assertNull(OpenLService.rulesFrontend);

        assertEquals("Good Morning", OpenLService.callJSONArgs("RulesFrontendTest_multimodule", "baseHello", "10"));
        assertEquals("World, Good Morning!", OpenLService.callJSONArgs("RulesFrontendTest_multimodule", "worldHello", "10"));

        assertNull(OpenLService.callJSONArgs("RulesFrontendTest_multimodule", "oneArg", (String) null));
        assertEquals("{\"name\":\"Nick\",\"age\":25}", OpenLService.callJSONArgs("RulesFrontendTest_multimodule", "oneArg", "{}"));
        assertEquals("{\"name\":\"Mike\",\"age\":80}", OpenLService.callJSONArgs("RulesFrontendTest_multimodule", "oneArg", "{\"name\":\"Mike\",\"age\":80}"));

        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            OpenLService.callJSONArgs("RulesFrontendTest_multimodule", "twoArgs");
        });
        assertEquals("Method 'twoArgs' with 0 input arguments is not found in service 'RulesFrontendTest_multimodule'.", ex.getMessage());

        ex = assertThrows(IllegalArgumentException.class, () -> {
            OpenLService.callJSONArgs("RulesFrontendTest_multimodule", "twoArgs", (String) null);
        });
        assertEquals("Method 'twoArgs' with 1 input arguments is not found in service 'RulesFrontendTest_multimodule'.", ex.getMessage());

        ex = assertThrows(IllegalArgumentException.class, () -> {
            OpenLService.callJSONArgs("RulesFrontendTest_multimodule", "twoArgs", "a1");
        });
        assertEquals("Method 'twoArgs' with 1 input arguments is not found in service 'RulesFrontendTest_multimodule'.", ex.getMessage());

        ex = assertThrows(IllegalArgumentException.class, () -> {
            OpenLService.callJSONArgs("RulesFrontendTest_multimodule", "twoArgs", "a1", "a2", "a3");
        });
        assertEquals("Method 'twoArgs' with 3 input arguments is not found in service 'RulesFrontendTest_multimodule'.", ex.getMessage());


        assertEquals("i: null s: null", OpenLService.callJSONArgs("RulesFrontendTest_multimodule", "twoArgs", null, null));
        assertEquals("i: null s: Mike", OpenLService.callJSONArgs("RulesFrontendTest_multimodule", "twoArgs", null, "Mike"));
        assertEquals("i: 80 s: Mike", OpenLService.callJSONArgs("RulesFrontendTest_multimodule", "twoArgs", "80", "Mike"));
        assertEquals("i: 80 s: null", OpenLService.callJSONArgs("RulesFrontendTest_multimodule", "twoArgs", "80", null));

        ex = assertThrows(Exception.class, () -> {
            OpenLService.callJSONArgs("RulesFrontendTest_multimodule", "twoArgs", "Mike", "80");
        });

        assertNull(OpenLService.callJSONArgs("RulesFrontendTest_multimodule", "str2str", (String) null));
        assertEquals("", OpenLService.callJSONArgs("RulesFrontendTest_multimodule", "str2str", ""));
        assertEquals("\"acd\"", OpenLService.callJSONArgs("RulesFrontendTest_multimodule", "str2str", "\"acd\""));
        assertEquals("'\"\"'", OpenLService.callJSONArgs("RulesFrontendTest_multimodule", "str2str", "'\"\"'"));
        assertEquals("{\"data\":\"text\"}", OpenLService.callJSONArgs("RulesFrontendTest_multimodule", "str2str", "{\"data\":\"text\"}"));
        assertEquals("{\"s\":\"Mike\",\"i\":80}", OpenLService.callJSONArgs("RulesFrontendTest_multimodule", "str2str", "{\"s\":\"Mike\",\"i\":80}"));

        assertEquals("org.openl.generated.interfaces.VirtualModule$proxy", OpenLService.callJSONArgs("RulesFrontendTest_multimodule", "toString", (String[]) null).substring(0, 50));
        assertEquals("org.openl.generated.interfaces.VirtualModule$proxy", OpenLService.callJSONArgs("RulesFrontendTest_multimodule", "toString").substring(0, 50));

        assertNotNull(OpenLService.rulesFrontend);
        OpenLService.reset();
        System.setProperty("production-repository.uri", "no repo");

        ex = assertThrows(IllegalArgumentException.class, () -> {
            OpenLService.callJSONArgs("RulesFrontendTest_multimodule", "worldHello", "10");
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

        assertEquals("org.openl.generated.interfaces.VirtualModule$proxy", serviceBean.toString().substring(0, 50));

        var ex = assertThrows(IllegalArgumentException.class, () -> {
            serviceBean.absent("Hello");
        });
        assertEquals("Method 'absent(java.lang.String)' is not found in service 'RulesFrontendTest_multimodule'.", ex.getMessage());

        OpenLService.reset();
        System.setProperty("production-repository.uri", "no repo");

        ex = assertThrows(IllegalArgumentException.class, () -> {
            serviceBean.worldHello(8, "Peace");
        });
        assertEquals("Service 'RulesFrontendTest_multimodule' is not found.", ex.getMessage());

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

        String toString();

        String absent(String value);
    }
}
