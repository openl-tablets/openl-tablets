package org.openl.rules.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.BaseOpenlBuilderHelper;

public class DispatcherTableBuildingTest extends BaseOpenlBuilderHelper {
    private static final String SRC = "test/rules/overload/DispatcherTest.xlsx";

    public DispatcherTableBuildingTest() {
        super(SRC);
    }

    private static String csr;

    @BeforeAll
    public static void before() {
        csr = System.getProperty(OpenLSystemProperties.DISPATCHING_MODE_PROPERTY, "");
        System.setProperty(OpenLSystemProperties.DISPATCHING_MODE_PROPERTY,
            OpenLSystemProperties.DISPATCHING_MODE_JAVA);
    }

    @AfterAll
    public static void after() {
        System.setProperty(OpenLSystemProperties.DISPATCHING_MODE_PROPERTY, csr);
    }

    @Test
    public void checkKeywordsInSignature() {
        assertNotNull(findDispatcherForMethod("arraysTest"));
        assertNotNull(findDispatcherForMethod("keywordsTest"));
        assertEquals(0, getCompiledOpenClass().getAllMessages().size());
    }
}
