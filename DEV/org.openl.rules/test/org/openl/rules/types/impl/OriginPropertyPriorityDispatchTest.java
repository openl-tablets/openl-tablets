package org.openl.rules.types.impl;

import org.junit.Before;
import org.junit.Test;
import org.openl.rules.TestHelper;

import java.io.File;

import static org.junit.Assert.assertEquals;

/**
 * Created by dl on 10/3/14.
 */
public class OriginPropertyPriorityDispatchTest {
    private static final String RULES_SOURCE_FILE = "test/rules/dispatching/OriginPropertyPriorityDispatch.xls";

    private Rules instance;

    @Before
    public void setUp() throws Exception {
        instance = new TestHelper<Rules>(new File(RULES_SOURCE_FILE), Rules.class).getInstance();
    }

    @Test
    public void testOriginProperty() {
        assertEquals("Deviation", instance.testOriginProperty());
    }

    public static interface Rules {
        String testOriginProperty();
    }
}
