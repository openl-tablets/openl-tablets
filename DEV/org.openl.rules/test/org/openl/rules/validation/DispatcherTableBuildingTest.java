package org.openl.rules.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.BaseOpenlBuilderHelper;

public class DispatcherTableBuildingTest extends BaseOpenlBuilderHelper {
    private static final String SRC = "test/rules/overload/DispatcherTest.xlsx";

    public DispatcherTableBuildingTest() {
        super(SRC);
    }

    private static String csr;

    @BeforeClass
    public static void before() {
        csr = System.getProperty(OpenLSystemProperties.DISPATCHING_MODE_PROPERTY, "");
        System.setProperty(OpenLSystemProperties.DISPATCHING_MODE_PROPERTY,
            OpenLSystemProperties.DISPATCHING_MODE_JAVA);
    }

    @AfterClass
    public static void after() {
        System.setProperty(OpenLSystemProperties.DISPATCHING_MODE_PROPERTY, csr);
    }

    @Test
    public void checkKeywordsInSignature() {
        assertNotNull(findDispatcherForMethod("arraysTest"));
        assertNotNull(findDispatcherForMethod("keywordsTest"));
        assertEquals(0, getCompiledOpenClass().getMessages().size());
    }
}
